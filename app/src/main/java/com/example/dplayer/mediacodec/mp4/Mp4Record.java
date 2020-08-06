package com.example.dplayer.mediacodec.mp4;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.example.dplayer.mediacodec.mp4.AacEncoder.AAC_ENCODER;
import static com.example.dplayer.mediacodec.mp4.H264Encoder.H264_ENCODER;

public class Mp4Record implements H264VideoRecord.Callback, AacAudioRecord.Callback {

    private H264VideoRecord mH264VideoRecord;
    private AacAudioRecord mAacAudioRecord;
    private MediaMuxer mMediaMuxer;

    private boolean mHasStartMuxer;
    private boolean mHasStopAudio;
    private boolean mHasStopVideo;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private final Object mLock;

    public Mp4Record(Activity activity, SurfaceView surfaceView, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, String path) {
        mH264VideoRecord = new H264VideoRecord(activity, surfaceView);
        mAacAudioRecord = new AacAudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mH264VideoRecord.setCallback(this);
        mAacAudioRecord.setCallback(this);
        try {
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mHasStartMuxer = false;
        mLock = new Object();
    }

    public void start() {
        mAacAudioRecord.start();
        mH264VideoRecord.start();
    }

    public void stop() {
        mAacAudioRecord.stop();
        mH264VideoRecord.stop();
    }


    @Override
    public void outputAudio(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        Log.i("eee","outputAudio:"+mAudioTrackIndex);
        writeMediaData(mAudioTrackIndex, byteBuffer, bufferInfo);
    }

    @Override
    public void outputMediaFormat(int type, MediaFormat mediaFormat) {
        checkMediaFormat(type, mediaFormat);
    }

    @Override
    public void outputVideo(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        Log.i("eee","outputVideo:"+mVideoTrackIndex);
        writeMediaData(mVideoTrackIndex, byteBuffer, bufferInfo);
    }

    private void writeMediaData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        synchronized (mLock) {
            if (!mHasStartMuxer) {
                return;
            }
            mMediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
        }
    }

    private void checkMediaFormat(int type, MediaFormat mediaFormat) {
        synchronized (mLock) {
            if (type == H264_ENCODER) {
                mVideoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
            }
            if (type == AAC_ENCODER) {
                mAudioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
            }
            startMediaMuxer();
        }
    }

    private void startMediaMuxer() {
        if (mHasStartMuxer) {
            return;
        }
        if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1) {
            mMediaMuxer.start();
            mHasStartMuxer = true;
        }
    }

    @Override
    public void onStop(int type) {
        synchronized (mLock) {
            if (type == H264_ENCODER) {
                mHasStopVideo = true;
            }
            if (type == AAC_ENCODER) {
                mHasStopAudio = true;
            }
            if (mHasStopAudio && mHasStopVideo && mHasStartMuxer) {
                mHasStartMuxer = false;
                mMediaMuxer.stop();
            }
        }
    }
}
