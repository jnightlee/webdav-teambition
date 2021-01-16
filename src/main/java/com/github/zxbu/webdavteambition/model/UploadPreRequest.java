package com.github.zxbu.webdavteambition.model;

import java.util.List;

public class UploadPreRequest {
    private String checkNameMode = "autoRename";
    private String orgId;
    private String parentId;
    private String spaceId;
    private List<UploadPreInfo> infos;

    public String getCheckNameMode() {
        return checkNameMode;
    }

    public void setCheckNameMode(String checkNameMode) {
        this.checkNameMode = checkNameMode;
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

    public List<UploadPreInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<UploadPreInfo> infos) {
        this.infos = infos;
    }
}
