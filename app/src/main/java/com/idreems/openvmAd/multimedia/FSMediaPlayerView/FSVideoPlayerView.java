package com.idreems.openvmAd.multimedia.FSMediaPlayerView;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.danikula.videocache.HttpProxyCacheServer;
import com.idreems.openvmAd.MainActivity;
import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.R;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.NewAdRequest;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewCompletionListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewErrorListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewPreparedListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewTimeUpdateListener;
import com.idreems.openvmAd.multimedia.media.ui.widget.VideoViewInf;
import com.idreems.openvmAd.utils.MyActivityManager;
import com.idreems.openvmAd.utils.ReusableTimer;
import com.idreems.openvmAd.utils.Utils;

import java.io.File;
import java.util.TimerTask;

import pl.droidsonroids.gif.GifImageView;


/**
 * Created by ramonqlee on 5/19/16.
 */
public class FSVideoPlayerView implements IFSMediaPlayerView {
    private static final String TAG = FSVideoPlayerView.class.getSimpleName();
    public static final String PLAY_URL_METHOD = "return";
    public static final String PLAY_URL_METHOD_VALUE = "1";// 是否返回url，自己进行解析
    private static final long PEPARED_TIME_OUT_DELAY_IN_MILLS = 15 * 1000;
    private static final long TIME_OUT_DELAY_IN_MILLS = 10 * 1000;//首次检查的时间
    private static final int COUNT_DOWN_ALLOWED_GAP = 5 * 1000;//10秒

    // all possible internal states;BORROWED FROM IJKVIDEOVIEW
    private static final int STATE_IDLE = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_RELEASED = 5;
    private int mCurrentState = STATE_IDLE;

    private VideoViewInf mVLCVideoView;
    private ViewGroup mVideoViewParent;
    private GifImageView mLoadingImageView;
    private IFSMedia mMedia;
    private OnPreparedListener mPrepareListener;
    private OnCurrentTimeUpdateListener mOnCurrentTimeUpdateListener;
    private OnCompletionListener mCompleteListener;
    private OnErrorListener mErrorListener;

    private int mVideoWidth;
    private int mVideoHeight;

    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    private ReusableTimer mCountDownTimer;

    private ReusableTimer mTimeoutTimer;
    private ReusableTimer mPrepareTimeoutTimer;//准备的超时

    private TimerTask mCountDownTimerTask;
    private TimerTask mTimeoutTimerTask;
    private TimerTask mPrepareTimeoutTimerTask;//准备的超时
    private boolean mShowProgress = true;
    private HttpProxyCacheServer mCacheServer;
    private boolean mBackgoundMode;

    public FSVideoPlayerView(VideoViewInf VLCVideoView) {
        mVLCVideoView = VLCVideoView;
    }

    public View getPlayerView() {
        return mVLCVideoView.view();
    }

    @Override
    public void playAnimation(boolean enable) {

    }

    public void setVideoViewParent(ViewGroup parent) {
        mVideoViewParent = parent;
    }

    public void updateData(IFSMedia media) {
        if (null == media) {
            return;
        }
        mMedia = media;
    }

    public IFSMedia getMedia() {
        return mMedia;
    }

    public void setDimension(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
    }

    public void setHttpProxyCacheServer(HttpProxyCacheServer cacheServer) {
        mCacheServer = cacheServer;
    }

    public void setOnPreparedListener(final OnPreparedListener listener) {
        mPrepareListener = listener;
    }

    public void setOnCurrentTimeListener(OnCurrentTimeUpdateListener listener) {
        mOnCurrentTimeUpdateListener = listener;
    }

    public void setOnCompletionListener(final OnCompletionListener listener) {
        mCompleteListener = listener;
    }

    public void showProgress(boolean show) {
        mShowProgress = show;
    }

    public void setOnErrorListener(final OnErrorListener listener) {
        mErrorListener = listener;
    }

