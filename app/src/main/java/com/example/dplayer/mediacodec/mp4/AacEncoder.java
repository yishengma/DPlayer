package com.example.dplayer.mediacodec.mp4;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.example.dplayer.mediacodec.AVMediaMuxer;

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
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";//就是 aac

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
            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mMediaFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRateInHz, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);
            mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);//CHANNEL_IN_STEREO 立体声
            int bitRate = sampleRateInHz * 16 * channelConfig == AudioFormat.CHANNEL_IN_MONO ? 1 : 2;
            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig == AudioFormat.CHANNEL_IN_MONO ? 1 : 2);
            mMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRateInHz);
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
                mAudioEncoder.queueInputBuffer(inputIndex, 0, pcmData.length, pts, 0);
            }

            outputIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 10_000);

            if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mAudioEncoder.getOutputBuffers();
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mAudioEncoder.getOutputFormat();
                if (null != mCallback) {
                    mCallback.outputMediaFormat(AAC_ENCODER, newFormat);
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
