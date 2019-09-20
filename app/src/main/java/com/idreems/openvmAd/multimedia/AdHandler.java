package com.idreems.openvmAd.multimedia;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.callback.ListHttpCallback;
import com.idreems.openvmAd.utils.Config;
import com.idreems.openvmAd.utils.DeviceUtils;
import com.idreems.openvmAd.utils.ReusableTimer;
import com.idreems.openvmAd.utils.TimeUtil;
import com.idreems.openvmAd.utils.ToastUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;

/**
 * Created by Administrator on 2018/3/14.
 */
public abstract class AdHandler implements DeviceChangable {
    private static final int MAX_UPLOADED_AD_ARRARY_SIZE = 1000;

    private String mMac;
    private static JSONArray sThirdAdUploadJsonArray = new JSONArray();//待上传的广告列表
    private static Config sPlatformRequestConfig;
    private String mSwitchString;//广告位开关设置
    private JSONObject mOption;//额外的参数配置

    protected Map<String, ReusableTimer> mUri2DelayTaskMap = new HashMap<>();//广告和延时上报任务的map
    private ArrayList<String> mUploadedAdIdArray = new ArrayList<>();
    protected IFSMedia mMedia;
    private boolean mPreMode;
    private long mMockMacChangedTimeInMs;//设置模拟mac地址的时间，用于代替其他设备请求广告

    private boolean mSystemConfigedAdPlatform = true;//后台配置的，因为也有可能是后来某种他原因自己添加的（比如下单独发了GD广告请求）

    //是否自有平台广告
    public boolean isSelfAd() {
        return false;
    }

    //广告的优先级
    public abstract int priority();

    //广告开关的名字，需要和后台对应
    public abstract String getSwitchName();

    //广告平台id
    public abstract String getPlatformId();

    public abstract AdHandler newInstance();

    public void setPreMode(boolean preMode) {
        mPreMode = preMode;
    }

    public boolean isPreloadMode() {
        return mPreMode;
    }

    public void setSystemConfigedAdPlatform(boolean enabled) {
        mSystemConfigedAdPlatform = enabled;
    }

    public boolean isSystemConfigedAdPlatform() {
        return mSystemConfigedAdPlatform;
    }

    public abstract void start(final Context context, final String adPosition);

    public abstract void setCompleteListener(ListHttpCallback l);

    public abstract void uploadAd(IFSMedia ifsMedia, boolean complete);

    //保留最近的100条上传记录，如果已经上传过，则返回true，否则返回false
    public boolean uploadedAd(IFSMedia ifsMedia) {
        // TODO 防止重复上传
        if (null == ifsMedia || TextUtils.isEmpty(ifsMedia.getPlatform())) {
            return false;
        }
        //保留最近的100条上传记录，如果已经上传过，则返回true，否则返回false
        //不存在的话，添加进去，返回false；存在的话，直接返回true
        int pos = mUploadedAdIdArray.indexOf(ifsMedia.getRequestId());
        if (-1 == pos) {
            //是否超过了限制，超过的话，直接清理
            if (mUploadedAdIdArray.size() > MAX_UPLOADED_AD_ARRARY_SIZE) {
                mUploadedAdIdArray = new ArrayList<>();
            }
            mUploadedAdIdArray.add(ifsMedia.getRequestId());
            if (Consts.logEnabled()) {
                String msg = "now uploading ,adRequestId =" + ifsMedia.getRequestId();
                Log.d(Consts.AD_TAG, msg);
                ToastUtils.show(msg);
            }
           return false;
        }
        if (Consts.logEnabled()) {
            String msg = "uploadedAd already,adRequestId =" + ifsMedia.getRequestId();
            Log.d(Consts.AD_TAG, msg);
            ToastUtils.show(msg);
        }
        return true;
    }


