package com.github.zxbu.webdavteambition.model;

import java.util.List;

public class RemoveRequest {
    private String orgId;
    private List<String> nodeIds;

    public List<String> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }
}
