package com.github.zxbu.webdavteambition.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zxbu.webdavteambition.client.TeambitionClient;
import com.github.zxbu.webdavteambition.model.*;
import com.github.zxbu.webdavteambition.model.result.ListResult;
import com.github.zxbu.webdavteambition.model.result.TFile;
import com.github.zxbu.webdavteambition.model.result.UploadPreResult;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TeambitionClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeambitionClientService.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private TFile rootTFile = null;
    private Map<String, TFile> nodeIdMap = new ConcurrentHashMap<>();

    private final TeambitionClient client;

    public TeambitionClientService(TeambitionClient teambitionClient) {
        this.client = teambitionClient;
        TeambitionFileSystemStore.setBean(this);
    }

    public List<TFile> getTFiles(String nodeId) {
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
        return tFileListResult.getData();
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
            throw new RuntimeException(e);
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
        for (String oneUploadUrl : uploadUrl) {
            try {
                int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                if (read == -1) {
                    return;
                }
                client.upload(oneUploadUrl, buffer, 0, read);
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            RenameRequest renameRequest = new RenameRequest();
            renameRequest.setCcpFileId(uploadPreResult.getCcpFileId());
            renameRequest.setDriveId(client.getDriveId());
            renameRequest.setOrgId(client.getOrgId());
            renameRequest.setName(pathInfo.getName());
            client.put("/pan/api/nodes/" + parent.getNodeId(), renameRequest);
        }
        clearCache(path);
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
        clearCache(sourcePath);
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
        clearCache(sourcePath);
        clearCache(targetPath);
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
        clearCache(path);
    }


    public void createFolder(String path) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent =  getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
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
        client.post("/pan/api/nodes/folder", createFileRequest);
        clearCache(path);
    }


    public TFile getTFileByPath(String path) {
        path = normalizingPath(path);

        return nodeIdMap.computeIfAbsent(path, this::getNodeIdByPath2);
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
        List<TFile> tFiles = getTFiles(parentId);
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

    private void clearCache(String path) {
        nodeIdMap.remove(path);
    }
}
