package com.idreems.openvmAd.Push;

import java.util.ArrayList;

/**
 * Created by ramonqlee on 6/17/16.
 */
public class PushDispatcher {
    private static PushDispatcher sPushDispatcher = new PushDispatcher();
    private ArrayList<PushObserver> mPushObserverList = new ArrayList<>();

    public static PushDispatcher sharedInstance() {
        return sPushDispatcher;
    }

    public PushDispatcher() {
    }

    public void addObserver(PushObserver observer) {
        if (null == observer || -1 != mPushObserverList.indexOf(observer)) {
            return;
        }
        mPushObserverList.add(observer);
    }

    public void removeObserver(PushObserver observer) {
        if (null == observer) {
            return;
        }
        mPushObserverList.remove(observer);
    }

    public void clearObserver() {
        mPushObserverList.clear();
    }

    public void dispatch(String message) {
        for (PushObserver observer : mPushObserverList) {
            if (null == observer) {
                continue;
            }
            observer.onMessage(message);
        }
    }
}
