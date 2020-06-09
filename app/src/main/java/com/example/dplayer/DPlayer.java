package com.example.dplayer;

//
// Created by 海盗的帽子 on 2020/6/9.
//
public class DPlayer {
    public void play() {
        nativePlay();
    }

    private native void nativePlay();
}
