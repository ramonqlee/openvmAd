package com.idreems.openvmAd.multimedia;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.FSIndexedArray;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.callback.HttpCallback;
import com.idreems.openvmAd.multimedia.callback.ListHttpCallback;
import com.idreems.openvmAd.multimedia.media.AdBody;
import com.idreems.openvmAd.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.idreems.openvmAd.constant.Consts.PLATFORM_IFSAD;

/**
 * Created by ramonqlee on 7/27/16.
 */
public class NiuquAdRequester extends AdHandler {
    private static final String TAG = NiuquAdRequester.class.getSimpleName();
    private static final long MIN_AD_REQ_INTERVAL_MS = 5 * 60 * 1000;//自有广告，两次广告请求的最小时间间隔
    private static NiuquAdRequester instance = null;
    protected ListHttpCallback mListener;
    protected List<AdBody> mAdList = new ArrayList<>();
    private boolean mIsRequesting = false;
    private boolean mPriorityAdRequested;//优先级广告是否请求了

    private static long sAdlastPlayTime = 0;
    private FSIndexedArray<IFSMedia> mIndexedMediaArray = new FSIndexedArray<>();
    private Map<String, FSIndexedArray<IFSMedia>> mAdPosition2AdMap = new HashMap<>();
    private long mLastestAdRequest;//最近一次广告请求的时间

    public NiuquAdRequester() {
    }

    public AdHandler newInstance() {
        return new NiuquAdRequester();
    }

    public static NiuquAdRequester getInstance() {
        if (instance == null) {
            instance = new NiuquAdRequester();
        }
        return instance;
    }

    public boolean isAdListEmpty() {
        return null == mAdList || mAdList.isEmpty();
    }

    public String getPlatformId() {
        return PLATFORM_IFSAD;
    }

    /**
     * 设置请求完毕的回调
     */
    public void setCompleteListener(ListHttpCallback l) {
        mListener = l;
    }

    public ListHttpCallback getCompleteListener() {
        return mListener;
    }

    public void start() {
        start(MyApplication.getContext(), "");
    }

