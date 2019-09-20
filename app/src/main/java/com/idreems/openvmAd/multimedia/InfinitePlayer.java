package com.idreems.openvmAd.multimedia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMediaPlayer;
import com.idreems.openvmAd.multimedia.callback.DataHttpCallback;
import com.idreems.openvmAd.utils.ReusableTimer;

import java.util.TimerTask;

/**
 * Created by ramonqlee_macpro on 2019/4/28.
 */

//广告请求，循环播放控制器
public class InfinitePlayer {
    private static final String TAG = InfinitePlayer.class.getSimpleName();
    private Handler mUIHandler = new Handler(Looper.getMainLooper());

    private boolean mStop;

    public boolean isStop() {
        return mStop;
    }

    public void setState(boolean stopped) {
        mStop = stopped;
    }

    public void startLoop(final Context context, final String adPosition, final FSMediaPlayer player) {
        if (null == context || null == player) {
            return;
        }

        if (isStop()) {
            if (Consts.logEnabled()) {
                Log.d(TAG, TAG + " startLoop stopped before ad request");
            }
            return;
        }

        if (Consts.logEnabled()) {
            Log.d(TAG, TAG + " startLoop adPosition =" + adPosition + " player=" + player);
        }
        //  每次请求一个广告，然后播放，播放完毕，启动广告上传，然后再请求下一个广告，播放，依次类推
        NewAdRequest.getInstance().requestAd(context, adPosition, new DataHttpCallback() {
            @Override
            public void onResponse(final IFSMedia media) {
                if (Consts.logEnabled()) {
                    Log.d(TAG, TAG + " startLoop onResponse " + media);
                }
                // 如果没有广告，没停止的话，延时2秒继续请求，否则的话，开始播放，然后播放完毕后，启动下一次广告请求
                if (null == media) {
                    if (isStop()) {
                        if (Consts.logEnabled()) {
                            Log.d(TAG, TAG + " startLoop stopped with empty ad");
                        }
                        return;
                    }

                    if (Consts.logEnabled()) {
                        Log.d(TAG, TAG + " empty media,start after 2 sec");
                    }
                    //为了防止给服务器造成过大的压力，延时2秒
                    getReusableTimer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startLoop(context, adPosition, player);
                        }
                    }, 2 * 1000);
                    return;
                }

                if (isStop()) {
                    if (Consts.logEnabled()) {
                        Log.d(TAG, TAG + " startLoop stopped after getting ad");
                    }
                    return;
                }

                //启动播放，然后播放完毕后，请求下一次广告
                player.addPlayListener(new FSMediaPlayer.SimplePlayListener() {
                    @Override
                    public boolean onAfterPlay(IFSMedia media) {
                        if (Consts.logEnabled()) {
                            Log.d(TAG, TAG + " onAfterPlay,start new ad");
                        }
                        player.removePlayerLister(this);//删除自有的listener

                        startLoop(context, adPosition, player);
                        return true;
                    }
                });
                if (Consts.logEnabled()) {
                    Log.d(TAG, TAG + " post to main ui thread ad and start");
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Consts.logEnabled()) {
                            Log.d(TAG, TAG + " start playing now with player =" + player);
                        }
                        player.start(media);
                    }
                });
            }
        });
    }

    private ReusableTimer getReusableTimer() {
        return new ReusableTimer();
    }
}
