package com.example.dplayer.mediacodec.mp4;


import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public class AacAudioRecord implements AudioRecorder.Callback, AacEncoder.Callback {
    private AudioRecorder mAudioRecord;
    private AacEncoder mAacEncoder;
    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void outputMediaFormat(int type, MediaFormat mediaFormat);

        void outputAudio(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

        void onStop(int type);
    }

    public AacAudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        mAudioRecord = new AudioRecorder(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mAacEncoder = new AacEncoder(sampleRateInHz, channelConfig, bufferSizeInBytes);
        mAudioRecord.setCallback(this);
        mAacEncoder.setCallback(this);
    }

    public void start() {
        mAudioRecord.start();
        mAacEncoder.start();
    }

    public void stop() {
        mAudioRecord.stop();
        mAacEncoder.stop();
    }

    @Override
    public void onAudioOutput(byte[] data) {
        mAacEncoder.queueData(data);
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
        mCallback.outputAudio(byteBuffer, bufferInfo);
    }

    @Override
    public void onStop(int type) {
        if (mCallback == null) {
            return;
        }
        mCallback.onStop(type);
    }
}
