package com.idreems.openvmAd.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.file.FileUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ramonqlee on 4/19/16.
 */
public class Config {
    public static final String SERVER_ENV_KEY = "server_env";// 后续用于识别本地环境
    public static final String LAST_TIME_OFFSET = "last_time_offset";   // 上次同步的时间差：服务器和本地的时间差
    public static final String EXTERNAL_APP_CONFIG = "external_app_config";//外部应用app的配置参数

    public static final String VISIBLE_RES_WIDTH = "width";
    public static final String VISIBLE_RES_HEIGHT = "height";
    public static final String AD_PLAY_TIME = "ad_play_time";
    public static final String MIN_AD_UPLOAD_KEY = "min_ad_upload_key";
    public static final String SPLASH_MSG = "splash_msg";
    public static final String BOOTSTRAP_PIC = "bootstrap_pic";//企业定制页的模板
    public static final String BACKGROUND_IMG = "background_img";
    public static final String QRCODE_BG = "code_bg";
    public static final String QRCODE_X = "code_x";
    public static final String QRCODE_Y = "code_y";
    public static final String PHONE_BG = "phone_bg";
    public static final String SCREEN_SIZE = "screen_size";
    public static final String PHONE_X = "phone_x";
    public static final String PHONE_Y = "phonee_y";
    public static final String ACTIVE1 = "activeInfo1";
    public static final String ACTIVE2 = "activeInfo2";
    public static final String ACTIVE3 = "activeInfo3";
    public static final String SYSTEM_ENTER_IMG = "system_entry_img";
    public static final String SYSLOGOUTIMG = "sys_logout_img";

    public static final String TIME = "time";
    public static final String REBOOT = "reboot";
    public static final String FIRST_TOUCHTIME = "first_touch_time";

    public static final String IS_CUSTOM = "is_custom";
    public static final String UI_TYPE = "ui_type";
    public static final String SPLASH_URL = "splash_url";
    public static final String SPLASH_LOGO_URL = "splash_logo_url";

    public static final String WIFI_MAC_ID_KEY = "WIFI_MAC_ID_KEY";
    public static final String ETHERNET_MAC_ID_KEY = "ETHERNET_MAC_ID_KEY";

    public static final String PRE_WIFI_MAC_KEY = "PREV_WIFI_MAC_KEY";//之前的wifi mac地址
    public static final String PRE_ETHERNET_MAC_KEY = "PREV_ETHERNET_MAC_KEY";//之前的wifi mac地址

    public static final String SAVED_MAC_KEY = "LOCAL_SAVED_MAC_KEY";//对于有线和无线地址都发生变化的情况，记录到本地

    public static final String ENTER_GROUPID_KEY = "enter_group";
    public static final String POPUP_WINDOWS_TIP_INDEX = "POPUP_WINDOWS_TIP_INDEX";


    public static final String XYCHANGE_TIME = "xychange_time";

    public static final String KARTUN_TIME = "kartun_time";//卡顿+暂停
    public static final String GROUP_ID = "group_id";
    public static final String LOCALMENUTYPE = "localMenuType";

//    public static final String PLAYER_ENGINE_INFO = "playerEnine";//播放引擎

    public static final String GUIDE_NUM = "guide_num";
    public static final String LAST_TIME_START_UP = "LAST_TIME_START_UP";

    public static final String LOCAL_FIND_KEY = "localServerIsFind";
    public static final String LOCAL_SERVER_CONTENT_KEY = "localServerConent";
    public static final String LOCAL_SERVER_IP_KEY = "LOCAL_SERVER_IP_KEY";
    public static final String LOCAL_SERVER_FIND_ONE_BY_NE_IP_KEY = "FIND_ONE_BY_ONE";

//    public static final String  HomePageAdClick = "HomePageAdClick";//首页广告点击跳转

