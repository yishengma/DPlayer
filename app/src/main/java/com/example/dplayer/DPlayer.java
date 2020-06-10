package com.example.dplayer;

import android.media.AudioTrack;

//
// Created by 海盗的帽子 on 2020/6/9.
//
public class DPlayer {

    public void play() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                nativePlay();
            }
        }).start();
    }

    private native void nativePlay();
}
