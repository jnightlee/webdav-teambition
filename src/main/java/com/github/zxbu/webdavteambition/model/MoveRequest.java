package com.github.zxbu.webdavteambition.model;

import java.util.List;

public class MoveRequest {
    private String driveId;
    private String parentId;
    private String orgId;
    private boolean sameLevel = false;
    private List<MoveRequestId> ids;

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public boolean isSameLevel() {
        return sameLevel;
    }

    public void setSameLevel(boolean sameLevel) {
        this.sameLevel = sameLevel;
    }

    public List<MoveRequestId> getIds() {
        return ids;
    }

    public void setIds(List<MoveRequestId> ids) {
        this.ids = ids;
    }
}
