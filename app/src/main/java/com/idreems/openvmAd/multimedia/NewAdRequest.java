package com.idreems.openvmAd.multimedia;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.FSIndexedArray;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.admanager.AdListener;
import com.idreems.openvmAd.multimedia.admanager.LoopAdRequestManager;
import com.idreems.openvmAd.multimedia.admanager.SeqAdRequestManager;
import com.idreems.openvmAd.multimedia.callback.DataHttpCallback;
import com.idreems.openvmAd.multimedia.model.AdPopertyBean;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/3/8.
 */
public class NewAdRequest {
    private static NewAdRequest instance = null;
    private String sLoopModeAdPositionConfig;
    private Map<String, AdHandler> sAdHandlerMap = new HashMap<>();
    private Map<String, LoopAdRequestManager> mLoopAdRequestManagerMap = new HashMap<>();
    private Map<String, SeqAdRequestManager> mSeqAdRequestManagerMap = new HashMap<>();

    private NewAdRequest() {
    }

    public static NewAdRequest getInstance() {
        if (instance == null) {
            instance = new NewAdRequest();
        }
        return instance;
    }

    public Map<String, AdHandler> getAdHandlerMap() {
        initMapHanders();
        return sAdHandlerMap;
    }

    //初始化请求管理器
    public void initSelfAdService() {
        //从互动自己的广告接口，获取初始化数据，包括广告优先级
        NiuquAdRequester.getInstance().start();
    }

    public void preloadThirdAd(String adPosition) {
        if (Consts.logEnabled()) {
            Log.d(Consts.AD_TAG, "preloadThirdAd adPosition=" + adPosition);
        }
//        requestAd(MyApplication.getContext(), adPosition, null);
    }

    // 添加自定义广告请求处理器
    public void addCustomAdHandler(final String adPosition, final AdHandler handler) {
        if (TextUtils.isEmpty(adPosition) || null == handler) {
            return;
        }
        handler.setSystemConfigedAdPlatform(false);//设置为非系统配置的

        //mLoopAdRequestManagerMap是否存在
        if (null != mLoopAdRequestManagerMap) {
            LoopAdRequestManager manager = mLoopAdRequestManagerMap.get(adPosition);
            if (null != manager && manager.size() > 0) {
                // 添加到轮播器
                SeqAdRequestManager seqAdRequestManager = manager.get(0);
                seqAdRequestManager.add2Head(handler);
                return;
            }
        }

        //mSeqAdRequestManagerMap是否存在
        if (null != mSeqAdRequestManagerMap) {
            SeqAdRequestManager manager = mSeqAdRequestManagerMap.get(adPosition);
            if (null != manager) {
                // 添加到顺序请求器中
                manager.add2Head(handler);
                return;
            }
        }
    }

    // 重置广告请求处理器
    public void resetCustomAdHandlers(String adPosition) {
        // 删除自定义的广告请求器
//mLoopAdRequestManagerMap是否存在
        if (null != mLoopAdRequestManagerMap) {
            LoopAdRequestManager manager = mLoopAdRequestManagerMap.get(adPosition);
            if (null != manager && manager.size() > 0) {
                // 添加到轮播器
                SeqAdRequestManager seqAdRequestManager = manager.get(0);
                seqAdRequestManager.clearCustomAdHandlers();
                return;
            }
        }

        //mSeqAdRequestManagerMap是否存在
        if (null != mSeqAdRequestManagerMap) {
            SeqAdRequestManager manager = mSeqAdRequestManagerMap.get(adPosition);
            if (null != manager) {
                // 添加到顺序请求器中
                manager.clearCustomAdHandlers();
                return;
            }
        }
    }

    /*
    callback 为空时，可用于广告预加载
     */
    public void requestAd(Context outContext, final String adPosition, final DataHttpCallback callback) {
        if (TextUtils.isEmpty(adPosition)) {
            if (null == callback) {
                return;
            }
            callback.onResponse(null);
            return;
        }

        SeqAdRequestManager manager = mSeqAdRequestManagerMap.get(adPosition);
        if (null == manager) {
            manager = new SeqAdRequestManager();
            manager.add(NiuquAdRequester.getInstance());
            //添加一个替补
            manager.setCandidate(NiuquAdRequester.getInstance());
        }

        manager.setPreMode(null == callback);
        manager.setListener(new AdListener() {
            @Override
            public void onNotify(IFSMedia media) {
                if (null == callback) {
                    return;
                }
                callback.onResponse(media);
            }
        });
        manager.run(outContext, adPosition);
        mSeqAdRequestManagerMap.put(adPosition, manager);
    }

