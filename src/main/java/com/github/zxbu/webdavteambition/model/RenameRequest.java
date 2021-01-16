package com.github.zxbu.webdavteambition.model;

public class RenameRequest {
    private String ccpFileId;
    private String driveId;
    private String name;
    private String orgId;

    public String getCcpFileId() {
        return ccpFileId;
    }

    public void setCcpFileId(String ccpFileId) {
        this.ccpFileId = ccpFileId;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }
}
