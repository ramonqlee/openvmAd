package com.idreems.openvmAd.multimedia.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ifensi on 2017/2/23.
 */

public class LimitNetParam {
    @SerializedName("param")
    public String param;

    public LimitNetParam() {
    }

    public LimitNetParam(String param) {
        this.param = param;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || !(obj instanceof String)) {
            return false;
        }
        String temp = (String) obj;
        return TextUtils.equals(temp, param);
    }

    @Override
    public String toString() {
        return "LimitNetParam{" +
                "param='" + param + '\'' +
                '}';
    }
}
