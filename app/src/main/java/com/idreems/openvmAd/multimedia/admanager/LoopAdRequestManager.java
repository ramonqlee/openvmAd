package com.idreems.openvmAd.multimedia.admanager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;

import java.util.Vector;

/**
 * Created by ramonqlee_macpro on 2019/4/24.
 */

//轮流广告请求器
public class LoopAdRequestManager {
    private static final String TAG = LoopAdRequestManager.class.getSimpleName();
    private Vector<SeqAdRequestManager> mAdVector = new Vector<>();
    private AdListener mOuterAdListener;
    private int mCurrentPos;
    private int mPrevPos;
    private int mAdCount;
    private Context mContext;
    private String mAdPosition;
    private SeqAdRequestManager mCandidateManager;
    private boolean mPreMode;

    public LoopAdRequestManager() {
    }

    public void setListener(AdListener listener) {
        mOuterAdListener = listener;
    }

    public int size() {
        return mAdVector.size();
    }

    public void add(SeqAdRequestManager manager) {
        if (null == manager) {
            return;
        }
        if (-1 != mAdVector.indexOf(manager)) {
            return;
        }

        mAdVector.add(manager);
        if (Consts.logEnabled()) {
            Log.d(TAG, "add manager =" + manager);
        }
    }

    public SeqAdRequestManager get(int index) {
        if (index < 0 || index >= mAdVector.size()) {
            return null;
        }
        return mAdVector.get(index);
    }

    public void setCandidate(SeqAdRequestManager manager) {
        mCandidateManager = manager;
        if (Consts.logEnabled()) {
            Log.d(TAG, "setCandidate =" + mCandidateManager);
        }
    }

    public void remove(SeqAdRequestManager handler) {
        if (null == handler) {
            return;
        }
        mAdVector.remove(handler);
    }

    public void clear() {
        mAdVector = new Vector<>();
    }

    public void reset() {
        mPrevPos = 0;
        mCurrentPos = 0;
        mAdCount = 0;
    }

    public void run(Context context, String adPosition) {
        if (null == context || TextUtils.isEmpty(adPosition)) {
            return;
        }
        // 逐个请求
        mContext = context.getApplicationContext();
        mAdPosition = adPosition;
        // TODO 轮流请求
        // 先请求第一个，如果前面的返回了，直接返回就好，下次从上次请求的后面继续请求，到头的话，就回到最开始,每次最多进行mAdVector.size请求
        runFromCurrent();
    }

    private void runFromCurrent() {
        try {
            //本次请求已经轮了一轮了，结束本次请求
            if (++mAdCount >= mAdVector.size()) {
                if (Consts.logEnabled()) {
                    Log.d(TAG, "finish one loop,but no media found,return,isPreloadMode=" + isPreloadMode() + " mPrevPos =" + mPrevPos + " mCurrentPos=" + mCurrentPos);
                }
                // 如果是预加载模式，则直接返回吧
                if (isPreloadMode()) {
                    mAdCount--;//decrement
                    return;
                }
                mAdCount = 0;
                // 如果没有候选的，则使用
                if (null == mCandidateManager) {
                    notifyOuterListener(null);
                } else {
                    //如果有候选，则返回前使用候选
                    mCandidateManager.setPreMode(isPreloadMode());
                    mCandidateManager.setListener(mOuterAdListener);
                    mCandidateManager.run(mContext, mAdPosition);
                    if (Consts.logEnabled()) {
                        Log.d(TAG, "run candidate manager");
                    }
                }
                return;
            }

            // 如果已经到尾了，则返回头，继续请求
            if (mCurrentPos >= mAdVector.size()) {
                if (Consts.logEnabled()) {
                    Log.d(TAG, "tail now,return to head");
                }
                mCurrentPos = 0;
            }

            SeqAdRequestManager manager = mAdVector.get(mCurrentPos);
            if (null == manager) {
                if (Consts.logEnabled()) {
                    Log.d(TAG, "null manager,return");
                }
                notifyInnerListener(null);
                return;
            }

            if (Consts.logEnabled()) {
                Log.d(TAG, String.format("run at pos = %d with %s,total = %d", mCurrentPos, manager, mAdVector.size()));
            }

            mPrevPos = mCurrentPos;
            mCurrentPos++;//increment ad request position
            manager.setPreMode(isPreloadMode());
            manager.setListener(mInnerAdListener);
            manager.run(mContext, mAdPosition);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private AdListener mInnerAdListener = new AdListener() {
        @Override
        public void onNotify(IFSMedia media) {
            if (null != media) {
                //如果是预加载模式，则返回上次的广告请求器，然后返回
                if (isPreloadMode()) {
                    if (Consts.logEnabled()) {
                        Log.d(TAG, "find media,go back and return,mPrevPos =" + mPrevPos + " mCurrentPos=" + mCurrentPos);
                    }
                    //回退到上次请求的位置和状态
                    mCurrentPos = mPrevPos;
                    mAdCount--;
                    return;
                }
                mAdCount = 0;//请求到广告了，则本地计数清零
                if (Consts.logEnabled()) {
                    Log.d(TAG, "find media,notify,media=" + media);
                }
                notifyOuterListener(media);
                return;
            }


            runFromCurrent();
        }
    };

    private void notifyInnerListener(IFSMedia media) {
        if (null != mInnerAdListener) {
            mInnerAdListener.onNotify(media);
        }
    }

    private void notifyOuterListener(IFSMedia media) {
        if (Consts.logEnabled()) {
            Log.d(TAG, "mOuterAdListener=" + mOuterAdListener + " media=" + media);
        }
        if (null != mOuterAdListener) {
            mOuterAdListener.onNotify(media);
        }
    }

    public void setPreMode(boolean preMode) {
        mPreMode = preMode;
    }

    //是否预加载广告模式：没有对外的回调，可以认为是预加载模式
    public boolean isPreloadMode() {
        return mPreMode;
    }
}
