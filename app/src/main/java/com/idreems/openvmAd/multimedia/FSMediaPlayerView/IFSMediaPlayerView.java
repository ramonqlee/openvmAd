package com.idreems.openvmAd.multimedia.FSMediaPlayerView;

import android.view.View;
import android.view.ViewGroup;

import com.danikula.videocache.HttpProxyCacheServer;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;

/**
 * Created by ramonqlee on 5/19/16.
 */
public interface IFSMediaPlayerView {
    void setVideoViewParent(ViewGroup parent);

    // 是否后台模式
    void setBackgoundMode(boolean backgoundMode);

    View getPlayerView();
    void playAnimation(boolean enable);

    void setHttpProxyCacheServer(HttpProxyCacheServer cacheServer);

    void showProgress(boolean show);

    void updateData(IFSMedia media);

    IFSMedia getMedia();

    void setOnPreparedListener(OnPreparedListener listener);

    void setOnCurrentTimeListener(OnCurrentTimeUpdateListener listener);

    void setOnCompletionListener(OnCompletionListener listener);

    void setOnErrorListener(OnErrorListener listener);

    void start();

    void stop();

    void release();

    void setGone();

    void setDimension(int width, int height);

    /*--------------------
     * Listeners
     */
    interface OnPreparedListener {
        void onPrepared(IFSMediaPlayerView mp);
    }

    interface OnCompletionListener {
        void onCompletion(IFSMediaPlayerView mp, boolean isEnd);
    }

    interface OnCurrentTimeUpdateListener {
        void onCurrentTimeUpdate(IFSMediaPlayerView mp, int currentTime);
    }

    interface OnErrorListener {
        boolean onError(IFSMediaPlayerView mp, int what, int extra);
    }

}
