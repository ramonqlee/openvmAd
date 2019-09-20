package com.idreems.openvmAd.multimedia.FSMediaPlayerView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.R;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.NewAdRequest;
import com.idreems.openvmAd.utils.DeviceUtils;
import com.idreems.openvmAd.utils.QRCodeUtil;
import com.idreems.openvmAd.utils.ReusableTimer;
import com.idreems.openvmAd.utils.TaskUtils;
import com.idreems.openvmAd.utils.Utils;

import java.util.TimerTask;

/**
 * Created by ramonqlee on 5/19/16.
 */
public class FSImagePlayerView implements IFSMediaPlayerView {
    private static final long QRCODE_TIMEOUT = 10 * Consts.ONE_MINUTE_IN_MS;//缓存中二维码失效的时间
    private static final long ONE_SECOND_IN_MS = 1000;
    private SimpleDraweeView mImageView;
    private IFSMedia mMedia;
    private boolean mIsPlaying;
    private OnCompletionListener mCompleteListener;
    private OnPreparedListener mOnPrepareListener;
    private OnCurrentTimeUpdateListener mOnCurrentTimeUpdateListener;

    private Handler sUIHandler = new Handler(Looper.getMainLooper());
    private ReusableTimer mLoopTimer;
    private TimerTask mLoopTimerTask;
    private ReusableTimer mAnimaTimer = null;
    private ReusableTimer mQrcodeTimer = null;
    private boolean mPlayAnima = false;
    private boolean mBackgoundMode;

    //图片缓存
    private String mLastestBackgroundUrl;//最近一次背景的url
    private LruCache<String, Bitmap> mMemoryCache = new LruCache(2);//背景的内存缓存,最多保留最近使用的几个位图

    //图片和识别的二维码的缓存
    private LruCache<String, String> mUrl2QRCodeCache = new LruCache<>(100);//二维码url和识别的位图url之间的对应关系,保留最近100条
    private long mEldestIdentifiedTimeInMs;//第一次二维码识别的时间，防止缓存中二维码过期的情况
    private static int sPlayThirdAdCount;//播放第三方广告的次数

    private ReusableTimer mPrepareTimer;

    public FSImagePlayerView(SimpleDraweeView imageView) {
        mImageView = imageView;
    }

    public void playAnimation(boolean enable) {
        mPlayAnima = enable;
    }

    public void setDimension(int width, int height) {
    }

    // 是否后台模式
    public void setBackgoundMode(boolean backgoundMode) {
        mBackgoundMode = backgoundMode;
    }

    public void setHttpProxyCacheServer(HttpProxyCacheServer cacheServer) {
    }

    public void setVideoViewParent(ViewGroup parent) {
    }

    public View getPlayerView() {
        return mImageView;
    }

    public void updateData(IFSMedia media) {
        if (null == media) {
            return;
        }
        mMedia = media;
    }

    public void showProgress(boolean show) {
    }

    public IFSMedia getMedia() {
        return mMedia;
    }

    private boolean isPlaying() {
        return mIsPlaying;
    }

    private void setPlaying(boolean play) {
        mIsPlaying = play;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPrepareListener = listener;
    }

    public void setOnCurrentTimeListener(OnCurrentTimeUpdateListener listener) {
        mOnCurrentTimeUpdateListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mCompleteListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
    }

