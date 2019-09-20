package com.idreems.openvmAd.multimedia.media.ui.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.VideoView;

import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewBufferUpdateListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewCompletionListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewErrorListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewPreparedListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewSeekCompletionListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewTimeUpdateListener;
import com.idreems.openvmAd.utils.LogUtil;
import com.idreems.openvmAd.utils.ReusableTimer;

import java.util.TimerTask;

/**
 * Created by ramonqlee on 18/03/2017.
 */
//  进行封装
public class AndroidVideoViewWrapper implements VideoViewInf {
    private static final String TAG = "AndroidVideoViewWrapper";

    private static final long ONE_SECOND_IN_MS = 1000;
    OnVideoViewBufferUpdateListener mOnBufferingUpdateListener;
    MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (null == mOnBufferingUpdateListener) {
                return;
            }
            mOnBufferingUpdateListener.onBufferingUpdate(percent);
        }
    };
    private Context mContext;
    private VideoView mVideoView;
    private MediaPlayer mMediaPlayer;
    private ReusableTimer mTimeUpdateTimer;
    private int mPreviousPosition;
    private boolean mErrored;
    private boolean mPaused;
    private float mVolume = 1.0f;
    private OnVideoViewTimeUpdateListener mOnVideoViewTimeUpdateListener;


    public AndroidVideoViewWrapper(Context context) {
        mContext = context.getApplicationContext();
        initVideoView();
    }

    private void initVideoView() {
        if (null == mContext) {
            return;
        }
        mVideoView = new VideoView(mContext);
        mVideoView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public String getEngineName() {
        return TAG;
    }

    /**
     * Gets volume as integer
     */
    public float getVolume() {
        if (null == mVideoView) {
            return 0;
        }
        return mVolume;
    }

    /**
     * Sets volume as integer
     *
     * @param volume: Volume level passed as integer
     */
    public float setVolume(float volume) {
        final float r = mVolume;
        try {
            if (null == mMediaPlayer) {
                return 0;
            }
            mMediaPlayer.setVolume(volume, volume);
            mVolume = volume;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return r;
    }

    public String getVersion() {
        if (null == mVideoView) {
            return "UKN";
        }
        return "vv1.0";
    }

    public void setZOrderOnTop(boolean onTop) {
        if (null == mVideoView) {
            return;
        }
        mVideoView.setZOrderOnTop(onTop);
        mVideoView.setZOrderMediaOverlay(onTop);
    }

    public View view() {
        return mVideoView;
    }

    /**
     * Sets the path to the video.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param path The path to the video
     */
    public void setVideoPath(String path) {
        if (null == mVideoView) {
            return;
        }
        try {
            mVideoView.setVideoPath(path);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setVideoURI(@Nullable Uri uri) {
        if (null == mVideoView) {
            return;
        }
        try {
            mVideoView.setVideoURI(uri);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void prepareAsync() {
    }

    public void start() {
        if (null == mVideoView) {
            return;
        }
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "start android videoview player");
        }
        mVideoView.start();
        mPaused = false;
        startTimeUpdateTimer();
    }

    public void resume() {
        if (null == mVideoView) {
            return;
        }
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "resume android videoview player");
        }
        if (!mPaused) {
            mVideoView.resume();
        }
        mVideoView.start();
        mPaused = false;
        startTimeUpdateTimer();
    }

    public void pause() {
        if (null == mVideoView) {
            return;
        }
        stopTimeUpdateTimer();
        mVideoView.pause();
        mPaused = true;
    }

    private void resetListeners() {
        mOnVideoViewTimeUpdateListener = null;
        mOnBufferingUpdateListener = null;
        if (null == mVideoView) {
            return;
        }
        mVideoView.setOnPreparedListener(null);
        mVideoView.setOnErrorListener(null);
    }

    public void release() {
        if (null == mVideoView) {
            return;
        }
        try {
            stopPlayback();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        if (null == mVideoView) {
            return;
        }
        try {
            stopPlayback();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * If a video is currently in playback then the playback will be stopped
     */
    public void stopPlayback() {
        if (null == mVideoView) {
            return;
        }
        resetListeners();
        stopTimeUpdateTimer();
        mVideoView.stopPlayback();
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "release android videoview player by stopPlayback");
        }
    }

    public void stop(final Runnable runnable) {
        stop();
        if (null != runnable) {
            runnable.run();
        }
    }


    public void release(final Runnable runnable) {
        release();
        if (null != runnable) {
            runnable.run();
        }
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "release android videoview player,isPlaying =" + isPlaying());
        }
    }

    /**
     * Moves the current video progress to the specified location.
     *
     * @param milliSeconds The time to move the playback to
     */
    public void seekTo(int milliSeconds) {
        if (null == mVideoView) {
            return;
        }
        mVideoView.seekTo(milliSeconds);
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "android videoview seekTo =" + milliSeconds);
        }
    }


    /**
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(final OnVideoViewPreparedListener listener) {
        if (null == mVideoView || null == listener) {
            return;
        }

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mMediaPlayer == null) {
                    mMediaPlayer = mp;
//                    mMediaPlayer.setOnInfoListener(mOnInfoListener);
                    mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
                }

                if (null == listener) {
                    return;
                }
                listener.onPrepared();
            }
        });
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(final OnVideoViewCompletionListener listener) {
        if (null == mVideoView || null == listener) {
            return;
        }
        if (Consts.logEnabled()) {
            LogUtil.d(Consts.PLAYER_TAG, "setOnCompletionListener in AndroidVideoViewWrapper");
        }

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                stopTimeUpdateTimer();
                if (null == listener) {
                    return;
                }
                listener.onCompletion();
                mVideoView.setOnCompletionListener(null);
            }
        });
    }

    /**
     * Sets the listener to inform of VideoPlayer buffer update events
     *
     * @param listener The listener
     */
    public void setOnBufferingUpdateListener(final OnVideoViewBufferUpdateListener listener) {
        if (null == mVideoView || null == listener) {
            return;
        }

        mOnBufferingUpdateListener = listener;
    }

    private void startTimeUpdateTimer() {
        stopTimeUpdateTimer();

        if (null == mOnVideoViewTimeUpdateListener) {
            return;
        }

        mTimeUpdateTimer = new ReusableTimer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {

                    if (!mVideoView.isPlaying()) {
                        return;
                    }

                    final int pos = getCurrentPosition();

                    if (mPreviousPosition == pos) {
                        return;
                    }

//                    LogUtil.logD(Consts.PLAYER_TAG, "onCurrentTimeUpdate previousPosition=" + mPreviousPosition + " currentPosition=" + getCurrentPosition());
                    mPreviousPosition = pos;
                    mVideoView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null == mOnVideoViewTimeUpdateListener) {
                                return;
                            }
                            mOnVideoViewTimeUpdateListener.onCurrentTimeUpdate(mPreviousPosition);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        mTimeUpdateTimer.schedule(task, 0, ONE_SECOND_IN_MS);
    }

    private void stopTimeUpdateTimer() {
        if (null != mTimeUpdateTimer) {
            mTimeUpdateTimer.cancel();
            mTimeUpdateTimer = null;
        }
    }

    public OnVideoViewTimeUpdateListener getOnCurrentTimeUpdateListener() {
        return mOnVideoViewTimeUpdateListener;
    }

    public void setOnCurrentTimeUpdateListener(final OnVideoViewTimeUpdateListener listener) {
        mOnVideoViewTimeUpdateListener = listener;
        if (null == mVideoView || null == listener) {
            return;
        }

        //FIXME 转移到start中
    }

    /**
     * Sets the listener to inform of VideoPlayer seek completion events
     *
     * @param listener The listener
     */
    public void setOnSeekCompletionListener(final OnVideoViewSeekCompletionListener listener) {
        if (null == mVideoView || null == listener) {
            return;
        }
    }

    /**
     * Sets the listener to inform of playback errors
     *
     * @param listener The listener
     */
    public void setOnErrorListener(final OnVideoViewErrorListener listener) {
        if (null == mVideoView || null == listener) {
            return;
        }
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (Consts.logEnabled()) {
                    LogUtil.d(Consts.PLAYER_TAG, "PlayerError what = " + what + " extra=" + extra);
                }
                listener.onError();
                mErrored = true;
                return true;
            }
        });
    }


    /**
     * Retrieves the current position of the audio playback.  If an audio item is not currently
     * in playback then the value will be 0.  This should only be called after the item is
     * prepared
     *
     * @return The millisecond value for the current position
     */
    public int getCurrentPosition() {
        if (null == mVideoView) {
            return 0;
        }
        return mVideoView.getCurrentPosition();
    }

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared
     *
     * @return The millisecond duration of the video
     */
    public int getDuration() {
        if (null == mVideoView) {
            return 0;
        }
        return mVideoView.getDuration();
    }

    /**
     * Returns if a video is currently in playback
     *
     * @return True if a video is playing
     */
    public boolean isPlaying() {
        if (null == mVideoView) {
            return false;
        }
        return mVideoView.isPlaying();
    }

    public boolean isNothingSpecial() {
        if (null == mVideoView) {
            return false;
        }
        // TODO 待实现
        return true;
    }

    public boolean isOpening() {
        if (null == mVideoView) {
            return false;
        }
        // TODO 待实现
        return true;
    }

    public boolean isBuffering() {
        if (null == mVideoView) {
            return false;
        }
        return mVideoView.getBufferPercentage() > 0;
    }

    public boolean isPaused() {
        if (null == mVideoView) {
            return false;
        }
        return mPaused;
    }

    public boolean isStopped() {
        if (null == mVideoView) {
            return false;
        }
        return !mVideoView.isPlaying();
    }

    public boolean isEnded() {
        if (null == mVideoView) {
            return false;
        }
        return !mVideoView.isPlaying();
    }

    public boolean isError() {
        if (null == mVideoView) {
            return false;
        }
        return mErrored;
    }

    public boolean canSeekForward() {
        if (null == mVideoView) {
            return false;
        }
        return mVideoView.canSeekForward();
    }

    public void setHWDecode(boolean hwDecode) {

    }

    public void setSurfaceView(View view) {
    }
}
