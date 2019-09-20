package com.idreems.openvmAd.multimedia.media.router;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.media.ui.widget.VEncoding;
import com.idreems.openvmAd.multimedia.media.ui.widget.VideoViewInf;
import com.idreems.openvmAd.utils.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 18/03/2017.
 */
// TODO 视频引擎router，将根据条件选择合适的视频引擎
public class VideoViewRouter {
    private static final String TAG = VideoViewRouter.class.getSimpleName();

    private static Map<String, VideoViewCreator> sViewCreatorMap = new HashMap<>();
    private static boolean sExtraLoaded;

    static {
        sViewCreatorMap.put(AndroidVideoViewCreator.MY_CONDITION.toUpperCase(), new AndroidVideoViewCreator());//android
        // FIXME TEMP CODE
        // 添加更多creator
    }

    // 返回第一个支持vEncod的引擎
    public static String getPlayerEngine(String vEncoding) {
        for (VideoViewCreator creator : sViewCreatorMap.values()) {
            if (null == creator) {
                continue;
            }
            if (creator.canSupport(vEncoding)) {
                return creator.condition();
            }
        }
        return "";
    }

    public static VideoViewInf createVideoView(Context context, String condition) {
        VideoViewCreator creator = getVideoViewCreator(condition);
        return creator.create(context.getApplicationContext());
    }

    // 能力检测
    public static boolean canSupport(String condition, String vEncoding) {
        VideoViewCreator creator = getVideoViewCreator(condition);
        if (!(creator instanceof VEncoding)) {
            return false;
        }

        VEncoding vEncodingInst = (VEncoding) creator;
        return vEncodingInst.canSupport(vEncoding);
    }

    private static VideoViewCreator getVideoViewCreator(String condition) {
        VideoViewCreator creator = null;
        String myCondition = condition;
        if (TextUtils.isEmpty(condition)) {
            myCondition = AndroidVideoViewCreator.MY_CONDITION;
        }
        dynamicLoadExtra();

        creator = sViewCreatorMap.get(myCondition.toUpperCase());
        if (null == creator) {
            creator = sViewCreatorMap.get(AndroidVideoViewCreator.MY_CONDITION);
        }
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "MediaPlayer creator = " + creator.getClass().getName());
        }
        return creator;
    }

    // 在此增加一些需要第三方的库
    private static void dynamicLoadExtra() {
        if (sExtraLoaded)
            return;
        sExtraLoaded = true;
    }
}
