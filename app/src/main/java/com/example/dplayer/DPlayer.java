package com.example.dplayer;

import android.media.AudioTrack;
import android.util.Log;

//
// Created by 海盗的帽子 on 2020/6/9.
//

//OpenSLES 和 OpenGLES 是播放音频的
//OpenSLES 是 OpenSL 的精简版本
//OpenGLES 是 OpenGL 的精简版本
public class DPlayer {
    public interface ErrorListener {
        void onError(int code, String msg);
    }

    private static final String TAG = "DPlayer";
    private ErrorListener errorListener;

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void setDataSource(String url) {
        nativeSetDataSource(url);
    }

    public void prepare() {
        nativePrepare();
    }

    public void prepareAsync() {
        nativePrepareAsync();
    }

    public void play() {
        nativePlay();
    }

    public void release() {
        nativeRelease();
    }

    private native void nativePrepare();
    private native void nativePrepareAsync();
    private native void nativePlay();
    private native void nativeRelease();
    private native void nativeSetDataSource(String url);


    private void onError(int code, String msg) {
        if (errorListener != null) {
            errorListener.onError(code, msg);
        }
    };
}
