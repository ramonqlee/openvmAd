package com.idreems.openvmAd.multimedia.FSMediaPlayerView;


import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 5/19/16.
 */
public class FSMediaPlayerRecognizer {

    private Map<IFSMedia.MediaType, IFSMediaPlayerView> mPlayerViewMap = new HashMap<>();

    public synchronized void registerPlayerView(IFSMedia.MediaType type, IFSMediaPlayerView view) {
        if (null == view) {
            return;
        }
        mPlayerViewMap.put(type, view);
    }

    public synchronized void unregisterPlayerView(IFSMedia.MediaType mediaType) {
        if (null == mediaType) {
            return;
        }
        mPlayerViewMap.remove(mediaType);
    }

    public IFSMediaPlayerView getPlayerView(IFSMedia.MediaType mediaType) {
        if (null == mediaType) {
            return null;
        }
        return mPlayerViewMap.get(mediaType);
    }

    public synchronized void clear() {
        if (null == mPlayerViewMap || mPlayerViewMap.isEmpty()) {
            return;
        }
        for (IFSMediaPlayerView playerView : mPlayerViewMap.values()) {
            if (null == playerView) {
                continue;
            }
            playerView.stop();
        }

        mPlayerViewMap.clear();
    }

    public int size() {
        return mPlayerViewMap.size();
    }
}
