package com.idreems.openvmAd.utils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by ramonqlee on 6/29/16.
 */
public class JsonUtils {
    public static final int INVALID_INT = -1;
    public static final long INVALID_LONG = -1L;

    // 缺省返回""
    public static Object getObject(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return null;
        }
        try {
            return object.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 缺省返回""
    public static JSONArray getJSONArray(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return null;
        }
        try {
            return object.getJSONArray(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 缺省返回""
    public static JSONObject getJsonObject(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return null;
        }
        try {
            return object.getJSONObject(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 缺省返回""
    public static String getString(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return "";
        }
        try {
            return object.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // 缺省返回-1
    public static long getLong(JSONObject object, String key) {
        if (null == object) {
            return INVALID_LONG;
        }
        if (!object.has(key)) {
            return -INVALID_LONG;
        }
        try {
            return object.getLong(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return INVALID_LONG;
    }

    // 缺省返回-1
    public static int getInt(JSONObject object, String key) {
        if (null == object) {
            return INVALID_INT;
        }
        if (!object.has(key)) {
            return INVALID_INT;
        }
        try {
            return object.getInt(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return INVALID_INT;
    }
}
