package com.github.zxbu.webdavteambition.model.result;

import java.util.Date;
import java.util.Objects;

public class TFile {
    private String kind;
    private String nodeId;
    private String name;
    private Date created;
    private Date updated;
    private String parentId;
    private String status;
    private String downloadUrl;
    private Long size;
    private String ccpFileId;
    private String ccpParentFileId;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

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

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSize() {
        return size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getCcpFileId() {
        return ccpFileId;
    }

    public void setCcpFileId(String ccpFileId) {
        this.ccpFileId = ccpFileId;
    }

    public String getCcpParentFileId() {
        return ccpParentFileId;
    }

    public void setCcpParentFileId(String ccpParentFileId) {
        this.ccpParentFileId = ccpParentFileId;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TFile tFile = (TFile) o;
        return Objects.equals(kind, tFile.kind) &&
                Objects.equals(name, tFile.name) &&
                Objects.equals(parentId, tFile.parentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name, parentId);
    }
}
