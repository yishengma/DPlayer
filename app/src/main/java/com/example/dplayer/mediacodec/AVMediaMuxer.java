package com.example.dplayer.mediacodec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AVMediaMuxer {
    public static final int VIDEO_TRACK = 0;
    public static final int AUDIO_TRACK = 1;

    private MediaMuxer mMediaMuxer;
    private LinkedBlockingQueue<MuxerData> mMuxerQueue = new LinkedBlockingQueue<>();
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private boolean mIsVideoAdd;
    private boolean mIsAudioAdd;
    private Thread mWorkThread;
    private VideoRecorder mVideoRecorder;
    private AudioRecorder mAudioRecorder;
    private AVEncoder mAVEncoder;
    private boolean mIsMediaMuxerStart;
    private volatile boolean mLoop;
    private Object mLock = new Object();

    private static AVMediaMuxer instance;

    public static AVMediaMuxer getInstance() {
        if (instance == null) {
            instance = new AVMediaMuxer();
        }
        return instance;
    }

    public void initMediaMuxer(String outfile) {
        Log.e("ethan",""+outfile);
        if (mLoop) {
            return;
        }
        try {
            mMediaMuxer = new MediaMuxer(outfile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mVideoRecorder = VideoRecorder.getInstance();
        mAudioRecorder = AudioRecorder.getInstance();

        mAVEncoder = AVEncoder.getInstance();
        setListener();
        mWorkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    while (mLoop && !Thread.interrupted()) {
                        MuxerData data = mMuxerQueue.take();
                        int track = -1;
                        if (data.trackIndex == VIDEO_TRACK) {
                            track = videoTrackIndex;
                        }
                        if (data.trackIndex == AUDIO_TRACK) {
                            track = audioTrackIndex;
                        }
                        mMediaMuxer.writeSampleData(track, data.byteBuffer, data.bufferInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mMuxerQueue.clear();
                stopMediaMuxer();
            }
        });
        mLoop = true;
        mWorkThread.start();
    }

    public void initVideoEncoder(int width, int height, int fps) {
        mAVEncoder.initVideoEncoder(width, height, fps);
    }

    public void initAudioEncoder() {
        mAVEncoder.initAudioEncoder(mAudioRecorder.getSampleRate(), mAudioRecorder.getPcmFormat(), mAudioRecorder.getChannelCount());
    }

    public void startAudioRecord() {
        mAudioRecorder.prepareAudioRecord();
        mAudioRecorder.startRecord();
    }

    public void stopAudioRecord() {
        mAudioRecorder.stopRecord();
    }

    public void release() {
        mAudioRecorder.release();
        mAudioRecorder = null;
        mLoop = false;
        if (mWorkThread != null) {
            mWorkThread.interrupt();
        }
        mVideoRecorder = null;
        mAVEncoder = null;
    }

    private void setListener() {
        mVideoRecorder.setCallback(new VideoRecorder.Callback() {
            @Override
            public void onVideoData(byte[] data) {
                if (mAVEncoder != null) {
                    mAVEncoder.putVideoData(data);
                }
            }
        });
        mAudioRecorder.setCallback(new AudioRecorder.Callback() {
            @Override
            public void onAudioData(byte[] data) {
                if (mAVEncoder != null) {
                    mAVEncoder.putAudioData(data);
                }
            }
        });

        mAVEncoder.setCallback(new AVEncoder.Callback() {
            @Override
            public void outputVideoFrame(int trackIndex, ByteBuffer outBuffer, MediaCodec.BufferInfo bufferInfo) {
                try {
                    mMuxerQueue.put(new MuxerData(trackIndex, outBuffer, bufferInfo));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void outputAudioFrame(int trackIndex, ByteBuffer outBuffer, MediaCodec.BufferInfo bufferInfo) {
                try {
                    mMuxerQueue.put(new MuxerData(trackIndex, outBuffer, bufferInfo));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void outMediaFormat(int trackIndex, MediaFormat mediaFormat) {
                if (trackIndex == AUDIO_TRACK) {
                    if (mMediaMuxer != null) {
                        audioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
                        mIsAudioAdd = true;
                    }
                }
                if (trackIndex == VIDEO_TRACK) {
                    if (mMediaMuxer != null) {
                        videoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
                        mIsVideoAdd = true;
                    }
                }
                startMediaMuxer();
            }
        });
    }


    public void startMediaMuxer() {
        if (mIsMediaMuxerStart) {
            return;
        }
        synchronized (mLock) {
            Log.i("ethan", "startMediaMuxer"+mIsAudioAdd+mIsVideoAdd);
            if (mIsAudioAdd && mIsVideoAdd) {
                mMediaMuxer.start();
                mIsMediaMuxerStart = true;
                mLock.notify();
            }
        }
    }

    public void stopMediaMuxer() {
        if (!mIsMediaMuxerStart) {
            return;
        }
        mMediaMuxer.stop();
        mMediaMuxer.release();
        mIsMediaMuxerStart = false;
        mIsAudioAdd = false;
        mIsVideoAdd = false;
    }

    public void startEncoder() {
        Log.i("ethan", "startEncoder");
        mAVEncoder.start();
    }

    public void stopEncoder() {
        mAVEncoder.stop();
    }

    static class MuxerData {
        int trackIndex = 0;
        ByteBuffer byteBuffer;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuffer = byteBuffer;
            this.bufferInfo = bufferInfo;
        }
    }
}
