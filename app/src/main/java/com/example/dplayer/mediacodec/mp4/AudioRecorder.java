package com.example.dplayer.mediacodec.mp4;

import android.media.AudioFormat;
import android.media.AudioRecord;

public class AudioRecorder {

    private int mAudioSource;
    private int mSampleRateInHz;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSizeInBytes;

    private AudioRecord mAudioRecord;
    private volatile boolean mIsRecording;
    private Callback mCallback;
    private byte[] mBuffer;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void onAudioOutput(byte[] data);
    }

    public AudioRecorder(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        mAudioSource = audioSource;
        mSampleRateInHz = sampleRateInHz;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mBufferSizeInBytes = bufferSizeInBytes;

        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mIsRecording = false;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

        mBuffer = new byte[Math.min(2048, minBufferSize)];
    }

    public void start() {
        if (mIsRecording) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                onStart();
            }
        }).start();
    }

    public void onStart() {
        if (mAudioRecord == null) {
            mAudioRecord = new android.media.AudioRecord(mAudioSource, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes);
        }
        mAudioRecord.startRecording();
        mIsRecording = true;
        while (mIsRecording) {
            int len = mAudioRecord.read(mBuffer, 0, mBuffer.length);
            if (len > 0) {
                if (mCallback != null) {
                    mCallback.onAudioOutput(mBuffer);
                }
            }
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
    }

    public void stop() {
        mIsRecording = false;
    }
}
