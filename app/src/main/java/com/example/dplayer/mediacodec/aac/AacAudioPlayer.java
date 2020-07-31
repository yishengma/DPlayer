package com.example.dplayer.mediacodec.aac;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Policy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AacAudioPlayer {

    private AudioTrack mAudioTrack;
    private MediaCodec mAudioDecoder;

    private MediaExtractor mMediaExtractor;
    private int mStreamType = AudioManager.STREAM_MUSIC;
    private int mSampleRate = 44100;
    private int mChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mMode = AudioTrack.MODE_STREAM;
    private int mMinBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);
    private volatile boolean mIsPlaying = false;
    private ExecutorService mExecutorService;

    public AacAudioPlayer() {
        mAudioTrack = new AudioTrack(mStreamType, mSampleRate, mChannelConfig, mAudioFormat, mMinBufferSize, mMode);
        mMediaExtractor = new MediaExtractor();
        mExecutorService = Executors.newFixedThreadPool(1);
    }


    public void play(String filePath) {
        try {
            mMediaExtractor.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(0);
        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        if (TextUtils.isEmpty(mimeType)) {
            return;
        }
        assert mimeType != null;
        if (mimeType.startsWith("audio")) {
            mMediaExtractor.selectTrack(0);
            mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 0);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, 0);
            try {
                mAudioDecoder = MediaCodec.createDecoderByType(mimeType);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAudioDecoder.configure(mediaFormat, null, null, 0);
        }
        mAudioDecoder.start();
        mAudioTrack.play();
        mExecutorService.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                decode();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void decode() {
        MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer inputBuffer;
        mIsPlaying = true;
        while (mIsPlaying) {
            int inputIndex = mAudioDecoder.dequeueInputBuffer(10_000);
            if (inputIndex < 0) {
                mIsPlaying = false;
                continue;
            }
            inputBuffer = mAudioDecoder.getInputBuffer(inputIndex);
            inputBuffer.clear();
            int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize > 0) {
                mAudioDecoder.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);
                mMediaExtractor.advance();
            } else {
                mIsPlaying = false;
            }
            int outputIndex = mAudioDecoder.dequeueOutputBuffer(decodeBufferInfo, 0);
            ByteBuffer outputBuffer;
            byte[] buffer;
            while (outputIndex >= 0) {
                outputBuffer = mAudioDecoder.getOutputBuffer(outputIndex);
                if (outputBuffer == null) {
                    break;
                }
                buffer = new byte[decodeBufferInfo.size];
                outputBuffer.get(buffer);
                outputBuffer.clear();
                mAudioTrack.write(buffer, 0, decodeBufferInfo.size);
                mAudioDecoder.releaseOutputBuffer(outputIndex, false);
                outputIndex = mAudioDecoder.dequeueOutputBuffer(decodeBufferInfo, 0);
            }
        }
        mIsPlaying = false;
        mAudioDecoder.stop();
        mAudioDecoder.release();
        mAudioDecoder = null;
        Log.e("AAA","play stop");
    }

}