    public void start() {
        try {
            // 如果是后台任务，模拟播放
            if (mBackgoundMode) {
                long countdown = 0;
                if (null != mMedia) {
                    countdown = Math.abs(mMedia.getEndTimeInSec() - mMedia.getStartTimeInSec()) * 1000;
                    //如果是自有广告，则立即返回
                    if (mMedia.isSelfMedia()) {
                        countdown = Consts.ONE_SECOND_IN_MS;
                    }
                }
                if (countdown <= 0) {
                    countdown = 15 * Consts.ONE_SECOND_IN_MS;
                }

                mCurrentState = STATE_PLAYING;//设置为播放状态
                addLoadingViews();
                startCountDownMonitor(countdown);
                return;
            }

            if (null != MyActivityManager.getInstance().getCurrentActivity() && MyActivityManager.getInstance().getCurrentActivity().getClass().getSimpleName().equals(MainActivity.class.getSimpleName())) {
                mCurrentState = STATE_IDLE;
            }
            if (mCurrentState == STATE_RELEASED) {
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "player stopped,mCurrentState = " + mCurrentState);
                }
                return;
            }

            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "start with player engine =" + mVLCVideoView.getEngineName() + " mCurrentState=" + mCurrentState);
            }

            // 待增加按段播放功能
            // FIXME ksy stop有bug，暂时修改为自己判断的方式，而不是调用原始的engine的状态
            if (null == mVLCVideoView || null == mMedia || STATE_IDLE != mCurrentState) {
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "playing video,ignore current request = " + mVLCVideoView);
                }
                return;
            }

            mVLCVideoView.view().setVisibility(View.VISIBLE);
            mVLCVideoView.setZOrderOnTop(true);
            mVLCVideoView.view().bringToFront();

            if (!TextUtils.equals(mMedia.getAd_position(), Consts.POSITIONADPRE)) {
                if (Consts.logEnabled()) {
                    Log.d(Consts.AD_TAG, "start FSImagePlayerView preloadThirdAd adPosition=" + mMedia.getAd_position());
                }
                NewAdRequest.getInstance().preloadThirdAd(mMedia.getAd_position());
            }
            // 增加mediaId支持，优先尝试url，然后尝试mediaID
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, TAG+" start play mMedia.getUrl() = " + mMedia.getUrl()  + " time = " + mMedia.getEndTimeInSec());
            }
            String url = mMedia.getUrl();

            if (!TextUtils.isEmpty(url) && Utils.isPlayableUrl(url)) {
                if (Utils.isLocalFile(url)) {
                    removeLoadingViews();
                    mVLCVideoView.setVideoURI(Uri.parse(url));
                } else {
                    final String videoUrl = Utils.urlEncodeURL(url);
                    // 历史原因，兼容之前已经缓存的文件
                    String cacheName = Utils.getVideoCacheFileName(mMedia);
                    String cachedFileName = String.format("%s%s%s", Utils.getVideoCacheDir(MyApplication.getContext()), File.separator, cacheName);
                    if (Utils.isFileExist(cachedFileName)) {
                        removeLoadingViews();
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "cached1 file with name = " + cachedFileName);
                        }
                        mVLCVideoView.setVideoPath(cachedFileName);
                    } else if (null != mCacheServer && !url.contains("file://")) { //有httpcacheserver
                        //不存在于缓存中，显示加载占位图
                        if (!mCacheServer.isCached(videoUrl)) {
                            addLoadingViews();
                        }

                        final String httpCacheServerVideoUrl = mCacheServer.getProxyUrl(videoUrl);
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "http cache server now play = " + httpCacheServerVideoUrl + " isCached =" + mCacheServer.isCached(videoUrl));
                        }
                        try {
                            mVLCVideoView.setVideoURI(Uri.parse(httpCacheServerVideoUrl));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    } else {
                        try {
                            //不存在于缓存中，显示加载占位图
                            if (Consts.logEnabled()) {
                                Log.d(Consts.PLAYER_TAG, "http now play = " + videoUrl);
                            }
                            addLoadingViews();
                            mVLCVideoView.setVideoURI(Uri.parse(videoUrl));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    }
                }

                initListeners();
                //FIXME VLC目前有个bug，需要在这里调用start方法，其他的播放器转移到OnPrepare回调中调用

                mVLCVideoView.prepareAsync();
                startPlayerPrepareTimeoutMonitor();

                mCurrentState = STATE_PLAYING;
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "start1 playing video with " + mMedia.getName() + " and url = " + url);
                }
                return;
            }

            startPlayerPrepareTimeoutMonitor();
            mCurrentState = STATE_IDLE;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 释放的是引擎，而不是资源
    public void stop() {
        stop(null);
    }

    public void stop(final Runnable r) {
        try {
            if (STATE_RELEASED == mCurrentState || STATE_IDLE == mCurrentState) {
                return;
            }

            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, TAG + " stop");
            }

            removeLoadingViews();
            stopCountDownMonitor();
            stopPlayPrepareTimeoutMonitor();
            stopPlayAfterPrepareTimeoutMonitor();
