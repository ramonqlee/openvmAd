package com.idreems.openvmAd.multimedia.admanager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.AdHandler;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.multimedia.callback.ListHttpCallback;
import java.util.Vector;

/**
 * Created by ramonqlee_macpro on 2019/4/24.
 */

//顺序广告请求器
// 注册广告请求器，按照注册顺序，逐个请求，一旦请求到广告，则停止请求，否则一直请求，直到最后一个
public class SeqAdRequestManager {
    private static final String TAG = SeqAdRequestManager.class.getSimpleName();
    private static final int INVALID_POS = -1;
    private Vector<AdHandler> mAdHandlerVector = new Vector<>();
    private AdListener mAdListener;
    private int mPrevPos = INVALID_POS;
    private int mCurrentAdPos = 0;
    private Context mContext;
    private String mAdPosition;
    private AdHandler mCandidateHandler;
    private boolean mPreMode;

    public SeqAdRequestManager() {
    }

    public void setListener(AdListener listener) {
        mAdListener = listener;
    }

    public boolean isEmpty() {
        return (null == mAdHandlerVector) || (mAdHandlerVector.isEmpty());
    }

    public void clearCustomAdHandlers() {
        // TODO 遍历所有的，然后清理自定义的广告请求器
        if (mAdHandlerVector.isEmpty()) {
            return;
        }

        synchronized (this) {
            if (Consts.logEnabled()) {
                Log.d(TAG, "clearCustomAdHandlers before remove = " + mAdHandlerVector.size());
            }
            for (int i = mAdHandlerVector.size() - 1; i >= 0; i--) {
                AdHandler handler = mAdHandlerVector.get(i);
                if (null == handler || handler.isSystemConfigedAdPlatform()) {
                    continue;
                }
                mAdHandlerVector.remove(i);
                if (Consts.logEnabled()) {
                    Log.d(TAG, "clearCustomAdHandlers remove " + handler + " mac = " + handler.getDeviceId());
                }
            }
            if (Consts.logEnabled()) {
                Log.d(TAG, "clearCustomAdHandlers after remove = " + mAdHandlerVector.size());
            }
        }

        // 重置广告请求位置
        mCurrentAdPos = 0;
    }

    public void add2Head(AdHandler adHandler) {
        if (null == adHandler) {
            return;
        }

        synchronized (this) {
            if (Consts.logEnabled()) {
                Log.d(TAG, "add2Head before add size = " + mAdHandlerVector.size());
            }
            // 添加到广告处理器中，并且重置请求的顺序
            if (mAdHandlerVector.isEmpty()) {
                mAdHandlerVector.add(adHandler);
            } else {
                mAdHandlerVector.add(0, adHandler);
            }
            if (Consts.logEnabled()) {
                Log.d(TAG, "add2Head after add size = " + mAdHandlerVector.size());
            }
        }

        mCurrentAdPos = 0;
    }

    public void add(AdHandler handler) {
        if (null == handler) {
            return;
        }
        if (-1 != mAdHandlerVector.indexOf(handler)) {
            return;
        }
        synchronized (this) {
            mAdHandlerVector.add(handler);
        }
        if (Consts.logEnabled()) {
            Log.d(TAG, "add handler =" + handler);
        }
    }

    public void setCandidate(AdHandler handler) {
        mCandidateHandler = handler;
        if (Consts.logEnabled()) {
            Log.d(TAG, "setCandidate =" + mCandidateHandler);
        }
    }

    public void remove(AdHandler handler) {
        if (null == handler) {
            return;
        }
        synchronized (this) {
            mAdHandlerVector.remove(handler);
        }
    }

    public int size() {
        return mAdHandlerVector.size();
    }

    public void reset() {
        mCurrentAdPos = 0;
        synchronized (this) {
            mAdHandlerVector = new Vector<>();
        }
        mPreMode = false;
    }

