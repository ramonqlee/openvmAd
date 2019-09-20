package com.idreems.openvmAd.multimedia.media.ui.widget;

/**
 * Created by ramonqlee on 18/03/2017.
 */
public interface VEncoding {
    String H264 = "H264";
    String HEVC = "HEVC";

    String H265_ID = "h265";//仅仅用于url中识别

    boolean canSupport(String vEncoding);
}
