package com.example.dplayer.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class DAudioTrack {
    private AudioTrack mAudioTrack;
    public static final int DEFAULT_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    public static final int DEFAULT_SAMPLE_RATE_IN_HZ = 16_000;
    public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_BUFFER_SIZE_IN_BYTES = 1024;
    public static final int DEFAULT_MODE = AudioTrack.MODE_STREAM;

    private int mStreamType;
    private int mSampleRateInHz;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSizeInBytes;
    private int mMode;

    protected DAudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) {
        mStreamType = streamType;
        mSampleRateInHz = sampleRateInHz;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mBufferSizeInBytes = bufferSizeInBytes;
        mMode = mode;
        mAudioTrack = new AudioTrack(mStreamType, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes, mMode);
    }

    public void play() {
        mAudioTrack.play();
    }

    public void write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
    }

    public void stop() {
        mAudioTrack.stop();
    }

    public int getBufferSize() {
        return AudioTrack.getMinBufferSize(mSampleRateInHz, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }


    public static final class Builder {
        int mStreamType;
        int mSampleRateInHz;
        int mChannelConfig;
        int mAudioFormat;
        int mBufferSizeInBytes;
        int mMode;

        public Builder() {
            this(DEFAULT_STREAM_TYPE, DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT, DEFAULT_BUFFER_SIZE_IN_BYTES, DEFAULT_MODE);
        }

        public Builder(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) {
            mStreamType = streamType;
            mSampleRateInHz = sampleRateInHz;
            mChannelConfig = channelConfig;
            mAudioFormat = audioFormat;
            mBufferSizeInBytes = bufferSizeInBytes;
            mMode = mode;
        }

        public Builder setStreamType(int streamType) {
            mStreamType = streamType;
            return this;
        }

        public Builder setSampleRateInHz(int sampleRateInHz) {
            mSampleRateInHz = sampleRateInHz;
            return this;
        }

        public Builder setChannelConfig(int channelConfig) {
            mChannelConfig = channelConfig;
            return this;
        }

        public Builder setAudioFormat(int audioFormat) {
            mAudioFormat = audioFormat;
            return this;
        }

        public Builder setBufferSizeInBytes(int bufferSizeInBytes) {
            mBufferSizeInBytes = bufferSizeInBytes;
            return this;
        }

        public Builder setMode(int mode) {
            mMode = mode;
            return this;
        }

        public DAudioTrack build() {
            return new DAudioTrack(
                    mStreamType,
                    mSampleRateInHz,
                    mChannelConfig,
                    mAudioFormat,
                    mBufferSizeInBytes,
                    mMode
            );
        }
    }
}