    //当前请求的广告位，是否支持轮播；如果不支持，则按照后台配置的优先级，优先播放；否则将采用轮播模式,播放一个第三方广告，播放一个自有广告
    public boolean isLoopModeAdPosition(String adPosition) {
        if (TextUtils.isEmpty(adPosition)) {
            return false;
        }

        //  没设置，则支持轮播
        if (TextUtils.isEmpty(sLoopModeAdPositionConfig)) {
            return true;
        }

        if (Consts.logEnabled()) {
//            Consts.logD(Consts.AD_TAG, "isLoopModeAdPosition sLoopModeAdPositionConfig = " + sLoopModeAdPositionConfig + " adPosition=" + adPosition);
        }

        String arr[] = sLoopModeAdPositionConfig.split(",");
        if (0 == arr.length) {
            return false;
        }

        for (String item : arr) {
            if (TextUtils.equals(item, adPosition)) {
                return true;
            }
        }
        return false;
    }

    private void setLoopModeAdPositionConfig(String config) {
        sLoopModeAdPositionConfig = config;
    }

    //设置各个平台，广告位的开关
    public void setAdConfig(JSONObject jsonObject) {
        initSelfAdService();
        if (null == jsonObject || !(jsonObject instanceof JSONObject)) {
            return;
        }

        // 设置广告轮播模式
        String loopModeStr = jsonObject.optString("LoopAdPosition");
        setLoopModeAdPositionConfig(loopModeStr);

        //  遍历后台设置的所有平台的广告开关，设置到对应的request中
        JSONObject adOptionJson = jsonObject.optJSONObject("Option");//广告的额外配置
        Map<String, AdHandler> map = getAdHandlerMap();
        for (Map.Entry<String, AdHandler> entry : map.entrySet()) {
            AdHandler handler = entry.getValue();
            if (null == handler) {
                continue;
            }
            handler.setAdPositionSwitch(jsonObject);//设置广告的开关配置
            handler.setAdOption(adOptionJson);//设置广告的额外配置，比如探针等
        }
    }

    // 获取新的广告处理器（可以不是后台配置的）
    public AdHandler getNewAdHandlerByName(String name) {
        // 修改为单独的获取方法，不判断是否系统后台配置
        AdHandler retHandler = null;

        Map<String, AdHandler> map = getAdHandlerMap();
        for (Map.Entry<String, AdHandler> entry : map.entrySet()) {
            AdHandler handler = entry.getValue();
            if (null == handler) {
                continue;
            }
            if (!TextUtils.equals(handler.getSwitchName(), name)) {
                continue;
            }

            retHandler = handler;
        }

        if (null == retHandler) {
            return retHandler;
        }
        return retHandler.newInstance();
    }

    // 获取单例版本的,后台配置的广告
    public AdHandler getSharedAdHandlerByName(String name) {
        Map<String, AdHandler> map = getAdHandlerMap();
        for (Map.Entry<String, AdHandler> entry : map.entrySet()) {
            AdHandler handler = entry.getValue();
            if (!TextUtils.equals(handler.getSwitchName(), name)) {
                continue;
            }
            return handler.isSystemConfigedAdPlatform() ? handler : null;//如果是后台配置的，则返回，否则不返回
        }
        return null;
    }

    private void initMapHanders() {
        if (!sAdHandlerMap.isEmpty()) {
            return;
        }

        sAdHandlerMap.put(Consts.PLATFORM_NQAD, NiuquAdRequester.getInstance());
    }

    //根据后台配置，选择相应的广告请求器
    private void initSystemConfigAdManager(SeqAdRequestManager manager, List<AdPopertyBean> adPopertyBeanList) {
        if (null == manager || null == adPopertyBeanList || adPopertyBeanList.isEmpty()) {
            return;
        }

        try {
            Map<String, AdHandler> mapHandler = getAdHandlerMap();
            for (AdPopertyBean item : adPopertyBeanList) {
                if (null == item) {
                    continue;
                }

                AdHandler handler = mapHandler.get(item.getType());
                if (null == handler) {
                    continue;
                }
                // TODO 增加请求的mac
                handler.setSystemConfigedAdPlatform(true);
                manager.add(handler);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized boolean hasAd(String position) {
        FSIndexedArray<IFSMedia> mediaList = NiuquAdRequester.getInstance().getClassifiedAds(position);
        return null != mediaList && 0 != mediaList.size();
    }
}
