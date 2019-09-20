package com.idreems.openvmAd.multimedia.callback;


import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;

public interface DataHttpCallback {
    //数据返回，如果没有广告的话，media返回空
    void onResponse(IFSMedia media);
}
