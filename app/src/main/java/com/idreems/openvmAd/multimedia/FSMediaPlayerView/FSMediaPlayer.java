package com.idreems.openvmAd.multimedia.FSMediaPlayerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.view.SimpleDraweeView;
import com.idreems.openvmAd.R;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.media.ui.widget.VideoViewInf;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 7/26/16.
 * 可以设定播放次数，然后回调
 */
public class FSMediaPlayer {
    private static String TAG = FSMediaPlayer.class.getSimpleName();
    HttpProxyCacheServer mCacheServer;
    private PlayerViewProvider mPlayerViewProvider = null;
    private List<PlayListener> mPlayerListeners = new ArrayList<>();
    private boolean mIsPlaying;
    private boolean mPaused;//是否暂停
    private boolean mShowProgress = true;
    private boolean mBackgroundMode;

    private FSMediaPlayerRecognizer mPlayerRecognizer = new FSMediaPlayerRecognizer();
    private Handler mUIHandler = new Handler(Looper.getMainLooper());

    public PlayerViewProvider getPlayerViewProvider() {
        return mPlayerViewProvider;
    }

    public void setPlayerViewProvider(PlayerViewProvider provider) {
        mPlayerViewProvider = provider;
    }

    public void setHttpProxyCacheServer(HttpProxyCacheServer cacheServer) {
        mCacheServer = cacheServer;
    }

    public void showProgress(boolean show) {
        mShowProgress = show;
    }

