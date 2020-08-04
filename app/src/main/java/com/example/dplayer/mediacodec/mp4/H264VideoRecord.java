package com.example.dplayer.mediacodec.mp4;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceView;

import java.nio.ByteBuffer;

public class H264VideoRecord implements CameraHelper.PreviewCallback, H264Encoder.Callback {

    private CameraHelper mCameraHelper;
    private H264Encoder mH264Encoder;

    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void outputMediaFormat(int type, MediaFormat mediaFormat);

        void outputVideo(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

        void onStop(int type);
    }

    public H264VideoRecord(Activity activity, SurfaceView surfaceView) {
        mCameraHelper = new CameraHelper(surfaceView, activity);
        mH264Encoder = new H264Encoder(mCameraHelper.mPreviewWidth, mCameraHelper.mPreviewHeight, mCameraHelper.mFrameRate);
        mCameraHelper.setPreviewCallback(this);
        mH264Encoder.setCallback(this);
    }

    public void start() {
        mH264Encoder.start();
    }

    public void stop() {
        mH264Encoder.stop();
        mCameraHelper.stop();
    }

    @Override
    public void onFrame(byte[] data) {
        mH264Encoder.queueData(data);
    }

    @Override
    public void outputMediaFormat(int type, MediaFormat mediaFormat) {
        if (mCallback == null) {
            return;
        }
        mCallback.outputMediaFormat(type, mediaFormat);
    }

    @Override
    public void onEncodeOutput(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (mCallback == null) {
            return;
        }
        mCallback.outputVideo(byteBuffer, bufferInfo);
    }

    @Override
    public void onStop(int type) {
        if (mCallback == null) {
            return;
        }
        mCallback.onStop(type);
    }
}