    public void run(Context context, String adPosition) {
        if (null == context || TextUtils.isEmpty(adPosition)) {
            return;
        }
        // 逐个请求
        mContext = context.getApplicationContext();
        mAdPosition = adPosition;

        //一进入就尾部了，则自动恢复到头部
        if (mCurrentAdPos >= mAdHandlerVector.size()) {
            mCurrentAdPos = 0;
        }
        runCurrentHandler();
    }

    private void notifyForTail() {
        //已经到头了，则返回
        if (Consts.logEnabled()) {
            Log.d(TAG, String.format("notifyForTail run for pos = %d, total = %d,end now", mCurrentAdPos, mAdHandlerVector.size()));
        }

        if (mCurrentAdPos < mAdHandlerVector.size()) {
            return;
        }

        // 如果没有候选的，则使用
        if (null == mCandidateHandler) {
            notifyOuterListener(null);
        } else {
            //如果有候选，则返回前使用候选
            mCandidateHandler.setCompleteListener(new ListHttpCallback() {
                @Override
                public void onResponse(IFSMedia media) {
                    notifyOuterListener(media);
                }
            });
            mCandidateHandler.setPreMode(isPreloadMode());
            mCandidateHandler.start(mContext, mAdPosition);

            if (Consts.logEnabled()) {
                Log.d(TAG, "run candidate handler");
            }
        }
    }

    private void incrementPosition() {
        if (isPreloadMode() && INVALID_POS == mPrevPos) {
            if (Consts.logEnabled()) {
                Log.d(TAG, "isPreloadMode, mCurrentAdPos=" + mCurrentAdPos);
            }
            mPrevPos = mCurrentAdPos;
        }
        mCurrentAdPos++;
    }

    private void runCurrentHandler() {
        try {
            if (Consts.logEnabled()) {
                Log.d(TAG, "runCurrentHandler mCurrentAdPos=" + mCurrentAdPos + " mAdHandlerVector.size()=" + mAdHandlerVector.size());
            }
            // 到头了，则直接返回，并发送通知
            if (mCurrentAdPos >= mAdHandlerVector.size()) {
                notifyForTail();
                return;
            }

            // 在添加的地方判断了，不会为空
            final AdHandler handler = mAdHandlerVector.get(mCurrentAdPos);
            handler.setCompleteListener(new ListHttpCallback() {
                @Override
                public void onResponse(IFSMedia media) {
                    if (Consts.logEnabled()) {
                        Log.d(TAG, "onResponse received handler=" + handler + " mac = " + handler.getDeviceId());
                    }

                    // 有数据返回，直接返回；没有的话，尝试进入下一个位置
                    if (null != media) {
                        if (Consts.logEnabled()) {
                            Log.d(TAG, String.format("run for pos = %d, total = %d,find ad and return", mCurrentAdPos, mAdHandlerVector.size()));
                        }
                        notifyOuterListener(media);
                        return;
                    }

                    // 如果到头了，则调用到头的回调
                    incrementPosition();
                    if (mCurrentAdPos >= mAdHandlerVector.size()) {
                        notifyForTail();
                        return;
                    }

                    // 继续下一次
                    runCurrentHandler();
                }
            });

            handler.setPreMode(isPreloadMode());
            handler.start(mContext, mAdPosition);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setPreMode(boolean preMode) {
        mPreMode = preMode;
    }

    //是否预加载广告模式：没有对外的回调，可以认为是预加载模式
    public boolean isPreloadMode() {
        return mPreMode;
    }

    private void notifyOuterListener(IFSMedia media) {
        //预加载模式，回滚位置
        if (isPreloadMode() && INVALID_POS != mPrevPos) {
            if (Consts.logEnabled()) {
                Log.d(TAG, "isPreloadMode will reset position, mCurrentAdPos=" + mCurrentAdPos + " mPrevPos=" + mPrevPos + " media=" + media);
            }
            mCurrentAdPos = mPrevPos;
            mPrevPos = INVALID_POS;//reset previous position
        }
        if (null != mAdListener) {
            mAdListener.onNotify(media);
        }
    }
}
