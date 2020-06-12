package com.example.dplayer;

import android.media.AudioTrack;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Text;

//
// Created by 海盗的帽子 on 2020/6/9.
//

//OpenSLES 和 OpenGLES 是播放音频的
//OpenSLES 是 OpenSL 的精简版本
//OpenGLES 是 OpenGL 的精简版本
public class DPlayer {
    private static final String TAG = "DPlayer";

    static {
        System.loadLibrary("native-lib");
    }

    public interface OnErrorListener {
        void onError(int code, String msg);
    }

    public interface OnPrepareListener {
        void onPrepared();
    }

    private String mUrl;
    private OnErrorListener mOnErrorListener;
    private OnPrepareListener mOnPrepareListener;

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    public void setOnPrepareListener(OnPrepareListener onPrepareListener) {
        mOnPrepareListener = onPrepareListener;
    }

    public void setDataSource(String url) {
        this.mUrl = url;
    }

    public void prepare() {
        if (TextUtils.isEmpty(mUrl)) {
            return;
        }
        nativePrepare(mUrl);
    }

    public void prepareAsync() {
        if (TextUtils.isEmpty(mUrl)) {
            return;
        }
        nativePrepareAsync(mUrl);
    }

    public void play() {
        nativePlay();
    }

    public void release() {
        nativeRelease();
    }

    private void onError(int code, String msg) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(code, msg);
        }
    }

    private void onPrepared() {
        if (mOnPrepareListener != null) {
            mOnPrepareListener.onPrepared();
        }
    }

    private native void nativePrepare(String url);

    private native void nativePrepareAsync(String url);

    private native void nativePlay();

    private native void nativeRelease();


}
