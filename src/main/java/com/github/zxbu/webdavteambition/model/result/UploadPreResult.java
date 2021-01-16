package com.github.zxbu.webdavteambition.model.result;

import java.util.List;

public class UploadPreResult {
    private String ccpFileId;
    private String nodeId;
    private String name;
    private String kind;
    private String uploadId;
    private List<String> uploadUrl;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCcpFileId() {
        return ccpFileId;
    }

    public void setCcpFileId(String ccpFileId) {
        this.ccpFileId = ccpFileId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public List<String> getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(List<String> uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
}