    /**
     * 开始播放，如果暂停的话，继续播放
     */
    public void start(IFSMedia media) {
        if (null == media) {
            return;
        }

        if (mIsPlaying) {
            if (Consts.logEnabled()) {
                Log.d(TAG, "FSMediaPlayer started player,ignore this request");
            }
            return;
        }

        mPaused = false;
        mIsPlaying = true;

        registerPlayerView();

        final IFSMediaPlayerView view = mPlayerRecognizer.getPlayerView(media.getMediaType());
        if (null == view) {
            mIsPlaying = false;
            return;
        }
        view.updateData(media);
        try {
            notifyOnPrepare2StartPlayerListener(media);
            view.setBackgoundMode(mBackgroundMode);
            view.showProgress(mShowProgress);

            if (Consts.logEnabled()) {
                Log.d(TAG, TAG + " start, start play media=" + media + " view =" + view);
            }

            view.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isPaused() {
        return mPaused;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void addPlayListener(PlayListener l) {
        if (null == l || -1 != mPlayerListeners.indexOf(l)) {
            return;
        }

        mPlayerListeners.add(l);
        if (Consts.logEnabled()) {
            Log.d(TAG, "addPlayListener = " + l + " newSize =" + mPlayerListeners.size());
        }
    }

    public void removePlayerLister(PlayListener l) {
        if (null == l || null == mPlayerListeners || mPlayerListeners.isEmpty()) {
            return;
        }
        mPlayerListeners.remove(l);
        if (Consts.logEnabled()) {
            Log.d(TAG, "removePlayerLister = " + l + " newSize =" + mPlayerListeners.size());
        }
    }

    public void clearPlayerListener() {
        if (null == mPlayerListeners || mPlayerListeners.isEmpty()) {
            return;
        }

        mPlayerListeners.clear();
        if (Consts.logEnabled()) {
            Log.d(TAG, "clearPlayerListener");
        }
    }

    public void stop() {
        // 是否有资源可以停止
        if (null == mPlayerRecognizer || 0 == mPlayerRecognizer.size()) {
            return;
        }
        if (!mIsPlaying) {
            return;
        }

        // 为安全起见，将所有的全部停止
        mIsPlaying = false;
        mPaused = false;
        ungisterPlayerView();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 暂停播放
     */
    public void pause() {
        pauseImp(true);
    }

    public void release() {
        if (null == mPlayerRecognizer || 0 == mPlayerRecognizer.size()) {
            return;
        }

        mIsPlaying = false;
        mPaused = false;
        releasePlayerView();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 暂停播放
     */
    private void pauseImp(boolean setFlag) {
        // 为安全起见，将所有的全部停止
        mIsPlaying = false;
        if (Consts.logEnabled()) {
            Log.d(TAG, "FSMediaPlayer pauseImp");
        }
        mPaused = setFlag;
        ungisterPlayerView();
    }

    private void notifyOnPrepare2StartPlayerListener(IFSMedia media) {
        if (Consts.logEnabled()) {
            Log.d(TAG, "notifyOnPrepare2StartPlayerListener");
        }
        try {
            List<PlayListener> listeners = new ArrayList(mPlayerListeners);
            for (PlayListener l : listeners) {
                if (null == l) {
                    continue;
                }
                //已经空了，直接返回吧
                if (null == mPlayerListeners) {
                    break;
                }

                // 检查是否还存在
                if (-1 == mPlayerListeners.indexOf(l)) {
                    continue;
                }
                l.onPrepareToStart(media);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void notifyOnPreparePlayerListener(IFSMedia media) {
        if (Consts.logEnabled()) {
            Log.d(TAG, "notifyOnPreparePlayerListener");
        }
        try {
            List<PlayListener> listeners = new ArrayList(mPlayerListeners);
            for (PlayListener l : listeners) {
                if (null == l) {
                    continue;
                }
                //已经空了，直接返回吧
                if (null == mPlayerListeners) {
                    break;
                }

                // 检查是否还存在
                if (-1 == mPlayerListeners.indexOf(l)) {
                    continue;
                }

                l.onPrepareToPlay(media);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void notifyOnCurrentTimePlayerListener(IFSMedia media, int currentTime) {
        if (Consts.logEnabled()) {
//            Log.d(TAG, "notifyOnCurrentTimePlayerListener " + media + " currentTime=" + currentTime);
        }
        try {
            List<PlayListener> listeners = new ArrayList(mPlayerListeners);
            for (PlayListener l : listeners) {
                if (null == l) {
                    continue;
                }
                //已经空了，直接返回吧
                if (null == mPlayerListeners) {
                    break;
                }

                // 检查是否还存在
                if (-1 == mPlayerListeners.indexOf(l)) {
                    continue;
                }
                l.onCurrentTimePlay(media, currentTime);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean notifyAferPlayerListener(IFSMedia media) {
        boolean r = true;

        // 修复外部调用时，产生的crash
        try {
            List<PlayListener> listeners = new ArrayList<>(mPlayerListeners);
            for (PlayListener l : listeners) {
                if (null == l) {
                    continue;
                }

                //已经空了，直接返回吧
                if (null == mPlayerListeners) {
                    break;
                }

                // 检查是否还存在
                if (-1 == mPlayerListeners.indexOf(l)) {
                    continue;
                }

                if (Consts.logEnabled()) {
                    Log.d(TAG, "in notifyAferPlayerListener onAfterPlay = " + l);
                }
                if (!l.onAfterPlay(media)) {
                    r = false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return r;
    }

    private void registerVideoView() {
        try {
            if (null == mPlayerViewProvider) {
                if (Consts.logEnabled()) {
                    Log.d(TAG, "PlayerViewProvider must be set");
                }
                return;
            }
            if (null == mPlayerViewProvider.videoView()) {
                return;
            }
            if (null != mPlayerRecognizer.getPlayerView(IFSMedia.MediaType.VIDEO)) {
                return;
            }

            if (null != mPlayerViewProvider.getParentView()) {
                mPlayerViewProvider.getParentView().setBackgroundResource(R.color.transparent);
            }

            final IFSMediaPlayerView videoPlayerView = new FSVideoPlayerView(mPlayerViewProvider.videoView());
            videoPlayerView.setHttpProxyCacheServer(mCacheServer);
            videoPlayerView.setVideoViewParent(mPlayerViewProvider.getParentView());
            mPlayerRecognizer.registerPlayerView(IFSMedia.MediaType.VIDEO, videoPlayerView);

            int width = 1920;//VideoUtil.getWidth(mPlayerViewProvider.videoView().view().getContext());
            int height = 1080;//VideoUtil.getHeight(mPlayerViewProvider.videoView().view().getContext());
            videoPlayerView.setDimension(width, height);

            videoPlayerView.setOnPreparedListener(new IFSMediaPlayerView.OnPreparedListener() {
                @Override
                public void onPrepared(IFSMediaPlayerView mp) {
                    notifyOnPreparePlayerListener(mp.getMedia());
                }
            });

            videoPlayerView.setOnCurrentTimeListener(new IFSMediaPlayerView.OnCurrentTimeUpdateListener() {
                @Override
                public void onCurrentTimeUpdate(IFSMediaPlayerView mp, int currentTime) {
                    notifyOnCurrentTimePlayerListener(mp.getMedia(), currentTime);
                }
            });

            videoPlayerView.setOnCompletionListener(new IFSMediaPlayerView.OnCompletionListener() {
                @Override
                public void onCompletion(final IFSMediaPlayerView mp, boolean isEnd) {
                    mIsPlaying = false;
                    notifyAferPlayerListener(videoPlayerView.getMedia());
                }
            });

            videoPlayerView.setOnErrorListener(new IFSMediaPlayerView.OnErrorListener() {
                @Override
                public boolean onError(final IFSMediaPlayerView mp, int what, int extra) {
                    mIsPlaying = false;
                    notifyAferPlayerListener(videoPlayerView.getMedia());
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 是否后台模式
    public void setBackgoundMode(boolean backgoundMode) {
        mBackgroundMode = backgoundMode;
    }

    private void registerImageView() {
        if (null == mPlayerViewProvider) {
            if (Consts.logEnabled()) {
                Log.d(TAG, "PlayerViewProvider must be set");
            }
            return;
        }
        mPlayerViewProvider.imageView().setVisibility(View.GONE);

        final IFSMediaPlayerView imagePlayerView = new FSImagePlayerView(mPlayerViewProvider.imageView());
        mPlayerRecognizer.registerPlayerView(IFSMedia.MediaType.IMAGE, imagePlayerView);
        imagePlayerView.setOnPreparedListener(new IFSMediaPlayerView.OnPreparedListener() {
            @Override
            public void onPrepared(IFSMediaPlayerView mp) {
                notifyOnPreparePlayerListener(mp.getMedia());

            }
        });
        imagePlayerView.setOnCurrentTimeListener(new IFSMediaPlayerView.OnCurrentTimeUpdateListener() {
            @Override
            public void onCurrentTimeUpdate(IFSMediaPlayerView mp, int currentTime) {
                notifyOnCurrentTimePlayerListener(mp.getMedia(), currentTime);
            }
        });
        imagePlayerView.setOnCompletionListener(new IFSMediaPlayerView.OnCompletionListener() {
            @Override
            public void onCompletion(final IFSMediaPlayerView mp, boolean isEnd) {
                mIsPlaying = false;
                notifyAferPlayerListener(mp.getMedia());
            }
        });

        imagePlayerView.setOnErrorListener(new IFSMediaPlayerView.OnErrorListener() {
            @Override
            public boolean onError(final IFSMediaPlayerView mp, int what, int extra) {
                mIsPlaying = false;
                notifyAferPlayerListener(mp.getMedia());
                return true;
            }
        });
    }

    private void registerPlayerView() {
        //  视频类型,待增加图片类型
        // 注册播放器插件
        //videoview
        registerVideoView();
        registerImageView();
    }

    private void ungisterPlayerView() {
        if (null == mPlayerRecognizer) {
            return;
        }

        mPlayerRecognizer.clear();
    }

    private void releasePlayerView() {
        ungisterPlayerView();
    }

    public interface PlayerViewProvider {
        SimpleDraweeView imageView();

        VideoViewInf videoView();

        /**
         * 提供加载添加占位view
         *
         * @return
         */
        ViewGroup getParentView();
    }

    public interface PlayListener {
        void onPrepareToStart(IFSMedia media);

        void onPrepareToPlay(IFSMedia media);

        void onCurrentTimePlay(IFSMedia media, int currentTime);

        boolean onAfterPlay(IFSMedia media);
    }

    public static abstract class SimplePlayListener implements PlayListener {
        public void onPrepareToStart(IFSMedia media) {
        }

        public void onPrepareToPlay(IFSMedia media) {
        }

        public void onCurrentTimePlay(IFSMedia media, int currentTime) {
        }

        public abstract boolean onAfterPlay(IFSMedia media);
    }
}
