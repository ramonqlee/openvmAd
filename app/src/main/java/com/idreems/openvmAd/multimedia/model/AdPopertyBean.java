package com.idreems.openvmAd.multimedia.model;

import android.text.TextUtils;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/3/16.
 */
public class AdPopertyBean implements Serializable {
    private String type="1";
    private String sort="1";
    private String price="1.0";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "AdPopertyBean{" +
                "type='" + type + '\'' +
                ", sort='" + sort + '\'' +
                ", price='" + price + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        return TextUtils.equals(this.getType(), ((AdPopertyBean) obj).getType());
    }
}