//            mVLCVideoView.view().setVisibility(View.GONE);
            mCompleteListener.onCompletion(FSVideoPlayerView.this, false);
            mCurrentState = STATE_RELEASED;
            if (mVLCVideoView != null) {
                mVLCVideoView.stopPlayback();
            }
            if (null != r) {
                r.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void release() {
        try {
            if (STATE_RELEASED == mCurrentState) {
                return;
            }
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "release");
            }
            removeLoadingViews();
            stopCountDownMonitor();
            stopPlayPrepareTimeoutMonitor();
            stopPlayAfterPrepareTimeoutMonitor();
//            mVLCVideoView.view().setVisibility(View.GONE);

            mCurrentState = STATE_RELEASED;
            if (null != mMedia) {
                mVLCVideoView.release();
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "release media immediately " + mMedia.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setGone() {

    }


    /**
     * 准备超时
     */
    private void startPlayerPrepareTimeoutMonitor() {
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "startPlayerPrepareTimeoutMonitor");
        }
        //超时的处理，防止一个视频半天没缓冲出来，卡死的现象
        mPrepareTimeoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                //FIXME 直播的待修复
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "startPlayerPrepareTimeoutMonitor timeout");
                }
                if (null != mMedia && Utils.isLiveShow(mMedia.getUrl())) {
                    return;
                }

                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stop();
                            if (Consts.logEnabled()) {
                                Log.d(Consts.PLAYER_TAG, "startPlayerPrepareTimeoutMonitor onComplete");
                            }
                            if (null == mCompleteListener) {
                                return;
                            }
                            mCompleteListener.onCompletion(FSVideoPlayerView.this, false);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        };
        stopPlayPrepareTimeoutMonitor();
        mPrepareTimeoutTimer = new ReusableTimer();
        mPrepareTimeoutTimer.schedule(mPrepareTimeoutTimerTask, PEPARED_TIME_OUT_DELAY_IN_MILLS);
    }

    private void stopPlayPrepareTimeoutMonitor() {
        if (null == mPrepareTimeoutTimer) {
            return;
        }
//        mPrepareTimeoutTimerTask.cancel();
        mPrepareTimeoutTimer.cancel();
        mPrepareTimeoutTimer = null;
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "stopPlayPrepareTimeoutMonitor");
        }
    }

    /**
     * 准备完毕后，播放超时
     */
    private void startPlayerAfterPrepareTimeoutMonitor() {
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "startPlayerAfterPrepareTimeoutMonitor");
        }
        stopPlayAfterPrepareTimeoutMonitor();
        //超时的处理，防止一个视频半天没缓冲出来，卡死的现象
        mTimeoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                //FIXME 直播的待修复
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "startPlayerAfterPrepareTimeoutMonitor timeout");
                }
                if (null != mMedia && Utils.isLiveShow(mMedia.getUrl())) {
                    return;
                }

                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stop();
                            if (Consts.logEnabled()) {
                                Log.d(Consts.PLAYER_TAG, "startPlayerAfterPrepareTimeoutMonitor onComplete");
                            }
                            if (null == mCompleteListener) {
                                return;
                            }
                            mCompleteListener.onCompletion(FSVideoPlayerView.this, false);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        };
        mTimeoutTimer = new ReusableTimer();
        mTimeoutTimer.schedule(mTimeoutTimerTask, TIME_OUT_DELAY_IN_MILLS);
    }

    private void stopPlayAfterPrepareTimeoutMonitor() {
        if (null == mTimeoutTimer) {
            return;
        }
        mTimeoutTimer.cancel();
        mTimeoutTimer = null;
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "stopPlayAfterPrepareTimeoutMonitor");
        }
    }

    private void startCountDownMonitor(final long countdown) {
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "startCountDownMonitor = "+mMedia);
        }
        stopCountDownMonitor();
        final Runnable completeRunner = new Runnable() {
            @Override
            public void run() {
                try {
                    if (null == mCompleteListener) {
                        return;
                    }
                    mCompleteListener.onCompletion(FSVideoPlayerView.this, false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        mCountDownTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (!mBackgoundMode) {//非后台模式，检查状态；后台模式，直接回调
                    // FIXME 快播放完毕了,直接等着播放完毕吧
                    if (mVLCVideoView.isPlaying() && Math.abs(mVLCVideoView.getDuration() - mVLCVideoView.getCurrentPosition()) < COUNT_DOWN_ALLOWED_GAP) {
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "CountDownTask finished, but continue finishing video " + mMedia.getName());
                        }
                        return;
                    }

                    if (!mVLCVideoView.isPlaying()) {
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "Both countDownTask and video finished!");
                        }
                        return;
                    }
                }

                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "CountDownTask finished,now stop playing video " + mMedia.getName());
                }
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        mUIHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCurrentState = STATE_IDLE;//等同于正常结束，恢复正常播放状态
                                if (null != completeRunner) {
                                    completeRunner.run();
                                }
                            }
                        });
                    }
                };

                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stop(r);
                    }
                });
            }
        };
        mCountDownTimer = new ReusableTimer();
        mCountDownTimer.schedule(mCountDownTimerTask, countdown + COUNT_DOWN_ALLOWED_GAP);
        // 开始计时
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "start countdown for video,countdown = " + countdown);
        }
    }

    private boolean countDownTaskStarted() {
        return null != mCountDownTimer;
    }

    private void stopCountDownMonitor() {
        if (null == mCountDownTimer) {
            return;
        }
//        mCountDownTimerTask.cancel();
        mCountDownTimer.cancel();
        mCountDownTimer = null;
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "stopCountDownMonitor");
        }
    }

    private void initListeners() {
        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "initListeners");
        }

        final OnVideoViewPreparedListener preparedListener = new OnVideoViewPreparedListener() {
            boolean mPrepared;

            @Override
            public void onPrepared() {
                try {
                    if (Consts.logEnabled()) {
                        Log.d(Consts.PLAYER_TAG, "onPrepared called " + mMedia);
                    }
                    stopPlayPrepareTimeoutMonitor();
                    if (mCurrentState == STATE_RELEASED) {
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "player stopped");
                        }
                        stop();
                        return;
                    }

                    if (null != getMedia()) {
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "before onPrepared in FSVideoPlayer " + mMedia.getName());
                        }
                    }

                    if (mPrepared) {
                        return;
                    }

                    mPrepared = true;
                    // 非vlc的在此启动播放
                    mVLCVideoView.start();

                    if (null != getMedia()) {
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "onPrepared in FSVideoPlayer " + mMedia.getName());
                        }
                    }

                    int seekTo = (int) mMedia.getStartTimeInSec() * 1000;
                    if (seekTo > 0 && !Utils.isLiveShow(mMedia.getUrl()) && mVLCVideoView.getCurrentPosition() != seekTo) {
                        mVLCVideoView.seekTo(seekTo);
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "onPrepared seekTo =" + seekTo);
                        }
                    }

