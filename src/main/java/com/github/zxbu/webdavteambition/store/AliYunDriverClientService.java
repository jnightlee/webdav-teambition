package com.github.zxbu.webdavteambition.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import com.github.zxbu.webdavteambition.model.*;
import com.github.zxbu.webdavteambition.model.result.CreateFileResult;
import com.github.zxbu.webdavteambition.model.result.TFileListResult;
import com.github.zxbu.webdavteambition.model.result.TFile;
import com.github.zxbu.webdavteambition.model.result.UploadPreResult;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import net.sf.webdav.exceptions.WebdavException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AliYunDriverClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverClientService.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private TFile rootTFile = null;
    private ThreadLocal<Map<String, Set<TFile>>> tFilesCache = new ThreadLocal<>();

    private final AliYunDriverClient client;

    public AliYunDriverClientService(AliYunDriverClient aliYunDriverClient) {
        this.client = aliYunDriverClient;
        AliYunDriverFileSystemStore.setBean(this);
    }

    public Set<TFile> getTFiles(String nodeId) {
        Map<String, Set<TFile>> map = tFilesCache.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            tFilesCache.set(map);
        }
        return map.computeIfAbsent(nodeId, key -> {
            FileListRequest listQuery = new FileListRequest();
            listQuery.setOffset(0);
            listQuery.setLimit(10000);
            listQuery.setOrder_by("updated_at");
            listQuery.setOrder_direction("DESC");
            listQuery.setDrive_id(client.getDriveId());
            listQuery.setParent_file_id(nodeId);
            String json = client.post("/file/list", listQuery);
            TFileListResult<TFile> tFileListResult = JsonUtil.readValue(json, new TypeReference<TFileListResult<TFile>>() {
            });
            List<TFile> tFileList = tFileListResult.getItems();
            tFileList.sort(Comparator.comparing(TFile::getUpdated_at).reversed());
            Set<TFile> tFileSets = new LinkedHashSet<>();
            for (TFile tFile : tFileList) {
                if (!tFileSets.add(tFile)) {
                    LOGGER.info("当前目录下{} 存在同名文件：{}，文件大小：{}", nodeId, tFile.getName(), tFile.getSize());
                }
            }
            // 对文件名进行去重，只保留最新的一个
            return tFileSets;
        });

    }


    private Map<String, String> toMap(Object o) {
        try {
            String json = objectMapper.writeValueAsString(o);
            Map<String, Object> rawMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Map<String, String> stringMap = new LinkedHashMap<>();
            rawMap.forEach((s, o1) -> {
                if (o1 != null) {
                    stringMap.put(s, o1.toString());
                }
            });
            return stringMap;
        } catch (Exception e) {
            throw new WebdavException(e);
        }
    }

    public void uploadPre(String path, int size, InputStream inputStream) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent =  getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            return;
        }
        int chunkCount = (int) Math.ceil(((double) size) / chunkSize); // 进1法

        UploadPreRequest uploadPreRequest = new UploadPreRequest();
        uploadPreRequest.setContent_hash(UUID.randomUUID().toString());
        uploadPreRequest.setDrive_id(client.getDriveId());
        uploadPreRequest.setName(pathInfo.getName());
        uploadPreRequest.setParent_file_id(parent.getFile_id());
        uploadPreRequest.setSize((long) size);
        List<UploadPreRequest.PartInfo> part_info_list = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            UploadPreRequest.PartInfo partInfo = new UploadPreRequest.PartInfo();
            partInfo.setPart_number(i + 1);
            part_info_list.add(partInfo);
        }
        uploadPreRequest.setPart_info_list(part_info_list);

        LOGGER.info("开始上传文件，文件名：{}，总大小：{}, 文件块数量：{}", path, size, chunkCount);

        String json = client.post("/file/create_with_proof", uploadPreRequest);
        UploadPreResult uploadPreResult = JsonUtil.readValue(json, UploadPreResult.class);
        List<UploadPreRequest.PartInfo> partInfoList = uploadPreResult.getPart_info_list();
        if (partInfoList != null) {
            LOGGER.info("文件预处理成功，开始上传。文件名：{}，上传URL数量：{}", path, partInfoList.size());

            byte[] buffer = new byte[chunkSize];
            for (int i = 0; i < partInfoList.size(); i++) {
                UploadPreRequest.PartInfo partInfo = partInfoList.get(i);
                try {
                    int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                    if (read == -1) {
                        return;
                    }
                    client.upload(partInfo.getUpload_url(), buffer, 0, read);
                    LOGGER.info("文件正在上传。文件名：{}，当前进度：{}/{}", path, (i+1), partInfoList.size());

                } catch (IOException e) {
                    throw new WebdavException(e);
                }
            }
        }



        UploadFinalRequest uploadFinalRequest = new UploadFinalRequest();
        uploadFinalRequest.setFile_id(uploadPreResult.getFile_id());
        uploadFinalRequest.setDrive_id(client.getDriveId());
        uploadFinalRequest.setUpload_id(uploadPreResult.getUpload_id());

        client.post("/file/complete", uploadFinalRequest);
        LOGGER.info("文件上传成功。文件名：{}", path);
