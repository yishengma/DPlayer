package com.example.dplayer;

import android.media.AudioTrack;
import android.util.Log;

//
// Created by 海盗的帽子 on 2020/6/9.
//
public class DPlayer {
    public interface ErrorListener {
        void onError(int code, String msg);
    }

    private static final String TAG = "DPlayer";
    private ErrorListener errorListener;

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void play() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                nativePlay();
            }
        }).start();
    }

    private native void nativePlay();

    private void onError(int code, String msg) {
        if (errorListener != null) {
            errorListener.onError(code,msg);
        }
    }

    ;
}