    // 视频状态变量
    public static final String PLAYER_TRACER_SW_DECODE = "SW";//软件解码
    public static final String PLAYER_TRACER_PLAY_STATE = "PL";//播放状态
    public static final String PLAYER_TRACER_CACHE_COUNT = "CA";//播放过程中卡顿的次数
    public static final String PLAYER_TRACER_VIDEO_LENGTH = "LEN";//视频时长
    public static final String PLAYER_TRACER_PLAY_ERROR = "ERR";//视频播放过程中是否出错
    public static final String PLAYER_TRACER_COMPLETE_POSITION = "END";//视频播放是否提前结束
    public static final String PLAYER_TRACER_QUIT_FLAG = "QT";//视频是否正常退出
    public static final String PLAYER_TRACER_PLAY_SOURCE = "FR";
    public static final String PLAYER_TRACER_PLAY_ENCODING = "CD";

    public static final String PUSH_CID_KEY = "push_id";//push id
    public static final String PUSH_UID_KEY = "push_uid_key";//push_uid

    public static final String GZIP_ENABLED_KEY = "gzip_enabled_key";//gzip key
    public static final String TURN_ON_LOG_KEY = "TURN_ON_LOG_KEY";//TURN_ON_LOG_KEY
    public static final String QRCODE_EXPIRE_INMILLS_KEY = "QRCODE_EXPIRE_INMILLS_KEY";
    public static final String PREVIOUS_VERSION_CODE = "previous_version_code";//上次运行的版本号
    public static final String CURRENT_VERSION_CODE = "current_version_code";//当前运行的版本号

    public static final String OLD_PREF_FILE_NAME = "sharedPref";
    public static final String SYSTEM_UPDATE = "system_update";
    public static final String SYSTEM_UPDATE_NUM = "system_update_num";
    public static final String SYSTEM_UPDATE_TIME = "system_update_time";
    public static final String CLEAR_DATA_TIME = "clear_dataTime";
    public static final String JD_UPDATA_TIME = "jdup_dataTime";
    public static final String JD_UPDATA_DATA = "jdup_data";
    public static final String CLEAR_IMAGE = "clearImage";
    private static final String PREF_FILE_NAME = "sharedPref4GlobalOnly";
    private static final String XML_FILE_SUFFIX = ".xml";
    public static String API_CACHE_UID = "f0c5603997c943749eb87daa0bcc8f55_";
    public static final String RECODE_TIME = "recode_time";
    public static final String RECODE_THIRD = "recode_third";

    //腾讯
    public static final String TENCENT_CID = "tencent_cid";


    private static final int MAX_FILE_COUNT = 1000;//最多缓存的文件(40K/file)
    private static Config sConfig;
    private static Map<String, String> sConfigCache = new HashMap<>();
    private SharedPreferences mPref;

    private Config(Context context) {
        if (null != context) {
            mPref = context.getApplicationContext().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        }
    }

    public Config(Context context, String fileName) {
        if (null == context || TextUtils.isEmpty(fileName)) {
            return;
        }

        mPref = context.getApplicationContext().getSharedPreferences(fileName, Context.MODE_PRIVATE);
        clearCacheConfig(MAX_FILE_COUNT);
    }

    public static String getAPICachePrefix(String versionCode) {
        if (TextUtils.isEmpty(versionCode)) {
            return API_CACHE_UID;
        }
        return API_CACHE_UID + "_" + "vc_" + versionCode + "_";
    }

