package com.idreems.openvmAd.multimedia.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wxliao on 2015/5/19.
 */
public class Result {
    @SerializedName("code")
    public int code;
    @SerializedName("timestamp")
    public long timestamp;
    @SerializedName("message")
    public String msg;
    @SerializedName("data")
    private LimitNetCountBean data;

    public static boolean isSuccess(int code) {
        return (code == 1) || (code == 200);
    }

    /**
     * @return The data
     */
    public LimitNetCountBean getData() {
        return data;
    }

    /**
     * @param data The data
     */
    public void setData(LimitNetCountBean data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return isSuccess(code);
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
