package com.example.dplayer.opengles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

//
// Created by 海盗的帽子 on 2020/6/14.
//
public class DPlayerSurface extends GLSurfaceView {
    public DPlayerSurface(Context context) {
        this(context, null);
    }

    public DPlayerSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
    }
}
