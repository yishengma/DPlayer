package com.example.dplayer.mediacodec.aac;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.dplayer.R;
import com.example.dplayer.utils.IOUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AacAudioRecord2 {
    private AudioRecord mAudioRecord;
    private MediaCodec mAudioEncoder;

    private volatile boolean mIsRecording = false;
    private ExecutorService mExecutorService;

    private int mAudioSource;
    private int mSampleRateInHz;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSizeInBytes;
    private MediaFormat mMediaFormat;
    private String mFilePath;
    private File mFile;
    private FileOutputStream mFileOutputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private BlockingQueue<byte[]> mDataQueue;
    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            byte[] pcmData = dequeueData();
            if (pcmData == null) {
                return;
            }
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(i);
            inputBuffer.clear();
            inputBuffer.put(pcmData);
            mediaCodec.queueInputBuffer(i, 0, pcmData.length, 0, 0);

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
            byte[] aacChunk = new byte[bufferInfo.size + 7];
            addADTStoPacket(mSampleRateInHz, aacChunk, aacChunk.length);
            outputBuffer.get(aacChunk, 7, bufferInfo.size);
            try {
                mBufferedOutputStream.write(aacChunk);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mAudioEncoder.releaseOutputBuffer(i, false);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {

        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AacAudioRecord2(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        mAudioSource = audioSource;
        mSampleRateInHz = sampleRateInHz;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mBufferSizeInBytes = bufferSizeInBytes;
        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        try {
            mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mHandlerThread = new HandlerThread("Encoder");
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandlerThread.start();
        mMediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelConfig);
        mAudioEncoder.setCallback(mCallback, mHandler);
        mAudioEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mDataQueue = new ArrayBlockingQueue<>(10);
        mExecutorService = Executors.newFixedThreadPool(2);

    }

    public void start() {
        mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media_codec_audio.aac";
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                startRecord();
            }
        });
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                startEncode();
            }
        });
    }

    public void stop() {
        mIsRecording = false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mBufferedOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                IOUtil.close(mBufferedOutputStream);
                IOUtil.close(mFileOutputStream);
                mAudioEncoder.stop();
                mAudioEncoder.release();
                mAudioEncoder = null;
            }
        });
    }

    private void startRecord() {
        mAudioRecord.startRecording();
        mIsRecording = true;
        byte[] buffer = new byte[2048];
        while (mIsRecording) {
            int len = mAudioRecord.read(buffer, 0, 2048);
            if (len > 0) {
                byte[] data = new byte[len];
                System.arraycopy(buffer, 0, data, 0, len);
                queueData(data);
            }
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
    }

    private void startEncode() {
        if (TextUtils.isEmpty(mFilePath)) {
            return;
        }
        mFile = new File(mFilePath);
        if (mFile.exists()) {
            mFile.delete();
        }
        try {
            mFile.createNewFile();
            mFileOutputStream = new FileOutputStream(mFile);
            mBufferedOutputStream = new BufferedOutputStream(mFileOutputStream, 2048);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioEncoder.start();
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

    private void queueData(byte[] data) {
        try {
            mDataQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addADTStoPacket(int sampleRateType, byte[] packet, int packetLen) {
        int profile = 2;
        int chanCfg = 2;
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (sampleRateType << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

}
