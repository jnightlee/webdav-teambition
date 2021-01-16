package com.github.zxbu.webdavteambition.model;

public class CreateFileRequest {
    private String ccpParentId;
    private String checkNameMode = "refuse";
    private String driveId;
    private String name;
    private String orgId;
    private String parentId;
    private String spaceId;
    private String type;

    public String getCheckNameMode() {
        return checkNameMode;
    }

    public void setCheckNameMode(String checkNameMode) {
        this.checkNameMode = checkNameMode;
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCcpParentId() {
        return ccpParentId;
    }

    public void setCcpParentId(String ccpParentId) {
        this.ccpParentId = ccpParentId;
    }
}
