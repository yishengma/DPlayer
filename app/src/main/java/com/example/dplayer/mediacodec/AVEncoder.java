package com.example.dplayer.mediacodec;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.example.dplayer.utils.YUVEngine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class AVEncoder {
    private static AVEncoder sInstance;
    //Video
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int I_FRAME_INTERVAL = 5;
    private byte[] yuvBuffer;
    private byte[] rotateYuvBuffer;

    private int[] outWidth = new int[1];
    private int[] outHeight = new int[1];

    private int mWidth;
    private int mHeight;
    private int mFps;

    private MediaCodec mVideoEncoder;
    private MediaFormat mVideoFormat;

    private int mColorFormat = 0;
    private MediaCodec.BufferInfo mVideoBufferInfo;

    private ArrayList<Integer> mSupportColorFormatList;
    private Thread mVideoEncoderThread;
    private volatile boolean mVideoEncoderLoop = false;
    private volatile boolean mVideoEncoderEnd = false;
    private LinkedBlockingQueue<byte[]> mVideoQueue;

    //Audio
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private MediaCodec mAudioEncoder;
    private MediaCodec.BufferInfo mAudioBufferInfo;
    private MediaCodecInfo mAudioCodecInfo;
    private MediaFormat mAudioFormat;
    private Thread mAudioEncoderThread;

    private volatile boolean mAudioEncoderLoop = false;
    private volatile boolean mAudioEncoderEnd = false;
    private LinkedBlockingQueue<byte[]> mAudioQueue;

    private long mPresentationTimeUs;
    private final int TIMEOUT_USEC = 10_000;
    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public static AVEncoder getInstance() {
        if (sInstance == null) {
            sInstance = new AVEncoder();
        }
        return sInstance;
    }

    public void initAudioEncoder(int sampleRate, int pcmFormat, int channelCount) {
        if (mAudioEncoder != null) {
            return;
        }
        mAudioBufferInfo = new MediaCodec.BufferInfo();
        mAudioQueue = new LinkedBlockingQueue<>();
        mAudioCodecInfo = selectCodec(AUDIO_MIME_TYPE);
        if (mAudioCodecInfo == null) {
            return;
        }
        mAudioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, channelCount);
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);//CHANNEL_IN_STEREO 立体声
        int bitRate = sampleRate * pcmFormat * channelCount;
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);

        try {
            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initVideoEncoder(int width, int height, int fps) {
        Log.i("eee","initVideoEncoder:w"+width+"h:"+height+"fps:"+fps);
        if (mVideoEncoder != null) {
            return;
        }
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mVideoQueue = new LinkedBlockingQueue<>();
        mSupportColorFormatList = new ArrayList<>();
        rotateYuvBuffer = new byte[YUVUtil.getYUVBuffer(width, height)];
        yuvBuffer = new byte[YUVUtil.getYUVBuffer(width, height)];
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        MediaCodecInfo codecInfo = selectCodec(VIDEO_MIME_TYPE);
        selectColorFormat(codecInfo, VIDEO_MIME_TYPE);
        for (int i = 0; i < mSupportColorFormatList.size(); i++) {
            if (isRecognizedFormat(mSupportColorFormatList.get(i))) {
                mColorFormat = mSupportColorFormatList.get(i);
                break;
            }
        }
        mVideoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mHeight, mWidth);//翻转方向
        int bitrate = (mWidth * mHeight * 3 / 2) * 8 * fps;
        //设置比特率,将编码比特率值设为bitrate
        mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        //设置帧率,将编码帧率设为Camera实际帧率mFps
        mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        //设置颜色格式
        mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        //设置关键帧的时间
        mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        try {
            mVideoEncoder = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        startAudioEncode();
        startVideoEncode();
    }

    private void startAudioEncode() {
        Log.i("ethan", "startAudioEncode");
        if (mAudioEncoder == null) {
            return;
        }
        if (mAudioEncoderLoop) {
            return;
        }
        mAudioEncoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mPresentationTimeUs = System.currentTimeMillis() * 1000;
                mAudioEncoderEnd = false;
                mAudioEncoder.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mAudioEncoder.start();

                while (mAudioEncoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = mAudioQueue.take();
                        encodeAudioData(data);
                    } catch (Exception e) {

                    }
                }
                if (mAudioEncoder != null) {
                    mAudioEncoder.stop();
                    mAudioEncoder.release();
                    mAudioEncoder = null;
                }
                mAudioQueue.clear();
            }
        });
        mAudioEncoderLoop = true;
        mAudioEncoderThread.start();
    }


    private void encodeAudioData(byte[] data) {
        Log.i("ethan", "encodeAudioData");
        ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
        int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            ByteBuffer byteBuffer = inputBuffers[inputBufferIndex];
            byteBuffer.clear();
            byteBuffer.put(data);

            long pts = System.currentTimeMillis() * 1000 - mPresentationTimeUs;
            if (mAudioEncoderEnd) {
                mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts, 0);
            }
        }
        ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();
        int outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mAudioBufferInfo, TIMEOUT_USEC);
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mAudioEncoder.getOutputBuffers();
        }
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat format = mAudioEncoder.getOutputFormat();
            if (mCallback != null && !mAudioEncoderEnd) {
                mCallback.outMediaFormat(AVMediaMuxer.AUDIO_TRACK, format);
            }
        }
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            if (outputBuffer == null) {
                throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                        " was null");
            }

            if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                mAudioBufferInfo.size = 0;
            }

            if (mAudioBufferInfo.size != 0) {
                // byte[] outData = new byte[mBufferInfo.size];
                // outputBuffer.get(outData);
                if (null != mCallback && !mAudioEncoderEnd) {
                    mCallback.outputAudioFrame(AVMediaMuxer.AUDIO_TRACK, outputBuffer, mAudioBufferInfo);
                }
            }
            mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mAudioBufferInfo, 0);
            if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mAudioEncoderLoop = false;
                mAudioEncoderThread.interrupt();
                return;
            }
        }
    }

    private void startVideoEncode() {
        Log.i("ethan", "startVideoEncode");
        if (mVideoEncoder == null) {
            return;
        }
        if (mVideoEncoderLoop) {
            return;
        }
        mVideoEncoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mPresentationTimeUs = System.currentTimeMillis() * 1000;
                mVideoEncoderEnd = false;
                mVideoEncoder.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mVideoEncoder.start();
                Log.i("ethan","mVideoEncoder.start();"+mVideoEncoderLoop);

                while (mVideoEncoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = mVideoQueue.take();
                        Log.i("ethan","mVideoEncoderLoop");
                        encodeVideoData(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (mVideoEncoder != null) {
                    mVideoEncoder.stop();
                    mVideoEncoder.release();
                    mVideoEncoder = null;
                }
                mVideoQueue.clear();
            }
        });
        mVideoEncoderLoop = true;
        mVideoEncoderThread.start();
    }

    private void encodeVideoData(byte[] data) {
        Log.i("ethan", "encodeVideoData");
//input为Camera预览格式NV21数据
        if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            //nv21格式转为nv12格式
            YUVEngine.Nv21ToNv12(data, yuvBuffer, mWidth, mHeight);
            YUVEngine.Nv12ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            //用于NV21格式转换为I420(YUV420P)格式
            YUVEngine.Nv21ToI420(data, yuvBuffer, mWidth, mHeight);
            YUVEngine.I420ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
            System.arraycopy(data, 0, yuvBuffer, 0, mWidth * mHeight * 3 / 2);
            YUVEngine.Nv21ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
            //用于NV21格式转换为YV12格式
            YUVEngine.Nv21ToYV12(data, yuvBuffer, mWidth, mHeight);
            YUVEngine.Yv12ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        }
        ByteBuffer[] inputBuffers = mVideoEncoder.getInputBuffers();
        int inputBufferIndex = mVideoEncoder.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(rotateYuvBuffer);
            long pts = System.currentTimeMillis() * 1000 - mPresentationTimeUs;
            if (mVideoEncoderEnd) {
                mVideoEncoder.queueInputBuffer(inputBufferIndex, 0, rotateYuvBuffer.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mVideoEncoder.queueInputBuffer(inputBufferIndex, 0, rotateYuvBuffer.length, pts, 0);
            }
        }
        ByteBuffer[] outputBuffers = mVideoEncoder.getOutputBuffers();
        int outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mVideoEncoder.getOutputBuffers();
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat newFormat = mVideoEncoder.getOutputFormat();
            if (null != mCallback && !mVideoEncoderEnd) {
                mCallback.outMediaFormat(AVMediaMuxer.VIDEO_TRACK, newFormat);
            }
        }
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            if (outputBuffer == null) {
                throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                        " was null");
            }

            if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                mVideoBufferInfo.size = 0;
            }

            if (mVideoBufferInfo.size != 0) {
                // byte[] outData = new byte[mBufferInfo.size];
                // outputBuffer.get(outData);
                if (null != mCallback && !mVideoEncoderEnd) {
                    mCallback.outputVideoFrame(AVMediaMuxer.VIDEO_TRACK, outputBuffer, mVideoBufferInfo);
                }
            }
            mVideoEncoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);
            if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mVideoEncoderLoop = false;
                mVideoEncoderThread.interrupt();
                return;
            }
        }
    }

    public void stop() {
        stopAudioEncode();
        stopVideoEncode();
    }

    private void stopVideoEncode() {
        mVideoEncoderEnd = true;
    }

    private void stopAudioEncode() {
        mAudioEncoderEnd = true;
    }

    public void putVideoData(byte[] data) {
        if (mVideoQueue == null) {
            return;
        }
        try {
            Log.i("ethan","putVideoData");
            mVideoQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void putAudioData(byte[] data) {
        if (mAudioQueue == null) {
            return;
        }
        try {
            mAudioQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public interface Callback {
        void outputVideoFrame(final int trackIndex, final ByteBuffer outBuffer, final MediaCodec.BufferInfo bufferInfo);

        void outputAudioFrame(final int trackIndex, final ByteBuffer outBuffer, final MediaCodec.BufferInfo bufferInfo);

        void outMediaFormat(final int trackIndex, MediaFormat mediaFormat);
    }


    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private void selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        mSupportColorFormatList.clear();
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            mSupportColorFormatList.add(colorFormat);
        }
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar://对应Camera预览格式I420(YV21/YUV420P)
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: //对应Camera预览格式NV12
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar://对应Camera预览格式NV21
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: {////对应Camera预览格式YV12
                return true;
            }
            default:
                return false;
        }
    }
}
