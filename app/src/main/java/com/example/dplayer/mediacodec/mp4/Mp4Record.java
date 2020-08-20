package com.example.dplayer.mediacodec.mp4;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.example.dplayer.mediacodec.mp4.AacEncoder.AAC_ENCODER;
import static com.example.dplayer.mediacodec.mp4.H264Encoder.H264_ENCODER;

public class Mp4Record implements H264VideoRecord.Callback, AacAudioRecord.Callback {
    private static int index = 0;
    private H264VideoRecord mH264VideoRecord;
    private AacAudioRecord mAacAudioRecord;
    private MediaMuxer mMediaMuxer;

    private boolean mHasStartMuxer;
    private boolean mHasStopAudio;
    private boolean mHasStopVideo;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private final Object mLock;
    private BlockingQueue<AVData> mDataBlockingQueue;
    private volatile boolean mIsRecoding;

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
        mDataBlockingQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        mIsRecoding = true;
        mAacAudioRecord.start();
        mH264VideoRecord.start();
    }

    public void stop() {
        mAacAudioRecord.stop();
        mH264VideoRecord.stop();
        mIsRecoding = false;
    }


    @Override
    public void outputAudio(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        writeMediaData(mAudioTrackIndex, byteBuffer, bufferInfo);
    }

    @Override
    public void outputMediaFormat(int type, MediaFormat mediaFormat) {
        checkMediaFormat(type, mediaFormat);
    }

    @Override
    public void outputVideo(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        writeMediaData(mVideoTrackIndex, byteBuffer, bufferInfo);
    }

    private void writeMediaData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        mDataBlockingQueue.add(new AVData(index++, trackIndex, byteBuffer, bufferInfo));
    }

    private void checkMediaFormat(int type, MediaFormat mediaFormat) {
        synchronized (mLock) {
            if (type == AAC_ENCODER) {
                mAudioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
            }
            if (type == H264_ENCODER) {
                mVideoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
            }
            startMediaMuxer();
        }
    }

    private void startMediaMuxer() {
        if (mHasStartMuxer) {
            return;
        }
        if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1) {
            Log.e("ethan", "video track index:" + mVideoTrackIndex + "audio track index:" + mAudioTrackIndex);
            mMediaMuxer.start();
            mHasStartMuxer = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mIsRecoding || !mDataBlockingQueue.isEmpty()) {
                        AVData avData = mDataBlockingQueue.poll();
                        if (avData == null) {
                            continue;
                        }
                        boolean keyFrame = (avData.bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                        Log.e("ethan", avData.index + "trackIndex:" + avData.trackIndex + ",writeSampleData:" + keyFrame);
                        mMediaMuxer.writeSampleData(avData.trackIndex, avData.byteBuffer, avData.bufferInfo);
                    }
                }
            }).start();
            mLock.notifyAll();
        } else {
            try {
                mLock.wait();
            } catch (InterruptedException e) {

            }
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

    private class AVData {
        int index = 0;
        int trackIndex;
        ByteBuffer byteBuffer;
        MediaCodec.BufferInfo bufferInfo;

        public AVData(int index, int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
            this.index = index;
            this.trackIndex = trackIndex;
            this.byteBuffer = byteBuffer;
            this.bufferInfo = bufferInfo;
            boolean keyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
            Log.e("ethan", index + "trackIndex:" + trackIndex + ",AVData:" + keyFrame);
        }
    }
}
