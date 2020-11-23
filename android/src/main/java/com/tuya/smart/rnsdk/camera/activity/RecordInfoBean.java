package com.tuya.smart.rnsdk.camera.activity;

import com.tuya.smart.camera.camerasdk.bean.TimePieceBean;

import java.util.List;

public class RecordInfoBean {
    private int count;
    private List<TimePieceBean> items;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<TimePieceBean> getItems() {
        return items;
    }

    public void setItems(List<TimePieceBean> items) {
        this.items = items;
    }
}