    public void start(final Context context, final String adPosition) {
        //设定广告请求的最小时间间隔：最近是否请求过，并且有广告返回
        final IFSMedia nextMedia = getNextAd(adPosition);
        if (Math.abs(mLastestAdRequest - System.currentTimeMillis()) < MIN_AD_REQ_INTERVAL_MS && null != nextMedia) {
            //直接返回下次展示的广告就好
            if (null != mListener) {
                mListener.onResponse(nextMedia);
            }
            return;
        }

        if (mIsRequesting) {
            return;
        }
        mIsRequesting = true;//在请求中的标志
        mLastestAdRequest = System.currentTimeMillis();

        // 1. 后台配置接口，请求数据
        final String url = String.format("%s/ad/list", Consts.getApiBaseUrl());
        NetworkUtils.getAndResponseOnMainThread(url, new HttpCallback() {
            @Override
            public void onFailure(IOException e) {
                mIsRequesting = false;
                if (Consts.logEnabled()) {
                    Log.d(Consts.LOG_TAG, "MainActivity onCreate -->onFailure");
                }
            }

            @Override
            public void onResponse(String response, boolean isCache) {
                try {
                    mIsRequesting = false;
                    if (Consts.logEnabled()) {
                        Log.d(Consts.AD_TAG, "response = " + response);
                    }
                    JSONObject obj = new JSONObject(response);
                    List<IFSMedia> retMedias = parseAdList(obj.optJSONArray("data"));
                    AdFileCache.preloadMediaSet(retMedias);
                    updateData(retMedias);

                    if (null == mListener) {
                        return;
                    }

                    if (null != retMedias && !retMedias.isEmpty()) {
                        IFSMedia nextMedia = getNextAd(adPosition);
                        mListener.onResponse(nextMedia);
//                        mListener.onResponse(retMedias.get(0));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, 0);
    }

    private static List<IFSMedia> parseAdList(JSONArray data) {
        if (null == data) {
            return new ArrayList<>();
        }
        List<IFSMedia> ret = new ArrayList<>();
        try {
            for (int i = 0; i < data.length(); ++i) {
                ret.add(new AdBody(data.getJSONObject(i)).toIFSMedia());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ret;
    }

    //是否自有平台广告
    public boolean isSelfAd() {
        return true;
    }

    //广告的优先级
    public int priority() {
        return 1;
    }

    //务必和后台第三方配置，数据中的名字为ThirdAd对应
    public String getSwitchName() {
        return "NiuQu";
    }

    //    private static final String THIRD_AD_DETAIL_INFO_KEY = "adThirdDetailInfo";
    public FSIndexedArray<IFSMedia> getClassifiedAds(final String adPosition) {
        try {
            FSIndexedArray<IFSMedia> ret = mAdPosition2AdMap.get(adPosition);
            if (null != ret) {
                return ret;
            }
            //lazy loading
            ret = new FSIndexedArray<>();
            mAdPosition2AdMap.put(adPosition, ret);

            //遍历广告数据，提取当前广告的数据，然后分类
            for (int i = 0; i < mIndexedMediaArray.size(); ++i) {
                IFSMedia media = mIndexedMediaArray.get(i);

                if (null == media || !TextUtils.equals(adPosition, media.getAd_position())) {
                    continue;
                }
                ret.add(media);
            }

            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // 获取下一个待播放的广告
    public IFSMedia getNextAd(String position) {
        // 按照广告位，进行数据的分别管理
        if (TextUtils.isEmpty(position)) {
            return null;
        }

        // Lazy loading now
        FSIndexedArray<IFSMedia> array = getClassifiedAds(position);
        if (null == array || 0 == array.size()) {
            return null;
        }

        int pos = array.position();
        IFSMedia temp = array.getCurrent();
        if (!isPreloadMode()) {
            array.increment();//increment for next ad return
        }
        if (Consts.logEnabled()) {
            Log.d(Consts.AD_TAG, "return adPosition = " + position + " isPreloadMode=" + isPreloadMode() + " at index=" + pos + " next index=" + array.position() + " array.size=" + array.size());
        }

        //启动自动的上报机制
        startAdUploadDelayTask(temp);

        incrementAdRequestCount();//请求成功，增加当天的请求条数
        return temp;
    }

    /**
     * 更新数据
     *
     * @param list
     */
    private void updateData(List<IFSMedia> list) {
        if (null == list || list.isEmpty()) {
            mIndexedMediaArray.clear();
            return;
        }

        // 如果数据没有发生变化，则保留上次的数据
        List<IFSMedia> originList = mIndexedMediaArray.toList();
        if (null != originList
                && originList.size() == list.size()
                && originList.containsAll(list)) {
            if (Consts.logEnabled()) {
                Log.d(Consts.AD_TAG, TAG + " update content");
            }
            mIndexedMediaArray.updateAll(list);

            return;
        }

        mIndexedMediaArray.clear();
        mIndexedMediaArray.addAll(list);
        if (Consts.logEnabled()) {
            Log.d(Consts.AD_TAG, TAG + " reset content,size = " + list.size());
        }
        mAdPosition2AdMap = new HashMap<>();//reset 按照广告分类的map，等待用的时候，再初始化
    }

    public void uploadAd(IFSMedia ifsMedia, boolean complete) {
        if (Consts.logEnabled()) {
            Log.d(TAG, "startSelfAdUpload ifsMedia->" + ifsMedia);
        }

//        if (null == ifsMedia || TextUtils.isEmpty(ifsMedia.getPlatform())) {
//            if (Consts.logEnabled()) {
//                Log.d(TAG, "startSelfAdUpload empty getPlatform");
//            }
//            return;
//        }
//
//        // 正常播放完毕了，走现有流程上传，同时删除延时任务中的任务
//        stopAdUploadDelayTask(ifsMedia);
//
//        if (Consts.logEnabled()) {
//            Log.d(TAG, "startSelfAdUpload now");
//        }
//        startSelfAdUpload(ifsMedia.getId(), ifsMedia.getAd_position());
    }

    public static JSONArray sIFSAdJsonArray = new JSONArray();
    private static Map<String, Long> sAdId2PlayCount = new HashMap<>();//广告id和播放次数的map


    private void startSelfAdUpload(String adId, String ad_position) {
    }


    private static void upLoadAd(final String url, final Map<String, String> map) {
    }


    private static void clearAdCache() {
        sAdId2PlayCount.clear();
        sIFSAdJsonArray = new JSONArray();
        if (Consts.logEnabled()) {
            Log.d(TAG, "startSelfAdUpload clearAdCache");
        }
    }
}
