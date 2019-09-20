package com.idreems.openvmAd.multimedia.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Administrator on 2018/1/14.
 */
public class LimitNetCountBean {

    @SerializedName("playerengine")
    public LimitNetBean playerengine;
    @SerializedName("global")
    public LimitNetBean global;
    @SerializedName("limitnet")
    public LimitNetBean limitnet;
    @SerializedName("deviceUpdate")
    public LimitNetBean deviceUpdate;
    @SerializedName("onlineCinema")
    public LimitNetBean onlineCinema;
    @SerializedName("tvapps")
    public LimitNetBean tvapps;
    @SerializedName("UninstallAppForPuruier")
    public LimitNetBean uninstallAppForPuruier;
    @SerializedName("ThirdAd")
    public LimitNetBean ThirdAd;
    @SerializedName("football")
    public LimitNetBean football;
    @SerializedName("XPO")
    public LimitNetBean XPO;
    @SerializedName("HTTP")
    public LimitNetBean TP;

    @SerializedName("OneYuanAd")
    public LimitNetBean OneYuanAd;

    @SerializedName("VodOptimium")
    public LimitNetBean vodOptimium;

    @SerializedName("PCDN")
    public LimitNetBean pcdn;

    public LimitNetBean getPlayerengine() {
        return playerengine;
    }

    public void setPlayerengine(LimitNetBean playerengine) {
        this.playerengine = playerengine;
    }

    public LimitNetBean getGlobal() {
        return global;
    }

    public void setGlobal(LimitNetBean global) {
        this.global = global;
    }

    public LimitNetBean getLimitnet() {
        return limitnet;
    }

    public void setLimitnet(LimitNetBean limitnet) {
        this.limitnet = limitnet;
    }

    public LimitNetBean getDeviceUpdate() {
        return deviceUpdate;
    }

    public void setDeviceUpdate(LimitNetBean deviceUpdate) {
        this.deviceUpdate = deviceUpdate;
    }

    public LimitNetBean getOnlineCinema() {
        return onlineCinema;
    }

    public void setOnlineCinema(LimitNetBean onlineCinema) {
        this.onlineCinema = onlineCinema;
    }

    public LimitNetBean getTvapps() {
        return tvapps;
    }

    public LimitNetBean getUninstallAppForPuruier()
    {
        return uninstallAppForPuruier;
    }

    public void setTvapps(LimitNetBean tvapps) {
        this.tvapps = tvapps;
    }

    public LimitNetBean getThirdAd() {
        return ThirdAd;
    }

    public void setThirdAd(LimitNetBean thirdAd) {
        ThirdAd = thirdAd;
    }

    public LimitNetBean getFootball() {
        return football;
    }

    public void setFootball(LimitNetBean football) {
        this.football = football;
    }

    public LimitNetBean getTP() {
        return TP;
    }

    public void setTP(LimitNetBean TP) {
        this.TP = TP;
    }

    public LimitNetBean getXPO() {
        return XPO;
    }

    public void setXPO(LimitNetBean XPO) {
        this.XPO = XPO;
    }

    public LimitNetBean getOneYuanAd() {
        return OneYuanAd;
    }

    public LimitNetBean getVodOptimium() {
        return vodOptimium;
    }

    public void setOneYuanAd(LimitNetBean oneYuanAd) {
        OneYuanAd = oneYuanAd;
    }

    public LimitNetBean getPCDN() {
        return pcdn;
    }
}
