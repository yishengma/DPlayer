package com.example.dplayer.mediacodec.mp4;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AacEncoder {
    public static final int AAC_ENCODER = 2;
    private MediaCodec mAudioEncoder;
    private MediaFormat mMediaFormat;
    private BlockingQueue<byte[]> mDataQueue;
    private volatile boolean mIsEncoding;
    private Callback mCallback;
    private long mPresentationTimeUs;
    private boolean mFirstFrame = true;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void outputMediaFormat(int type, MediaFormat mediaFormat);

        void onEncodeOutput(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

        void onStop(int type);
    }

    public AacEncoder(int sampleRateInHz, int channelConfig, int bufferSizeInBytes) {
        try {
            mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mMediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);
            //声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//64000, 96000, 128000
            mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSizeInBytes);
            mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);
            mAudioEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {

        }
        mDataQueue = new ArrayBlockingQueue<>(10);
        mIsEncoding = false;
    }

    public void start() {
        if (mIsEncoding) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                onStart();
            }
        }).start();
    }

    public void stop() {
        mIsEncoding = false;
    }

    private void onStart() {
        mPresentationTimeUs = System.currentTimeMillis() * 1000;
        mIsEncoding = true;
        mAudioEncoder.start();
        byte[] pcmData;
        int inputIndex;
        ByteBuffer inputBuffer;
        ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();

        int outputIndex;
        ByteBuffer outputBuffer;
        ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (mIsEncoding || !mDataQueue.isEmpty()) {
            pcmData = dequeueData();
            if (pcmData == null) {
                continue;
            }
            long pts = System.currentTimeMillis() * 1000 - mPresentationTimeUs;
            inputIndex = mAudioEncoder.dequeueInputBuffer(10_000);
            if (inputIndex >= 0) {
                inputBuffer = inputBuffers[inputIndex];
                inputBuffer.clear();
                inputBuffer.limit(pcmData.length);
                inputBuffer.put(pcmData);
                if (mFirstFrame) {
                    pts = 0;
                    mFirstFrame = false;
                }
                mAudioEncoder.queueInputBuffer(inputIndex, 0, pcmData.length, pts, 0);
            }

            outputIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 10_000);

            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mAudioEncoder.getOutputFormat();
                if (mCallback != null) {
                    mCallback.outputMediaFormat(AAC_ENCODER, format);
                }
            }
            while (outputIndex >= 0) {
                outputBuffer = outputBuffers[outputIndex];
                if (mCallback != null) {
                    mCallback.onEncodeOutput(outputBuffer, bufferInfo);
                }
                mAudioEncoder.releaseOutputBuffer(outputIndex, false);
                outputIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 10_000);
            }
        }
        mAudioEncoder.stop();
        mAudioEncoder.release();
        mAudioEncoder = null;
        if (mCallback != null) {
            mCallback.onStop(AAC_ENCODER);
        }
    }

    private byte[] dequeueData() {
        if (mDataQueue.isEmpty()) {
            return null;
        }
        try {
            return mDataQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void queueData(byte[] data) {
        if (!mIsEncoding) {
            return;
        }
        try {
            mDataQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