    public void stop() {
        release();
        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "notifyCompleteListener in stop");
        }
        notifyCompleteListener(false);
    }

    public void release() {
        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "release");
        }
        sUIHandler.removeCallbacksAndMessages(null);
        if (null != mAnimaTimer) {
            mAnimaTimer.cancel();
            mAnimaTimer = null;
        }
        if (null != mQrcodeTimer) {
            mQrcodeTimer.cancel();
            mQrcodeTimer = null;
        }

        stopPrepareTimeoutMonitor();
        stopLoopTimer();
        setGone();
        mMemoryCache.evictAll();
        mUrl2QRCodeCache.evictAll();

        //release image
        if (null != mImageView.getHierarchy()) {
            mImageView.getHierarchy().setPlaceholderImage(null);
            mImageView.getHierarchy().setBackgroundImage(null);
        }

        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "release image player");
        }
    }

    public boolean isBackgoundMode() {
        return mBackgoundMode;
    }

    @Override
    public void setGone() {
        MyApplication.getUIHandler().post(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageDrawable(null);
                mImageView.setVisibility(View.GONE);
            }
        });
    }

    private void complete() {
        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "complete");
        }
        sUIHandler.removeCallbacksAndMessages(null);
        stopLoopTimer();
    }

    public void start() {
        if (null == mImageView) {
            return;
        }
        try {
//            GlobalData.sIdentifiedMedia = null;

            if (Consts.logEnabled()) {
                Log.d(Consts.IMG_PLAYER_TAG, FSImagePlayerView.class.getSimpleName() + " start " + " isBackground=" + isBackgoundMode() + mMedia.getUrl() + " getRequestId:" + mMedia.getRequestId());
            }
            final Context context = mImageView.getContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && (context instanceof Activity) && ((Activity) context).isDestroyed()) {
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, FSImagePlayerView.class.getSimpleName() + " illegal display,return");
                }
                return;
            }

            if (isPlaying()) {
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, FSImagePlayerView.class.getSimpleName() + " isPlaying,return " + mMedia.getUrl() + " getRequestId:" + mMedia.getRequestId());
                }
                return;
            }

            if (null == mMedia || null == mImageView || TextUtils.isEmpty(mMedia.getUrl())) {
                notifyCompleteListener(false);
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, "notifyCompleteListener in start,empty url");
                }
                return;
            }
            setPlaying(true);//设置正常开始的标志

            if (Consts.logEnabled()) {
                Log.d(Consts.IMG_PLAYER_TAG, FSImagePlayerView.class.getSimpleName() + " start  " + mMedia.getUrl() + " getRequestId:" + mMedia.getRequestId());
            }

            // 开始显示，并在显示后计时
            mImageView.getHierarchy().setFadeDuration(0);
            mImageView.setVisibility(View.VISIBLE);
            mImageView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);

            // 如果和之前是一样的图片，就直接开始倒计时了，不再重新显示图片
            AbstractDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder();
            builder.setUri(mMedia.getUrl());
            if (null != mImageView.getController() && mImageView.getController().isSameImageRequest(builder.build())) {
                startImageDisplayTimer();
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, "displayed image,url =" + mMedia.getUrl());
                }
                return;
            }

            if (mMedia.getUrl().contains("file://")) {
                Utils.displayImage(mImageView, Uri.parse("file://" + mMedia.getUrl()));
                startImageDisplayTimer();
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, "display cached image,url =" + mMedia.getUrl());
                }
            } else {
                ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {

                    void updateViewSize(ImageInfo imageInfo) {
                        //贴片广告，需要动态变化图片尺寸
                        if (!TextUtils.equals(mMedia.getAd_position(), Consts.POSITIONADCENTER)) {
                            return;
                        }

                        if (imageInfo != null) {
                            float scale = (float) DeviceUtils.getScreenWidth() / (float) Consts.TV_WIDTH;
                            mImageView.getLayoutParams().width = (int) (scale * imageInfo.getWidth());
                            mImageView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            mImageView.setAspectRatio((float) imageInfo.getWidth() / imageInfo.getHeight());
                        }
                    }

                    @Override
                    public void onFinalImageSet(
                            String id,
                            ImageInfo imageInfo,
                            Animatable anim) {
                        if (Consts.logEnabled()) {
                            Log.d(Consts.IMG_PLAYER_TAG, "in onFinalImageSet");
                        }

                        stopPrepareTimeoutMonitor();

                        updateViewSize(imageInfo);
                        startImageDisplayTimer();

                        final Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                if (Consts.logEnabled()) {
                                    Log.d(Consts.PLAYER_TAG, "set background,mLastestBackgroundUrl=" + mLastestBackgroundUrl + " mMedia.getUrl()=" + mMedia.getUrl());
                                }

                                // 如果图片变了，切换背景
                                if (!TextUtils.equals(mLastestBackgroundUrl, mMedia.getUrl())) {
                                    //图片变了，尝试切换背景
                                    final Bitmap bitmap = getBitmap(mMedia.getUrl());
                                    if (null != bitmap) {
                                        MyApplication.getUIHandler().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mImageView.getHierarchy().setBackgroundImage(new BitmapDrawable(bitmap));
                                                if (null != mMedia && TextUtils.equals(mMedia.getUrl(), mLastestBackgroundUrl)) {
                                                    return;
                                                }
                                                //更新最近一次背景的url
                                                mLastestBackgroundUrl = mMedia.getUrl();
                                                if (Consts.logEnabled()) {
                                                    Log.d(Consts.PLAYER_TAG, "change background to url =" + mMedia.getUrl());
                                                }
                                            }
                                        });
                                    } else {
                                        if (Consts.logEnabled()) {
                                            Log.d(Consts.PLAYER_TAG, "oopse,null bitmap");
                                        }
                                    }
                                } else {
                                    if (Consts.logEnabled()) {
                                        Log.d(Consts.PLAYER_TAG, "same background to url =" + mMedia.getUrl());
                                    }
                                }
                            }
                        };
                        TaskUtils.runOnExecutorService(r);
                    }

                    @Override
                    public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
                        updateViewSize(imageInfo);
                        stopPrepareTimeoutMonitor();
                        if (Consts.logEnabled()) {
                            Log.d(Consts.IMG_PLAYER_TAG, "in onIntermediateImageSet");
                        }
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        mImageView.getHierarchy().setPlaceholderImage(null);

                        stopPrepareTimeoutMonitor();
                        if (!TextUtils.equals(mMedia.getAd_position(), Consts.POSITIONADPRE)) {
                            if (Consts.logEnabled()) {
                                Log.d(Consts.AD_TAG, "onFailure FSImagePlayerView preloadThirdAd adPosition=" + mMedia.getAd_position());
                            }
                            NewAdRequest.getInstance().preloadThirdAd(mMedia.getAd_position());
                        }

                        if (Consts.logEnabled()) {
                            Log.d(Consts.IMG_PLAYER_TAG, "onFailure");
                        }
                        //图片加载不出来时，防止一直闪屏
                        sUIHandler.removeCallbacksAndMessages(null);
                        sUIHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                notifyCompleteListener(false);
                                if (Consts.logEnabled()) {
                                    Log.d(Consts.IMG_PLAYER_TAG, "notifyCompleteListener in start,fail to display image");
                                }
                            }
                        }, 2 * 1000);
                    }
                };

                Uri uri = Uri.parse(mMedia.getUrl());
                // 不显示加载中占位图的情况
                // 1. 贴片广告
                // 2. 本地有缓存时
                if (!isBackgoundMode()) {
                    if (TextUtils.equals(mMedia.getAd_position(), Consts.POSITIONADCENTER) || Utils.isInFrescoCache(mMedia.getUrl())) {
                        if (mImageView.getHierarchy().hasPlaceholderImage()) {
                            mImageView.getHierarchy().setPlaceholderImage(null);
                            if (Consts.logEnabled()) {
                                Log.d(Consts.IMG_PLAYER_TAG, "setPlaceholderImage to null");
                            }
                        }
                    } else {//其他情况下,显示加载中
                        mImageView.getHierarchy().setPlaceholderImage(R.mipmap.loading);

                        if (Consts.logEnabled()) {
                            Log.d(Consts.IMG_PLAYER_TAG, "setPlaceholderImage to loading");
                        }
                    }
                }

                if (mPlayAnima && !Consts.isGIF(uri.getPath())) {
                    startAnimation(0, 1, 0, 1, null);//旋转放大

                    if (null != mAnimaTimer) {
                        mAnimaTimer.cancel();
                        mAnimaTimer = null;
                    }
                    int playTime = (int) Math.abs(mMedia.getEndTimeInSec() - mMedia.getStartTimeInSec());
                    int animStickTime = 0;
//                    if (VideoUtil.getStickerAnimSpan() > 0 && VideoUtil.getStickerAnimSpan() < playTime) {
//                        animStickTime = VideoUtil.getStickerAnimSpan();
//                    }
                    if (animStickTime > 0) {
                        mAnimaTimer = new ReusableTimer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                MyApplication.getUIHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        startShakeByView();
                                    }
                                });
                            }
                        };
                        mAnimaTimer.schedule(task, 1 * Consts.ONE_SECOND_IN_MS, animStickTime * Consts.ONE_SECOND_IN_MS);
                    }
                }
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, FSImagePlayerView.class.getSimpleName() + " isBackgoundMode=" + isBackgoundMode());
                }
                // 如果是后台模式，仅下载图片，同时启动图片倒计时，并不展示图片
                if (isBackgoundMode()) {
                    //下载图片
                    if (!Utils.isInFrescoCache(mMedia.getUrl())) {
                        Fresco.getImagePipeline().prefetchToDiskCache(ImageRequest.fromUri(uri), MyApplication.getContext());
                    }

                    //启动图片倒计时
                    startImageDisplayTimer();
                } else {
                    // 启动定时监控,如果图片在指定时间内没显示，则不再显示；否则，在图片显示后，会调用stopPrepareTimeoutMonitor取消
                    startPrepareTimeoutMonitor();
                    Utils.displayImage(mImageView, uri, controllerListener);
                }
            }

            if (null != mOnPrepareListener) {
                mOnPrepareListener.onPrepared(this);
            }
        } catch (Exception ex) {
            notifyCompleteListener(false);
            if (Consts.logEnabled()) {
                Log.d(Consts.IMG_PLAYER_TAG, "notifyCompleteListener in start,exception =" + ex.getMessage());
            }
            ex.printStackTrace();
        }
    }

    //执行动画
    private void startAnimation(float fromX, float toX, float fromY, float toY, Animation.AnimationListener listener) {
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        LinearInterpolator lin = new LinearInterpolator();
        rotate.setInterpolator(lin);
        rotate.setDuration(1000);//设置动画持续时间
        rotate.setRepeatCount(0);//设置重复次数
        rotate.setFillAfter(true);//动画执行完后是否停留在执行完的状态
//                rotate.setStartOffset(10);//执行前的等待时间

        ScaleAnimation scaleAnimation = new ScaleAnimation(fromX, toX, fromY, toY, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(1000);//设置动画持续时间
        rotate.setFillAfter(true);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(rotate);
        animationSet.addAnimation(scaleAnimation);
        animationSet.setAnimationListener(listener);
        mImageView.startAnimation(animationSet);
    }

    //动画抖动
    private void startShakeByView() {
        //由小变大
        Animation scaleAnim = new ScaleAnimation(0.8f, 1f, 0.8f, 1f);
        //从左向右
        Animation rotateAnim = new RotateAnimation(-3, 3, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        scaleAnim.setDuration(1000);
        rotateAnim.setDuration(100);
        rotateAnim.setRepeatMode(Animation.REVERSE);
        rotateAnim.setRepeatCount(10);

        AnimationSet smallAnimationSet = new AnimationSet(false);
        smallAnimationSet.addAnimation(scaleAnim);
        smallAnimationSet.addAnimation(rotateAnim);
        mImageView.startAnimation(smallAnimationSet);
    }

    private void startPrepareTimeoutMonitor() {
        stopPrepareTimeoutMonitor();

        mPrepareTimer = new ReusableTimer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, "Image prepare monitor timeout");
                }

                sUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyCompleteListener(true);
                    }
                });
            }
        };

        mPrepareTimer.schedule(task, 15 * Consts.ONE_SECOND_IN_MS);//15秒的时间
        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "startPrepareTimeoutMonitor " + " isBackgoundMode=" + isBackgoundMode() + " url=" + mMedia.getUrl());
        }
    }

    private void stopPrepareTimeoutMonitor() {
        if (null != mPrepareTimer) {
            if (Consts.logEnabled()) {
                Log.d(Consts.IMG_PLAYER_TAG, "stopPrepareTimeoutMonitor");
            }
            mPrepareTimer.cancel();
            mPrepareTimer = null;
        }
    }

    private void startImageDisplayTimer() {
        if (IFSMedia.INFINITE_END_TIME_IN_SEC < mMedia.getEndTimeInSec()) {
            if (Consts.logEnabled()) {
                Log.d(Consts.IMG_PLAYER_TAG, "startImageDisplayTimer now,countdown = " + mMedia.getEndTimeInSec() + " url=" + mMedia.getUrl());
            }
            sUIHandler.removeCallbacksAndMessages(null);
            sUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Consts.logEnabled()) {
                        Log.d(Consts.IMG_PLAYER_TAG, "startImageDisplayTimer time's up url=" + mMedia.getUrl());
                    }
                    Uri uri = Uri.parse(mMedia.getUrl());
                    if (mPlayAnima && Consts.isGIF(uri.getPath())) {
                        if (null != mAnimaTimer) {
                            mAnimaTimer.cancel();
                            mAnimaTimer = null;
                        }
                        startAnimation(1, 0, 1, 0, new Animation.AnimationListener() {//旋转缩小
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                notifyCompleteListener(true);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                    } else {
                        notifyCompleteListener(true);
                    }
                }
            }, mMedia.getEndTimeInSec() * 1000);
            // FIXME :预加载下一条广告,待优化
            if (!TextUtils.equals(mMedia.getAd_position(), Consts.POSITIONADPRE)) {
                if (Consts.logEnabled()) {
                    Log.d(Consts.AD_TAG, "startImageDisplayTimer FSImagePlayerView preloadThirdAd adPosition=" + mMedia.getAd_position());
                }
                NewAdRequest.getInstance().preloadThirdAd(mMedia.getAd_position());
            }
            if (mMedia.getAd_position().equals(Consts.POSITIONADPRE)) {
                startLoopTimer();
            }
        } else {
            notifyCompleteListener(false);
        }

    }

    private void startLoopTimer() {
        stopLoopTimer();

        //no listener,no timer
        if (null == mOnCurrentTimeUpdateListener) {
//            if(Consts.logEnabled()) Log.d(Consts.IMG_PLAYER_TAG, "FSImagePlayerView mOnCurrentTimeUpdateListener is null");
            return;
        }
        // start loop timer
        mLoopTimer = new ReusableTimer();
        mLoopTimerTask = new TimerTask() {
            private int currentTimeInMs = 0;

            @Override
            public void run() {
                if (null == mOnCurrentTimeUpdateListener) {
                    return;
                }

                currentTimeInMs += ONE_SECOND_IN_MS;
                mOnCurrentTimeUpdateListener.onCurrentTimeUpdate(FSImagePlayerView.this, currentTimeInMs);
                if (Consts.logEnabled()) {
                    Log.d(Consts.IMG_PLAYER_TAG, "FSImagePlayerView onCurrentTimeUpdate time= " + currentTimeInMs);
                }
            }
        };
        mLoopTimer.schedule(mLoopTimerTask, ONE_SECOND_IN_MS, ONE_SECOND_IN_MS);
    }

    private void stopLoopTimer() {
        if (null == mLoopTimer) {
            return;
        }
//        mLoopTimerTask.cancel();
        mLoopTimer.cancel();
        mLoopTimer = null;
        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "FSImagePlayerView stopLoopTimer");
        }
    }

    private void notifyCompleteListener(final boolean isEnd) {
        // 1. 首先查看是否在正常播放状态,不在的话，说明已经通知过了，直接返回
        // 2. 否则走相关流程，设置状态为未播放状态
        if (!isPlaying()) {
            if (Consts.logEnabled()) {
                Log.d(Consts.IMG_PLAYER_TAG, "notifyCompleteListener already called,ignore");
            }
            return;
        }

        if (Consts.logEnabled()) {
            Log.d(Consts.IMG_PLAYER_TAG, "notifyCompleteListener mCompleteListener=" + mCompleteListener);
        }
        setPlaying(false);//重新恢复未播放状态
        complete();
        if (null == mCompleteListener) {
            return;
        }

        mCompleteListener.onCompletion(FSImagePlayerView.this, isEnd);
    }

    //获取url和识别的二维码url之间的对应关系
    private String getQRCache(String url) {
        if (0 != mEldestIdentifiedTimeInMs && Math.abs(mEldestIdentifiedTimeInMs - SystemClock.elapsedRealtime()) > QRCODE_TIMEOUT) {
            mUrl2QRCodeCache.evictAll();
            mEldestIdentifiedTimeInMs = 0;//重置识别的开始时间
            return "";
        }

        return mUrl2QRCodeCache.get(url);
    }

    //保存url和识别的二维码url之间的对应关系
    private void saveQRCache(String url, String qrCode) {
        if (0 == mEldestIdentifiedTimeInMs) {//记录第一次识别的时间
            mEldestIdentifiedTimeInMs = SystemClock.elapsedRealtime();
        }

        mUrl2QRCodeCache.put(url, qrCode);
    }

    //提取url对应位图的缓存
    private Bitmap getBitmap(String url) {
        //有缓存，直接使用;没有的话，尝试从本地提取
        Bitmap bmp = mMemoryCache.get(url);
        if (null != bmp) {
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "getBitmap in bitmap cache");
            }
            return bmp;
        }

        Bitmap r = QRCodeUtil.fetchFrescoBitmap(Uri.parse(url));
        // 缓存下
        if (null != r) {
            mMemoryCache.put(url, r);
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "getBitmap from fetchFrescoBitmap and save to cache");
            }
        }

        return r;
    }
}
