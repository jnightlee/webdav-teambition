package com.github.zxbu.webdavteambition.model;

public class UploadFinalRequest {
    private String ccpFileId;
    private String driveId;
    private String nodeId;
    private String orgId;
    private String uploadId;

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getCcpFileId() {
        return ccpFileId;
    }

    public void setCcpFileId(String ccpFileId) {
        this.ccpFileId = ccpFileId;
    }
}