    /**
     * 将文件按时间降序排列
     */
    static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File file1, File file2) {
            if (file1.lastModified() < file2.lastModified()) {
                return 1;// 最后修改的文件在前
            } else {
                return -1;
            }
        }
    }

    //清理本地缓存文件
    private static long sLastClearTimeInMs;

    private static void clearCacheConfig(final int maxFileCount) {
        if (Math.abs(SystemClock.elapsedRealtime() - sLastClearTimeInMs) < 10 * Consts.ONE_MINUTE_IN_MS) {
            return;
        }

        if (Consts.logEnabled()) {
            Log.d(Consts.AD_TAG,"clearCacheConfig,sLastClearTimeInMs =" + sLastClearTimeInMs + " SystemClock.elapsedRealtime()=" + SystemClock.elapsedRealtime());
        }

        sLastClearTimeInMs = SystemClock.elapsedRealtime();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    // 清理API文件(getAPICachePrefix)
                    final List<File> vecFile = new ArrayList<File>();
                    String cacheDirFile = MyApplication.getContext().getCacheDir().getAbsolutePath();
                    String sharePrefsDir = cacheDirFile.replace("cache", "shared_prefs");
                    File file = new File(sharePrefsDir);
                    final String APICachePrefix = getAPICachePrefix("");
                    file.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if (null == pathname || pathname.isDirectory()) {
                                return false;
                            }

                            if (-1 == pathname.getAbsolutePath().indexOf(APICachePrefix)) {
                                return false;
                            }

                            vecFile.add(pathname);
                            return true;
                        }
                    });

                    if (vecFile.size() < maxFileCount) {
                        return;
                    }

                    // TODO 排序，删除较老的文件
                    Collections.sort(vecFile, new FileComparator());

                    //删除后面的文件
                    for (int i = vecFile.size(); i < maxFileCount; i++) {
                        FileUtil.deleteFile(vecFile.get(i).getAbsolutePath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        };
        TaskUtils.runOnFixedExecutorService(r);

    }

    //清理单个旧的数据
    public static void clearOldConfig(final Context context, final String fileName) {
        if (null == context || TextUtils.isEmpty(fileName)) {
            return;
        }

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    //  清除旧的文件
                    SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(fileName, Context.MODE_PRIVATE).edit();
                    editor.clear();
                    editor.commit();

                    // 尝试删除文件
                    String cacheDirFile = context.getCacheDir().getAbsolutePath();
                    String sharePrefsDir = cacheDirFile.replace("cache", "shared_prefs");
                    String xmlFile = String.format("%s/%s", sharePrefsDir, fileName);
                    if (!xmlFile.contains(XML_FILE_SUFFIX)) {
                        xmlFile += XML_FILE_SUFFIX;
                    }

                    FileUtil.deleteFile(xmlFile);
                    if (Consts.logEnabled()) {
                        Log.d(Consts.LOG_TEST_TAG, "deleteFile = " + xmlFile);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        TaskUtils.runOnExecutorService(r);
    }

    public static Config sharedInstance(Context context) {
        if (null != sConfig) {
            return sConfig;
        }

        if (null == context) {
            return null;
        }
        sConfig = new Config(context);
        return sConfig;
    }

    public String getValue(String key) {
        try {
            if (TextUtils.isEmpty(key)) {
                return "";
            }

            //如果已经缓存过，直接从缓存中获取
            String v = sConfigCache.get(key);
            if (null != v) {
                return v;
            }

            v = mPref.getString(key, "");

            //添加到缓存
            sConfigCache.put(key, v);
            return v;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public void saveValue(String key, String value) {
        try {
            if (TextUtils.isEmpty(key)) {
                return;
            }

            SharedPreferences.Editor editor = mPref.edit();
            editor.putString(key, value);
            editor.apply();

            sConfigCache.put(key, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public long getLong(String key) {
        final long defValue = 0;
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }

        try {
            String v = sConfigCache.get(key);
            if (!TextUtils.isEmpty(v) && TextUtils.isDigitsOnly(v)) {
                return Long.valueOf(v);
            }
            long val = mPref.getLong(key, defValue);

            // 添加到缓存
            sConfigCache.put(key, String.valueOf(val));
            return val;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return defValue;
    }

    public void saveLong(String key, long value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }

        try {
            SharedPreferences.Editor editor = mPref.edit();
            editor.putLong(key, value);
            editor.apply();

            // 添加到缓存
            sConfigCache.put(key, String.valueOf(value));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void removeKey(String key) {
        try {
            SharedPreferences.Editor editor = mPref.edit();
            editor.remove(key);

            sConfigCache.remove(key);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