    public void setAdOption(JSONObject object) {
        try {
            if (null == object || !(object instanceof JSONObject)) {
                return;
            }

            mOption = object.optJSONObject(getSwitchName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //每天每个平台可以展示广告的次数
    public int getMaxPlayPerDay() {
        final int defVal = 10000;
        return defVal;
    }

    public boolean uploadRequestResult() {
        return false;
    }

    //是否允许上报探针数据
    public boolean isProbeAllowed() {
        return false;
    }

    //设置当前广告平台的广告位开关
    public void setAdPositionSwitch(JSONObject jsonObject) {
        if (null == jsonObject) {
            return;
        }
        mSwitchString = jsonObject.optString(getSwitchName());
    }

    //当前平台的当前广告位是否开放
    public boolean isOpenAdPosition(String adPostion) {
        if (TextUtils.isEmpty(adPostion)) {
            return false;
        }
        if (null == mSwitchString) {//没设置之前，默认所有广告位全开
            return true;
        }

        if (TextUtils.isEmpty(mSwitchString)) {
            return false;
        }
        if (Consts.logEnabled()) {
            Log.d(Consts.AD_TAG, "isOpenAdPosition mSwitchString = " + mSwitchString + " adPostion=" + adPostion);
        }

        String arr[] = mSwitchString.split(",");
        if (0 == arr.length) {
            return false;
        }
        for (String item : arr) {
            if (TextUtils.equals(item, adPostion)) {
                return true;
            }
        }
        return false;
    }

    //当前广告平台，是否还有剩余的广告展示次数
    //是否可请求百度广告:目前是设置当天的请求量
    public boolean canRequestAd() {
        String platform = getPlatformId();
        final String key = makeCanRequestAdConfigKey(platform);
        // 和本地缓存的数据进行比对
        final Config config = makesurePlatformRequestConfigInited();
        String val = config.getValue(key);
        if (TextUtils.isEmpty(val) || !TextUtils.isDigitsOnly(val)) {
            val = "0";
            config.saveValue(key, val);//reset
        }

        // 获取广告播放的日期，如果和当前时间不一致，则清理数据
        final String platformDateKey = makePlatformDateKey(platform);
        final String configDate = config.getValue(platformDateKey);
        final String today = TimeUtil.formatTimeWithYMD(TimeUtil.getCheckedCurrentTimeInMills());
        // 如果不是同一天，则重置数据(为防止由于时间未同步导致的误伤，只重置获取的值，不更新本地缓存)，同时更新日期
        if (!TextUtils.equals(configDate, today)) {
            val = "0";
            config.saveValue(platformDateKey, today);
        }

        try {
            int valInt = Integer.parseInt(val);
            boolean r = valInt < getMaxPlayPerDay();//没超过当天的限制
            if (Consts.logEnabled()) {
                Log.d(Consts.AD_TAG, "canRequestAd for platform " + getSwitchName() + " maxPlayPerDay = " + getMaxPlayPerDay() + " current playCount=" + valInt + " date = " + today);
            }
            return r;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    // 增加当前广告的请求成功量：按天进行计算
    public void incrementAdRequestCount() {
        String platform = getPlatformId();
        //缓存到本地
        // 记录下当前的日期
        final Config config = makesurePlatformRequestConfigInited();
        final String key = makeCanRequestAdConfigKey(platform);
        String val = config.getValue(key);
        if (TextUtils.isEmpty(val) || !TextUtils.isDigitsOnly(val)) {
            val = "0";
        }

        final String platformDateKey = makePlatformDateKey(platform);
        final String configDate = config.getValue(platformDateKey);
        final String today = TimeUtil.formatTimeWithYMD(TimeUtil.getCheckedCurrentTimeInMills());
        // 如果不是同一天，则清理数据，同时更新日期
        if (!TextUtils.equals(configDate, today)) {
            val = "0";
            config.saveValue(platformDateKey, today);
        }

        try {
            int valInt = Integer.parseInt(val);
            config.saveValue(key, String.valueOf(++valInt));
            if (Consts.logEnabled()) {
                Log.d(Consts.AD_TAG, "incrementAdRequestCount for platform = " + getSwitchName() + " to val = " + valInt + " date=" + today);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // util methods
    private Config makesurePlatformRequestConfigInited() {
        if (null != sPlatformRequestConfig) {
            return sPlatformRequestConfig;
        }
        sPlatformRequestConfig = new Config(MyApplication.getContext(), "PlatformRequestConfig");
        return sPlatformRequestConfig;
    }

    private static String makeCanRequestAdConfigKey(String platform) {
        return "canRequestAd_" + platform;
    }

    private static String makePlatformDateKey(String platform) {
        return "date_" + platform;
    }

    //启动延时上报广告任务，防止请求到广告，漏掉上传的情况
    protected void startAdUploadDelayTask(final IFSMedia media) {
        if (null == media) {
            return;
        }

        //TODO 启动延时上报广告任务，防止请求到广告，漏掉上传的情况
        // 1. 根据广告id，创建延时上报任务
        ReusableTimer timer = new ReusableTimer();
        long period = media.getEndTimeInSec();
        if (period <= 0) {
            final int min = 18;
            final int max = 25;
            Random random = new Random();
            period = random.nextInt(max) % (max - min + 1) + min;
            if (Consts.logEnabled()) {
                String msg = "startAdUploadDelayTask abnormal upload period,reset to " + period;
                Log.d(Consts.AD_TAG, msg);
            }
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //删除任务
                stopAdUploadDelayTask(media);

                //第三方的，需要上传到我们自己的后台
                if (!isSelfAd()) {
                    uploadThirdAd(media, getSwitchName());
                }
                uploadAd(media, true);
                if (Consts.logEnabled()) {
                    String msg = "startAdUploadDelayTask runTask getSwitchName=" + getSwitchName() + " requestId =" + media.getRequestId();
                    Log.d(Consts.AD_TAG, msg);
                    ToastUtils.show(msg);
                }
            }
        }, period * Consts.ONE_SECOND_IN_MS);//延时18-25之间的随机时间后上传

        mUri2DelayTaskMap.put(media.getRequestId(), timer);
        if (Consts.logEnabled()) {
            String msg = "startAdUploadDelayTask addTask getSwitchName=" + getSwitchName() + " requestId =" + media.getRequestId() + " period=" + period;
            Log.d(Consts.AD_TAG, msg);
            ToastUtils.show(msg);
        }
    }

    protected void stopAdUploadDelayTask(final IFSMedia media) {
        if (null == media) {
            return;
        }
        try {
            ReusableTimer timer = mUri2DelayTaskMap.remove(media.getRequestId());
            if (null == timer) {
                return;
            }
            timer.cancel();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // 上传第三方的广告播放记录
    public void uploadThirdAd(IFSMedia ifsMedia, String adPlatformSwitchName) {
    }

    @Override
    public void setDeviceId(String mac) {
        mMac = mac;
    }

    @Override
    public String getDeviceId() {
        return TextUtils.isEmpty(mMac) ? DeviceUtils.getPreferedMac() : mMac;
    }

    public long getMockMacChangedTimeInMs() {
        return mMockMacChangedTimeInMs;
    }

    public void setMockMacChangedTimeInMs(long mockMacChangedTimeInMs) {
        this.mMockMacChangedTimeInMs = mockMacChangedTimeInMs;
    }

    //是否可以请求mock的mac地址
    public boolean canChangeMockMac() {
        return Math.abs(System.currentTimeMillis() - mMockMacChangedTimeInMs) > 10 * Consts.ONE_MINUTE_IN_MS;
    }
}
