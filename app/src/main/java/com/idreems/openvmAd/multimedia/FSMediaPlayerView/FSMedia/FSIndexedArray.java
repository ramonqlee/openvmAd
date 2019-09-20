package com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 5/19/16.
 */
public class FSIndexedArray<E> {
    private ArrayList<E> mObjArray = new ArrayList<>();
    private int mPosition;

    public void setmPosition(int mPosition) {
        this.mPosition = mPosition;
    }

    public void add(E e) {
        mObjArray.add(e);
    }

    public void addAll(List<E> list) {
        if (null == list) {
            return;
        }
        mObjArray.addAll(list);
    }

    public void updateAll(List<E> list) {
        if (null == list) {
            return;
        }
        mObjArray.clear();
        mObjArray.addAll(list);
    }

    public void remove(E e) {
        mObjArray.remove(e);
    }

    public List<E> toList() {
        return new ArrayList<>(mObjArray);
    }

    public E get(int pos) {
        if (pos >= mObjArray.size()) {
            return null;
        }
        return mObjArray.get(pos);
    }

    public E getCurrent() {
        if (null == mObjArray || mObjArray.isEmpty() || mPosition >= mObjArray.size()) {
            return null;
        }
        return mObjArray.get(mPosition);
    }

    public E getNext() {
        if (null == mObjArray || mObjArray.isEmpty()) {
            return null;
        }
        if (mPosition >= mObjArray.size() - 1) {
            return mObjArray.get(0);
        }
        return mObjArray.get(mPosition + 1);
    }

    public int position() {
        return mPosition;
    }

    public void increment() {
        if (null == mObjArray || 0 == mObjArray.size()) {
            return;
        }
        mPosition = (++mPosition) % mObjArray.size();
    }

    public void decrement() {
        mPosition = (--mPosition < 0) ? 0 : mPosition;
    }

    public boolean seekToPosition(E e) {
        if (null == e) {
            return false;
        }

        for (int i = 0; i < mObjArray.size(); ++i) {
            if (mObjArray.get(i).equals(e)) {
                mPosition = i;
                return true;
            }
        }
        return false;
    }

    public int size() {
        return mObjArray.size();
    }

    public void clear() {
        mPosition = 0;
        mObjArray.clear();
    }
}
