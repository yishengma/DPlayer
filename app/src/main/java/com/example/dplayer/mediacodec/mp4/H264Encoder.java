package com.example.dplayer.mediacodec.mp4;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.example.dplayer.utils.YUVUtil;
import com.example.dplayer.utils.YUVEngine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class H264Encoder {
    public static final String VIDEO_MIME_TYPE = "video/avc";//就是 H264
    public static final int H264_ENCODER = 1;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private BlockingQueue<byte[]> mQueue;
    private MediaCodecInfo mMediaCodecInfo;
    private int mColorFormat;
    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private byte[] mYUVBuffer;
    private byte[] mRotatedYUVBuffer;

    private int[] mOutWidth;
    private int[] mOutHeight;
    private ExecutorService mExecutorService;
    private volatile boolean mIsEncoding;

    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void outputMediaFormat(int type, MediaFormat mediaFormat);

        void onEncodeOutput(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

        void onStop(int type);
    }

    public H264Encoder(int width, int height, int fps) {
        Log.e("eee", "w:" + width + "h:" + height + "fps:" + fps);
        mWidth = width;
        mHeight = height;
        mQueue = new LinkedBlockingQueue<>();
        mMediaCodecInfo = selectCodecInfo();
        mColorFormat = selectColorFormat(mMediaCodecInfo);
        mBitRate = (mWidth * mHeight * 3 / 2) * 8 * fps;
        mMediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mHeight, mWidth);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);// todo 没有这一行会报错 configureCodec returning error -38
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        try {
            mMediaCodec = MediaCodec.createByCodecName(mMediaCodecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mExecutorService = Executors.newFixedThreadPool(1);

        mYUVBuffer = new byte[YUVUtil.getYUVBuffer(width, height)];
        mRotatedYUVBuffer = new byte[YUVUtil.getYUVBuffer(width, height)];
        mOutWidth = new int[1];
        mOutHeight = new int[1];
        YUVEngine.startYUVEngine();
    }


    public void start() {
        if (mIsEncoding) {
            return;
        }


        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                mIsEncoding = true;
                mMediaCodec.start();
                while (mIsEncoding) {
                    byte[] data = dequeueData();
                    if (data == null) {
                        continue;
                    }
                    encodeVideoData(data);
                }

                mMediaCodec.stop();
                mMediaCodec.release();
                if (mCallback != null) {
                    mCallback.onStop(H264_ENCODER);
                }

            }
        });
    }

    public void stop() {
        mIsEncoding = false;
    }

    private byte[] dequeueData() {
        if (mQueue.isEmpty()) {
            return null;
        }
        try {
            return mQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void queueData(byte[] data) {
        if (data == null || !mIsEncoding) {
            return;
        }
        try {
            mQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void encodeVideoData(byte[] data) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        mRotatedYUVBuffer = transferFrameData(data, mYUVBuffer, mRotatedYUVBuffer);
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        int inputIndex = mMediaCodec.dequeueInputBuffer(10_000);
        if (inputIndex >= 0) {
            ByteBuffer byteBuffer = inputBuffers[inputIndex];
            byteBuffer.clear();
            byteBuffer.put(mRotatedYUVBuffer);
            long pts = System.currentTimeMillis() * 1000 - AVTimer.getBaseTimestampUs();
            mMediaCodec.queueInputBuffer(inputIndex, 0, mRotatedYUVBuffer.length, pts, 0);
        }

        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        int outputIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
        if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mMediaCodec.getOutputBuffers();
        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat newFormat = mMediaCodec.getOutputFormat();
            if (null != mCallback) {
                mCallback.outputMediaFormat(H264_ENCODER, newFormat);
            }
        }
        while (outputIndex >= 0) {
            ByteBuffer byteBuffer = outputBuffers[outputIndex];
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                bufferInfo.size = 0;
            }
            if (bufferInfo.size != 0 && mCallback != null) {
//                boolean keyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
//                Log.i("ethan", "is key frame :%s"+keyFrame);
                mCallback.onEncodeOutput(byteBuffer, bufferInfo);
            }
            mMediaCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
        }
    }

    private byte[] transferFrameData(byte[] data, byte[] yuvBuffer, byte[] rotatedYuvBuffer) {
        //Camera 传入的是 NV21
        //转换成 MediaCodec 支持的格式
        switch (mColorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar://对应Camera预览格式I420(YV21/YUV420P)
                YUVEngine.Nv21ToI420(data, yuvBuffer, mWidth, mHeight);
                YUVEngine.I420ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotatedYuvBuffer, mOutWidth, mOutHeight);
                //Log.i("transferFrameData", "COLOR_FormatYUV420Planar");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: //对应Camera预览格式NV12
                YUVEngine.Nv21ToNv12(data, yuvBuffer, mWidth, mHeight);
                YUVEngine.Nv12ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotatedYuvBuffer, mOutWidth, mOutHeight);
                //Log.i("transferFrameData", "COLOR_FormatYUV420SemiPlanar");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar://对应Camera预览格式NV21
                System.arraycopy(data, 0, yuvBuffer, 0, mWidth * mHeight * 3 / 2);
                YUVEngine.Nv21ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotatedYuvBuffer, mOutWidth, mOutHeight);
                //Log.i("transferFrameData", "COLOR_FormatYUV420PackedSemiPlanar");
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: ////对应Camera预览格式YV12
                YUVEngine.Nv21ToYV12(data, yuvBuffer, mWidth, mHeight);
                YUVEngine.Yv12ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotatedYuvBuffer, mOutWidth, mOutHeight);
                //Log.i("transferFrameData", "COLOR_FormatYUV420PackedPlanar");
                break;
        }
        return rotatedYuvBuffer;
    }

    private MediaCodecInfo selectCodecInfo() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(com.example.dplayer.mediacodec.h264.H264Encoder.VIDEO_MIME_TYPE)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    //查询支持的输入格式
    private int selectColorFormat(MediaCodecInfo codecInfo) {
        if (codecInfo == null) {
            return -1;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(com.example.dplayer.mediacodec.h264.H264Encoder.VIDEO_MIME_TYPE);
        int[] colorFormats = capabilities.colorFormats;
        for (int i = 0; i < colorFormats.length; i++) {
            if (isRecognizedFormat(colorFormats[i])) {
                return colorFormats[i];
            }
        }
        return -1;
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
