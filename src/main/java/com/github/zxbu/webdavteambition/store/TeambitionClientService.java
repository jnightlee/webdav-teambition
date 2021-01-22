package com.github.zxbu.webdavteambition.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zxbu.webdavteambition.client.TeambitionClient;
import com.github.zxbu.webdavteambition.model.*;
import com.github.zxbu.webdavteambition.model.result.CreateFileResult;
import com.github.zxbu.webdavteambition.model.result.ListResult;
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
public class TeambitionClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeambitionClientService.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private TFile rootTFile = null;
    private ThreadLocal<Map<String, Set<TFile>>> tFilesCache = new ThreadLocal<>();

    private final TeambitionClient client;

    public TeambitionClientService(TeambitionClient teambitionClient) {
        this.client = teambitionClient;
        TeambitionFileSystemStore.setBean(this);
    }

    public Set<TFile> getTFiles(String nodeId) {
        Map<String, Set<TFile>> map = tFilesCache.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            tFilesCache.set(map);
        }
        return map.computeIfAbsent(nodeId, key -> {
            NodeQuery nodeQuery = new NodeQuery();
            nodeQuery.setOrgId(client.getOrgId());
            nodeQuery.setOffset(0);
            nodeQuery.setLimit(10000);
            nodeQuery.setOrderBy("updateTime");
            nodeQuery.setOrderDirection("desc");
            nodeQuery.setDriveId(client.getDriveId());
            nodeQuery.setSpaceId(client.getSpaceId());
            nodeQuery.setParentId(nodeId);
            String json = client.get("/pan/api/nodes", toMap(nodeQuery));
            ListResult<TFile> tFileListResult = JsonUtil.readValue(json, new TypeReference<ListResult<TFile>>() {
            });
            List<TFile> tFileList = tFileListResult.getData();
            tFileList.sort(Comparator.comparing(TFile::getUpdated).reversed());
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
        uploadPreRequest.setOrgId(client.getOrgId());
        uploadPreRequest.setParentId(parent.getNodeId());
        uploadPreRequest.setSpaceId(client.getSpaceId());
        UploadPreInfo uploadPreInfo = new UploadPreInfo();
        uploadPreInfo.setCcpParentId(parent.getCcpFileId());
        uploadPreInfo.setDriveId(client.getDriveId());
        uploadPreInfo.setName(pathInfo.getName());
        uploadPreInfo.setSize(size);
        uploadPreInfo.setChunkCount(chunkCount);
        uploadPreInfo.setType(FileType.file.name());
        uploadPreRequest.setInfos(Collections.singletonList(uploadPreInfo));
        LOGGER.info("开始上传文件，文件名：{}，总大小：{}, 文件块数量：{}", path, size, chunkCount);

        String json = client.post("/pan/api/nodes/file", uploadPreRequest);
        List<UploadPreResult> uploadPreResultList = JsonUtil.readValue(json, new TypeReference<List<UploadPreResult>>() {
        });
        UploadPreResult uploadPreResult = uploadPreResultList.get(0);
        List<String> uploadUrl = uploadPreResult.getUploadUrl();
        LOGGER.info("文件预处理成功，开始上传。文件名：{}，上传URL数量：{}", path, uploadUrl.size());

        byte[] buffer = new byte[chunkSize];
        for (int i = 0; i < uploadUrl.size(); i++) {
            String oneUploadUrl = uploadUrl.get(i);
            try {
                int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                if (read == -1) {
                    return;
                }
                client.upload(oneUploadUrl, buffer, 0, read);
                LOGGER.info("文件正在上传。文件名：{}，当前进度：{}/{}", path, (i+1), uploadUrl.size());

            } catch (IOException e) {
                throw new WebdavException(e);
            }
        }


        UploadFinalRequest uploadFinalRequest = new UploadFinalRequest();
        uploadFinalRequest.setCcpFileId(uploadPreResult.getCcpFileId());
        uploadFinalRequest.setDriveId(client.getDriveId());
        uploadFinalRequest.setNodeId(uploadPreResult.getNodeId());
        uploadFinalRequest.setOrgId(client.getOrgId());
        uploadFinalRequest.setUploadId(uploadPreResult.getUploadId());
        client.post("/pan/api/nodes/complete", uploadFinalRequest);
        LOGGER.info("文件上传成功。文件名：{}", path);
        if (!uploadPreResult.getName().equals(pathInfo.getName())) {
            LOGGER.info("上传文件名{}与原文件名{}不同，对文件进行重命名", uploadPreResult.getName(), pathInfo.getName());
            TFile oldFile = getNodeIdByPath2(path);
            // 如果旧文件存在，则先删除
            if (oldFile != null) {
                LOGGER.info("旧文件{}还存在，大小为{}，进行删除操作，可前往网页版的回收站查看", path, oldFile.getSize());
                remove(path);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    // no
                }
            }
            RenameRequest renameRequest = new RenameRequest();
            renameRequest.setCcpFileId(uploadPreResult.getCcpFileId());
            renameRequest.setDriveId(client.getDriveId());
            renameRequest.setOrgId(client.getOrgId());
            renameRequest.setName(pathInfo.getName());
            client.put("/pan/api/nodes/" + parent.getNodeId(), renameRequest);
        }
        clearCache();
    }

    public void rename(String sourcePath, String newName) {
        sourcePath = normalizingPath(sourcePath);
        TFile tFile = getTFileByPath(sourcePath);
        RenameRequest renameRequest = new RenameRequest();
        renameRequest.setCcpFileId(tFile.getCcpFileId());
        renameRequest.setDriveId(client.getDriveId());
        renameRequest.setOrgId(client.getOrgId());
        renameRequest.setName(newName);
        client.put("/pan/api/nodes/" + tFile.getParentId(), renameRequest);
        clearCache();
    }

    public void move(String sourcePath, String targetPath) {
        sourcePath = normalizingPath(sourcePath);
        targetPath = normalizingPath(targetPath);

        TFile sourceTFile = getTFileByPath(sourcePath);
        TFile targetTFile = getTFileByPath(targetPath);
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setOrgId(client.getOrgId());
        moveRequest.setDriveId(client.getDriveId());
        moveRequest.setParentId(targetTFile.getNodeId());
        MoveRequestId moveRequestId = new MoveRequestId();
        moveRequestId.setCcpFileId(sourceTFile.getCcpFileId());
        moveRequestId.setId(sourceTFile.getNodeId());
        moveRequest.setIds(Collections.singletonList(moveRequestId));
        client.post("/pan/api/nodes/move", moveRequest);
        clearCache();
        clearCache();
    }

    public void remove(String path) {
        path = normalizingPath(path);
        TFile tFile = getTFileByPath(path);
        if (tFile == null) {
            return;
        }
        RemoveRequest removeRequest = new RemoveRequest();
        removeRequest.setOrgId(client.getOrgId());
        removeRequest.setNodeIds(Collections.singletonList(tFile.getNodeId()));
        client.post("/pan/api/nodes/archive", removeRequest);
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
        createFileRequest.setCcpParentId(parent.getCcpFileId());
        createFileRequest.setDriveId(client.getDriveId());
        createFileRequest.setName(pathInfo.getName());
        createFileRequest.setOrgId(client.getOrgId());
        createFileRequest.setParentId(parent.getNodeId());
        createFileRequest.setSpaceId(client.getSpaceId());
        createFileRequest.setType(FileType.folder.name());
        String json = client.post("/pan/api/nodes/folder", createFileRequest);
        List<CreateFileResult> createFileResults = JsonUtil.readValue(json, new TypeReference<List<CreateFileResult>>() {
        });
        if (createFileResults.size() != 1) {
            LOGGER.error("创建目录{}失败: {}",path, json);
        }
        CreateFileResult createFileResult = createFileResults.get(0);
        if (!createFileResult.getName().equals(pathInfo.getName())) {
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
        String downloadUrl = getTFileByPath(path).getDownloadUrl();
        return client.download(downloadUrl);
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
        return getNodeIdByParentId(tFile.getNodeId(), pathInfo.getName());
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
            NodeQuery nodeQuery = new NodeQuery();
            nodeQuery.setOrgId(client.getOrgId());
            nodeQuery.setDriveId(client.getDriveId());
            nodeQuery.setSpaceId(client.getSpaceId());
            String json = client.get("/pan/api/nodes/" + client.getRootId(), toMap(nodeQuery));
            rootTFile = JsonUtil.readValue(json, TFile.class);
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
