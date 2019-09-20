package com.idreems.openvmAd.multimedia;

import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.R;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMediaPlayer;
import com.idreems.openvmAd.multimedia.media.router.VideoViewRouter;
import com.idreems.openvmAd.multimedia.media.ui.widget.VideoViewInf;


/**
 * Created by lizy on 15-12-6.
 * 支持视频，图片轮播
 * 视频和图片有播放插件，其中的数据有单独的管理器进行管理，每项进入播放后，自己管理自己的播放时间和内容，结束后回调，然后进入下一个项目的播放
 * 据此，轮播模块模块如下
 * 1. 播放插件管理器：
 * 支持播放的插件，包括视频和图片两类
 * 支持开始，暂停，销毁，播放回调等接口
 * <p/>
 * 2. 数据管理器
 * 管理所有的数据，并有一个游标记录当前项
 */
public class MainAdLinearLayout extends LinearLayout {
    private static final String TAG = MainAdLinearLayout.class.getSimpleName();

    ViewGroup mContainerViewGroup;
    TextView mTextView;
    SimpleDraweeView mImageView;
    TextView showAdress;
    WebView mQrCodeWebview;

    Context mContext;
    boolean mPaused;//暂停播放首页广告
    VideoViewInf mVLCVideoView;
    FrameLayout mVideoViewParent;

    FSMediaPlayer.PlayerViewProvider mHomePageAdViewProvider = new FSMediaPlayer.PlayerViewProvider() {
        public SimpleDraweeView imageView() {
            return mImageView;
        }

        public VideoViewInf videoView() {
            initVideo();
            return mVLCVideoView;
        }

        public ViewGroup getParentView() {
            return mVideoViewParent;
        }
    };

    private FSMediaPlayer mHomePageAdCountryPlayer = MyApplication.getApplication().getMainVideoCountryPlayer();
    private FSMediaPlayer.PlayListener mHomePagePlayListener;

    private InfinitePlayer mInfinitePlayer = new InfinitePlayer();

    public MainAdLinearLayout(Context context) {
        super(context);
        initView(context);
    }

    public MainAdLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MainAdLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initVideo() {
        if (null != mVLCVideoView) {
            return;
        }
        String playerEngine = "vv";
        final boolean hwDecode = true;
        mVLCVideoView = VideoViewRouter.createVideoView(getContext().getApplicationContext(), playerEngine);
        mVLCVideoView.setHWDecode(hwDecode);
        mVLCVideoView.setSurfaceView(mVideoViewParent);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        mVideoViewParent.addView(mVLCVideoView.view(), lp);
        mVLCVideoView.view().setFocusable(false);
        mVLCVideoView.view().clearFocus();
        mVLCVideoView.view().setEnabled(false);
    }

    private void initView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.fragment_main_play, null);
        this.addView(v);

        mContainerViewGroup = (ViewGroup) v.findViewById(R.id.layout_container);
        mTextView = (TextView) v.findViewById(R.id.textView_name);
        mImageView = (SimpleDraweeView) v.findViewById(R.id.imageView);
        showAdress = (TextView) v.findViewById(R.id.showAdress);
        mQrCodeWebview = (WebView) v.findViewById(R.id.qrcode_wv);

        mContext = context;
        mVideoViewParent = (FrameLayout) v.findViewById(R.id.video_framelayout);
        TextView showAdress2 = (TextView) v.findViewById(R.id.showAdress2);
        showAdress2.setVisibility(View.GONE);
//        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mVideoViewParent.getLayoutParams();
//        params.width = (int) getResources().getDimension(R.dimen.dp604);
//        params.height = (int) getResources().getDimension(R.dimen.dp340);
//        initVideo();

        showAdress.setVisibility(View.GONE);
        mPaused = false;
    }

//    public void setSize(int width, int height) {
//        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mVideoViewParent.getLayoutParams();
//        params.width = width;
//        params.height = height;
//
//        FrameLayout.LayoutParams params1 = (FrameLayout.LayoutParams) mImageView.getLayoutParams();
//        params1.width = width;
//        params1.height = height;
//        mImageView.setLayoutParams(params1);
//    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (VISIBLE != visibility) {
//            releaseAdRequest();
        }
    }

    private void setDisplayBar(String name) {
        // bugfix:修复从activity移除的bug
        // 如果配置了内容，则显示，否则不显示底部状态说明
        if (!TextUtils.isEmpty(name) && !name.contains("PLATFORM")) {
            mTextView.setVisibility(VISIBLE);
//            mTextView.setText(String.format(getResources().getString(R.string.video_name), name));
        } else {
            mTextView.setVisibility(GONE);
            mTextView.setText(null);
        }
    }

    public void releaseAdRequest() {
        mPaused = false;
        if (null != mInfinitePlayer) {
            mInfinitePlayer.setState(true);
        }

        // 暂停播放，待下次继续播放
        if (null != mHomePageAdCountryPlayer) {
            //释放资源
            mHomePageAdCountryPlayer.clearPlayerListener();
            mHomePageAdCountryPlayer.release();
            // 清理回调
            mHomePageAdCountryPlayer.setPlayerViewProvider(null);
        }
    }

    public void startAdRequest() {
        if (mPaused) {
            return;
        }

        mHomePageAdCountryPlayer.clearPlayerListener();
        mHomePageAdCountryPlayer.setPlayerViewProvider(mHomePageAdViewProvider);
        // 首页广告每次播放的回调，显示一些信息
        if (null == mHomePagePlayListener) {
            mHomePagePlayListener = new FSMediaPlayer.PlayListener() {
                @Override
                public void onPrepareToStart(IFSMedia media) {
                    if (null == media || TextUtils.isEmpty(media.getName())) {
                        mTextView.setText("");
                        return;
                    }

                    setDisplayBar(media.getName());
                }

                @Override
                public void onPrepareToPlay(IFSMedia media) {
                    mPaused = true;
                    if (null == media || TextUtils.isEmpty(media.getName())) {
                        mTextView.setText("");
                        return;
                    }
//                    setDisplayBar(media.getName());
                }

                @Override
                public void onCurrentTimePlay(IFSMedia media, int currentTime) {
                }

                @Override
                public boolean onAfterPlay(IFSMedia media) {
                    if (!media.isImage()) {
                        mVLCVideoView.stopPlayback(); //停止视频播放,并释放资源
                    }
                    return true;
                }
            };
        }
        mHomePageAdCountryPlayer.removePlayerLister(mHomePagePlayListener);
        mHomePageAdCountryPlayer.addPlayListener(mHomePagePlayListener);

        if (null != mInfinitePlayer) {
            mInfinitePlayer.setState(false);
            mInfinitePlayer.startLoop(MyApplication.getContext(), Consts.POSITIONADHOME, mHomePageAdCountryPlayer);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        try {
            super.onRestoreInstanceState(state);
        } catch (Exception e) {
            state = null;
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (Consts.logEnabled()) {
            Log.d(TAG, "onDetachedFromWindow");
        }
    }

}
