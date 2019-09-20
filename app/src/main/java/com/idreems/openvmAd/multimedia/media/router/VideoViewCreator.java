package com.idreems.openvmAd.multimedia.media.router;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvmAd.multimedia.media.ui.widget.VEncoding;
import com.idreems.openvmAd.multimedia.media.ui.widget.VideoViewInf;

/**
 * Created by ramonqlee on 18/03/2017.
 */

public abstract class VideoViewCreator implements VEncoding {
    abstract String condition();

    abstract VideoViewInf create(Context context);

    abstract String[] supportVEncodings();

    public boolean canSupport(String vEncoding) {
        if (TextUtils.isEmpty(vEncoding)) {
            return false;
        }

        for (String temp : supportVEncodings()) {
            if (TextUtils.isEmpty(temp)) {
                continue;
            }

            if (0 == temp.compareToIgnoreCase(vEncoding)) {
                return true;
            }
        }
        return false;
    }
}
