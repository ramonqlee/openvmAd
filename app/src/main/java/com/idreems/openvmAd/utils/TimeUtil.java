package com.idreems.openvmAd.utils;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ramonqlee on 7/10/16.
 */
public class TimeUtil {
    private static long sLastTimeOffset;

    // 获取校对后的时间
    public static long getCheckedCurrentTimeInMills() {
        return System.currentTimeMillis() + sLastTimeOffset;
    }

    public static long getLastCheckTimeOffsetInMs(Context context) {
        return sLastTimeOffset;
    }

    public static void setLastCheckTimeOffsetInMs(Context context, long time) {
        sLastTimeOffset = time;
    }

    public static String formatFullTime(long timeInMills) {
        try {
            SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdr.format(new Date(timeInMills));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String formatTime(long timeInMills) {
        try {
            SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdr.format(new Date(timeInMills));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String formatTimeWithYMD(long timeInMills) {
        try {
            SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd");
            return sdr.format(new Date(timeInMills));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