//        if (!uploadPreResult.getName().equals(pathInfo.getName())) {
//            LOGGER.info("上传文件名{}与原文件名{}不同，对文件进行重命名", uploadPreResult.getName(), pathInfo.getName());
//            TFile oldFile = getNodeIdByPath2(path);
//            // 如果旧文件存在，则先删除
//            if (oldFile != null) {
//                LOGGER.info("旧文件{}还存在，大小为{}，进行删除操作，可前往网页版的回收站查看", path, oldFile.getSize());
//                remove(path);
//                try {
//                    Thread.sleep(1500);
//                } catch (InterruptedException e) {
//                    // no
//                }
//            }
//            RenameRequest renameRequest = new RenameRequest();
//            renameRequest.setDrive_id(client.getDriveId());
//            renameRequest.setFile_id(oldFile.getFile_id());
//            renameRequest.setName(pathInfo.getName());
//            client.post("/file/update", renameRequest);
//        }
        clearCache();
    }

    public void rename(String sourcePath, String newName) {
        sourcePath = normalizingPath(sourcePath);
        TFile tFile = getTFileByPath(sourcePath);
        RenameRequest renameRequest = new RenameRequest();
        renameRequest.setDrive_id(client.getDriveId());
        renameRequest.setFile_id(tFile.getFile_id());
        renameRequest.setName(newName);
        client.post("/file/update", renameRequest);
        clearCache();
    }

    public void move(String sourcePath, String targetPath) {
        sourcePath = normalizingPath(sourcePath);
        targetPath = normalizingPath(targetPath);

        TFile sourceTFile = getTFileByPath(sourcePath);
        TFile targetTFile = getTFileByPath(targetPath);
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setDrive_id(client.getDriveId());
        moveRequest.setFile_id(sourceTFile.getFile_id());
        moveRequest.setTo_parent_file_id(targetTFile.getFile_id());
        client.post("/file/move", moveRequest);
        clearCache();
    }

    public void remove(String path) {
        path = normalizingPath(path);
        TFile tFile = getTFileByPath(path);
        if (tFile == null) {
            return;
        }
        RemoveRequest removeRequest = new RemoveRequest();
        removeRequest.setDrive_id(client.getDriveId());
        removeRequest.setFile_id(tFile.getFile_id());
        client.post("/recyclebin/trash", removeRequest);
        clearCache();
    }


    public void createFolder(String path) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent =  getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            LOGGER.warn("创建目录失败，未发现父级目录：{}", pathInfo.getParentPath());
            return;
        }

        CreateFileRequest createFileRequest = new CreateFileRequest();
        createFileRequest.setDrive_id(client.getDriveId());
        createFileRequest.setName(pathInfo.getName());
        createFileRequest.setParent_file_id(parent.getFile_id());
        createFileRequest.setType(FileType.folder.name());
        String json = client.post("/file/create_with_proof", createFileRequest);
        TFile createFileResult = JsonUtil.readValue(json, TFile.class);
        if (createFileResult.getFile_name() == null) {
            LOGGER.error("创建目录{}失败: {}",path, json);
        }
        if (!createFileResult.getFile_name().equals(pathInfo.getName())) {
            LOGGER.info("创建目录{}与原值{}不同，重命名", createFileResult.getName(), pathInfo.getName());
            rename(pathInfo.getParentPath() + "/" + createFileResult.getName(), pathInfo.getName());
            clearCache();
        }
        clearCache();
    }


    public TFile getTFileByPath(String path) {
        path = normalizingPath(path);

        return getNodeIdByPath2(path);
    }

    public InputStream download(String path) {
//        String downloadUrl = getTFileByPath(path).getDownloadUrl();
        return client.download("downloadUrl");
    }

    private TFile getNodeIdByPath2(String path) {
        if (!StringUtils.hasLength(path)) {
            path = rootPath;
        }
        if (path.equals(rootPath)) {
            return getRootTFile();
        }
        PathInfo pathInfo = getPathInfo(path);
        TFile tFile = getTFileByPath(pathInfo.getParentPath());
        if (tFile == null ) {
            return null;
        }
        return getNodeIdByParentId(tFile.getFile_id(), pathInfo.getName());
    }


    public PathInfo getPathInfo(String path) {
        path = normalizingPath(path);
        if (path.equals(rootPath)) {
            PathInfo pathInfo = new PathInfo();
            pathInfo.setPath(path);
            pathInfo.setName(path);
            return pathInfo;
        }
        int index = path.lastIndexOf("/");
        String parentPath = path.substring(0, index + 1);
        String name = path.substring(index+1);
        PathInfo pathInfo = new PathInfo();
        pathInfo.setPath(path);
        pathInfo.setParentPath(parentPath);
        pathInfo.setName(name);
        return pathInfo;
    }

    private TFile getRootTFile() {
        if (rootTFile == null) {
//            FileGetRequest fileGetRequest = new FileGetRequest();
//            fileGetRequest.setFile_id("root");
//            fileGetRequest.setDrive_id(client.getDriveId());
//            String json = client.post("/file/get", fileGetRequest);
//            rootTFile = JsonUtil.readValue(json, TFile.class);
            rootTFile = new TFile();
            rootTFile.setName("/");
            rootTFile.setFile_id("root");
            rootTFile.setCreated_at(new Date());
            rootTFile.setUpdated_at(new Date());
            rootTFile.setType("folder");
        }
        return rootTFile;
    }

    private TFile getNodeIdByParentId(String parentId, String name) {
        Set<TFile> tFiles = getTFiles(parentId);
        for (TFile tFile : tFiles) {
            if (tFile.getName().equals(name)) {
                return tFile;
            }
        }

        return null;
    }


    private String normalizingPath(String path) {
        path = path.replaceAll("//", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public void clearCache() {
        Map<String, Set<TFile>> map = tFilesCache.get();
        if (map != null) {
            map.clear();
        }

    }
}
