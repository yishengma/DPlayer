package com.example.dplayer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.xml.transform.Source;

//
// Created by 海盗的帽子 on 2020/6/13.
//
public class DVideoView extends SurfaceView implements DPlayer.OnPrepareListener {
    private DPlayer mDPlayer;
    //    private volatile boolean hasCreatedSurface;
    private SurfaceHolder mSurfaceHolder;

    public DVideoView(Context context) {
        this(context, null);
    }

    public DVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mDPlayer = new DPlayer();
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
        mDPlayer.setOnPrepareListener(this);
//        hasCreatedSurface = false;
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.e("videoView", "surfaceCreated");
                mSurfaceHolder = holder;
//                hasCreatedSurface = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    public void setDataSource(final String url) {
//        mDPlayer.setDataSource(url);
//        mDPlayer.prepareAsync();
        //需要等 SurfaceHolder 创建完成
//        if (!hasCreatedSurface) {
//            postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Log.e("VideoView",""+mSurfaceHolder.getSurface());
//                    setDataSource(url);
//                }
//            },1000);
//        }else {
//            Surface surface = mSurfaceHolder.getSurface();
//            if (surface == null) {
//                Log.e("VideoView","surface == NULL");
//            }
//            Log.e("videoView","decodeVideo");
//            decodeVideo(url,surface);
//        }
//
//    }
        decodeVideo(url, mSurfaceHolder.getSurface());
    }

    @Override
    public void onPrepared() {
        mDPlayer.play();
    }

    private native void decodeVideo(String url, Surface surface);
}
