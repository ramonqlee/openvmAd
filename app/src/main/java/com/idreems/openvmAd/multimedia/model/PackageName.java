package com.idreems.openvmAd.multimedia.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ramonqlee on 29/03/2017.
 */

public class PackageName {
    @SerializedName("packageName")
    public String packageName;

    public PackageName() {
    }

    public PackageName(String param) {
        this.packageName = param;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || !(obj instanceof PackageName)) {
            return false;
        }
        PackageName temp = (PackageName) obj;
        return TextUtils.equals(temp.packageName, packageName);
    }

    @Override
    public String toString() {
        return "PackageName{" +
                "packageName='" + packageName + '\'' +
                '}';
    }
}
