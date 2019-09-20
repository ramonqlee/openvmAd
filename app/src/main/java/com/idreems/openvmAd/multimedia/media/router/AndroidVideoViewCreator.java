package com.idreems.openvmAd.multimedia.media.router;

import android.content.Context;

import com.idreems.openvmAd.multimedia.media.ui.widget.AndroidVideoViewWrapper;
import com.idreems.openvmAd.multimedia.media.ui.widget.VEncoding;
import com.idreems.openvmAd.multimedia.media.ui.widget.VideoViewInf;

/**
 * Created by ramonqlee on 18/03/2017.
 */

public class AndroidVideoViewCreator extends VideoViewCreator {
    public static final String MY_CONDITION = "VV";
    private static String sSupportVEncodings[] =
            {
                    VEncoding.H264, VEncoding.HEVC
            };

    public String condition() {
        return MY_CONDITION;
    }

    public VideoViewInf create(Context context) {
        return new AndroidVideoViewWrapper(context);
    }

    protected String[] supportVEncodings() {
        return sSupportVEncodings;
    }
}
