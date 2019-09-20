package com.idreems.openvmAd.multimedia.media.ui.widget;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.View;

import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewBufferUpdateListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewCompletionListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewErrorListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewPreparedListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewSeekCompletionListener;
import com.idreems.openvmAd.multimedia.media.listener.OnVideoViewTimeUpdateListener;


/**
 * Created by ramonqlee on 18/03/2017.
 */
// TODO 视频播放接口,后续将直接使用这个接口进行播放视频
public interface VideoViewInf {
    String getEngineName();

    void setZOrderOnTop(boolean onTop);

    View view();

    String getVersion();

    /**
     * Gets volume as integer
     */
    float getVolume();

    /**
     * Sets volume as integer
     *
     * @param volume: Volume level passed as integer
     */
    float setVolume(float volume);

    /**
     * Sets the path to the video.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param path The path to the video
     */
    void setVideoPath(String path);

    void setVideoURI(@Nullable Uri uri);

    void prepareAsync();

    void start();

    void resume();

    void pause();

    void stop();

    void stop(final Runnable runnable);

    void release();

    void release(final Runnable runnable);

    /**
     * Moves the current video progress to the specified location.
     *
     * @param milliSeconds The time to move the playback to
     */
    void seekTo(int milliSeconds);

    /**
     * If a video is currently in playback then the playback will be stopped
     */
    void stopPlayback();

    /**
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener
     */
    void setOnPreparedListener(final OnVideoViewPreparedListener listener);


    OnVideoViewTimeUpdateListener getOnCurrentTimeUpdateListener();

    void setOnCurrentTimeUpdateListener(final OnVideoViewTimeUpdateListener onCurrentTimeUpdateListener);

    void setOnBufferingUpdateListener(final OnVideoViewBufferUpdateListener listener);

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener
     */
    void setOnCompletionListener(final OnVideoViewCompletionListener listener);

    /**
     * Sets the listener to inform of VideoPlayer seek completion events
     *
     * @param listener The listener
     */
    void setOnSeekCompletionListener(final OnVideoViewSeekCompletionListener listener);


    /**
     * Sets the listener to inform of playback errors
     *
     * @param listener The listener
     */
    void setOnErrorListener(final OnVideoViewErrorListener listener);


    /**
     * Retrieves the current position of the audio playback.  If an audio item is not currently
     * in playback then the value will be 0.  This should only be called after the item is
     * prepared
     *
     * @return The millisecond value for the current position
     */
    int getCurrentPosition();

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared
     *
     * @return The millisecond duration of the video
     */
    int getDuration();

    /**
     * Returns if a video is currently in playback
     *
     * @return True if a video is playing
     */
    boolean isPlaying();

    boolean isNothingSpecial();

    boolean isOpening();

    boolean isBuffering();

    boolean isPaused();

    boolean isStopped();

    boolean isEnded();

    boolean isError();

    boolean canSeekForward();

    void setHWDecode(boolean hwDecode);

    void setSurfaceView(View view);
}
