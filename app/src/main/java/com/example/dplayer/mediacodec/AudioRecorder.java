package com.example.dplayer.mediacodec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.IOException;

public class AudioRecorder {

    private static AudioRecorder mAudioRecorder;

    private AudioRecord mAudioRecord;
    private int mChannelCount;
    private int mSampleRate;
    private int mPcmFormat;
    private byte[] mAudioBuf;

    private Thread mWorkThread;
    private volatile boolean mLoop = false;
    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void onAudioData(byte[] data);
    }

    public static AudioRecorder getInstance() {
        if (mAudioRecorder == null) {
            synchronized (AudioRecorder.class) {
                if (mAudioRecorder == null) {
                    mAudioRecorder = new AudioRecorder();
                }
            }
        }
        return mAudioRecorder;
    }

    public void prepareAudioRecord() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        int[] sampleRates = {44100, 22050, 16000, 11025, 8000, 4000};
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            for (int sampleRate : sampleRates) {
                int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
                int minBufferSize = 2 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    mAudioRecord = null;
                    continue;
                }
                mSampleRate = sampleRate;
                mChannelCount = channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2 : 1;
                mPcmFormat = 16;
                int bufferSize = Math.min(4096, minBufferSize);
                mAudioBuf = new byte[bufferSize];
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRecord() {
        if (mLoop) {
            return;
        }
        mWorkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mAudioRecord != null) {
                    mAudioRecord.startRecording();
                }
                while (mLoop && !Thread.interrupted()) {
                    int size = mAudioRecord.read(mAudioBuf, 0, mAudioBuf.length);
                    if (size > 0) {
                        if (mCallback != null) {
                            mCallback.onAudioData(mAudioBuf);
                        }
                    }
                }
            }
        });
        mLoop = true;
        mWorkThread.start();
    }

    public void stopRecord() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
        mLoop = false;
        if (mWorkThread != null) {
            mWorkThread.interrupt();
        }
    }

    public void release() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
        }
        mAudioRecord = null;
        mCallback = null;
        mAudioRecorder = null;
    }

    public int getChannelCount() {
        return mChannelCount;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getPcmFormat() {
        return mPcmFormat;
    }
}
