package com.idreems.openvmAd.multimedia.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by ifensi on 2017/2/22.
 */

public class LimitNetBean {
    @SerializedName("packageName")
    public ArrayList<PackageName> packageNameList;

    @SerializedName("shell")
    public ArrayList<LimitNetParam> shellList;
    @SerializedName("unShell")
    public ArrayList<UnShellParam> unShellList;

    @SerializedName("is_rule")
    public String is_rule;

    @Override
    public String toString() {
        return "LimitNetBean{" +
                "shellList=" + shellList +
                ", unShellList=" + unShellList +
                '}';
    }
}
