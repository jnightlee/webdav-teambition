package com.github.zxbu.webdavteambition.model.result;

import java.util.List;

public class TFileListResult<T> {
    private List<T> items;

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
