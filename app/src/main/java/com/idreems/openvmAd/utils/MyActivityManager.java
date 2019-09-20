package com.idreems.openvmAd.utils;

import android.app.Activity;

import java.lang.ref.WeakReference;

/**
 * Created by Administrator on 2017/4/26.
 */
public class MyActivityManager {

    private static MyActivityManager sInstance = new MyActivityManager();
    private WeakReference<Activity> sCurrentActivityWeakRef;

    private MyActivityManager() {
    }

    public static MyActivityManager getInstance() {
        return sInstance;
    }

    public Activity getCurrentActivity() {
        return (sCurrentActivityWeakRef != null) ? sCurrentActivityWeakRef.get() : null;
    }

    public void setCurrentActivity(Activity activity) {
        sCurrentActivityWeakRef = new WeakReference<Activity>(activity);
    }
}