//                 如果超时了，则释放资源，进入下一个
                    startPlayerAfterPrepareTimeoutMonitor();
                    if (null != mPrepareListener) {
                        mPrepareListener.onPrepared(FSVideoPlayerView.this);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        mVLCVideoView.setOnPreparedListener(preparedListener);

        final OnVideoViewTimeUpdateListener updateListener = new OnVideoViewTimeUpdateListener() {
            private boolean mNormalTimeUpdateLogged;

            public void onCurrentTimeUpdate(int currentTime) {
                try {
                    stopPlayPrepareTimeoutMonitor();
                    if (null != mOnCurrentTimeUpdateListener) {
                        mOnCurrentTimeUpdateListener.onCurrentTimeUpdate(FSVideoPlayerView.this, currentTime);
                    }
                    if (STATE_RELEASED == mCurrentState) {
                        // 视频已经处于停止状态
                        if (Consts.logEnabled()) {
                            Log.d(Consts.PLAYER_TAG, "release video");
                        }
                        stop();
                        return;
                    }

                    removeLoadingViews();
                    mVLCVideoView.view().bringToFront();
                    // 如果不在播放中了,则更新状态即可,无需回调
                    if (!mVLCVideoView.isPlaying()) {
                        mCurrentState = STATE_IDLE;
                        return;
                    }

                    stopPlayAfterPrepareTimeoutMonitor();//开始播放了，取消加载超时
                    if (mNormalTimeUpdateLogged) {
                        return;
                    }
                    mNormalTimeUpdateLogged = true;
                    onStartPlay();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void onStartPlay() {
                // 设置了播放的时间
                if (IFSMedia.INFINITE_END_TIME_IN_SEC != mMedia.getEndTimeInSec()) {
                    long countdown = Math.abs(mMedia.getEndTimeInSec() - mMedia.getStartTimeInSec()) * 1000;
                    startCountDownMonitor(countdown);
                }
            }
        };
        mVLCVideoView.setOnCurrentTimeUpdateListener(updateListener);


        final OnVideoViewCompletionListener completionListener = new OnVideoViewCompletionListener() {
            boolean mCompleted = false;

            @Override
            public void onCompletion() {
                try {
                    if (Consts.logEnabled()) {
                        Log.d(Consts.PLAYER_TAG, "onCompletionListener called " + mMedia);
                    }

                    removeLoadingViews();
                    mCurrentState = STATE_IDLE;//正常结束，恢复状态
                    if (mCompleted) {
                        return;
                    }

                    stopPlayAfterPrepareTimeoutMonitor();
                    stopCountDownMonitor();
                    if (null == mCompleteListener) {
                        return;
                    }

                    mCompleteListener.onCompletion(FSVideoPlayerView.this, true);
                    mCompleted = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        mVLCVideoView.setOnCompletionListener(completionListener);

        final OnVideoViewErrorListener errorListener = new OnVideoViewErrorListener() {
            @Override
            public boolean onError() {
                try {
                    removeLoadingViews();
                    if (Consts.logEnabled()) {
                        Log.d(Consts.PLAYER_TAG, "onError");
                    }

                    stopPlayPrepareTimeoutMonitor();
                    stopPlayAfterPrepareTimeoutMonitor();
                    stopCountDownMonitor();

                    stop();

                    // 出错了，恢复状态，这样下次还可以继续播放
                    mCurrentState = STATE_IDLE;
                    if (null != mErrorListener) {
                        mErrorListener.onError(FSVideoPlayerView.this, 0, 0);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            }
        };
        mVLCVideoView.setOnErrorListener(errorListener);
    }

    // 是否后台模式
    public void setBackgoundMode(boolean backgoundMode) {
        mBackgoundMode = backgoundMode;
    }

    private void addLoadingViews() {
        removeLoadingViews();
        //添加视频加载过程中的背景
        if (null != mVideoViewParent) {
            if (null == mLoadingImageView) {
                mLoadingImageView = new GifImageView(mVLCVideoView.view().getContext());
            }

            if (null != mVLCVideoView && !mVLCVideoView.isPlaying()) {
                mVideoViewParent.addView(mLoadingImageView);

                mLoadingImageView.setImageResource(R.mipmap.loading);

                mLoadingImageView.setVisibility(View.VISIBLE);
                mLoadingImageView.bringToFront();

                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "gifview addLoadingViews");
                }
            }

        }
    }

    private void removeLoadingViews() {
        if (null != mLoadingImageView && null != mLoadingImageView.getParent()) {
            mLoadingImageView.setVisibility(View.GONE);
            ViewParent temp = mLoadingImageView.getParent();
            if (null != temp && temp instanceof ViewGroup) {
                mLoadingImageView.setImageDrawable(null);
                ((ViewGroup) temp).removeView(mLoadingImageView);
                mLoadingImageView = null;

                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "gifview removeLoadingViews");
                }
            }
        }
    }

}
