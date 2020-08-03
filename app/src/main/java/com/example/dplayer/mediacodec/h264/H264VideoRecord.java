package com.example.dplayer.mediacodec.h264;

import android.app.Activity;
import android.media.MediaMuxer;
import android.view.SurfaceView;

import java.io.IOException;

public class H264VideoRecord {
    private CameraHelper mCameraHelper;
    private H264Encoder mH264Encoder;

    public H264VideoRecord(Activity activity, String filePath, SurfaceView surfaceView) {
        mCameraHelper = new CameraHelper(surfaceView, activity);
        mH264Encoder = new H264Encoder(mCameraHelper.mPreviewWidth, mCameraHelper.mPreviewHeight, mCameraHelper.mFrameRate);
        mH264Encoder.setFilePath(filePath);
        mCameraHelper.setPreviewCallback(new CameraHelper.PreviewCallback() {
            @Override
            public void onFrame(byte[] data) {
                mH264Encoder.putFrameData(data);
            }
        });
    }

    public void start() {
        mH264Encoder.start();
    }

    public void stop() {
        mH264Encoder.stop();
    }
}
