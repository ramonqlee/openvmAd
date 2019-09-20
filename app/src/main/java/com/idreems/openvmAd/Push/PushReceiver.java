package com.idreems.openvmAd.Push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by ramonqlee on 6/16/16.
 */
public class PushReceiver extends BroadcastReceiver {
    /**
     * 应用未启动, 个推 service已经被唤醒,保存在该时间段内离线消息(此时 GetuiSdkDemoActivity.tLogView == null)
     */
//    public static StringBuilder payloadData = new StringBuilder();
    @Override
    public void onReceive(Context context, Intent intent) {
    }
}
