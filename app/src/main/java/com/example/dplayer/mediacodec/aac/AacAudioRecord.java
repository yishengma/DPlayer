package com.example.dplayer.mediacodec.aac;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

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

public class AacAudioRecord {
    private static final String TAG = "AacAudioRecord";
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

    private BlockingQueue<byte[]> mDataQueue;


    public AacAudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
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
        try {
            mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mMediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);
            //声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//64000, 96000, 128000
            mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSizeInBytes);
            mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2);
            mAudioEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            Log.e(TAG, "" + e.getMessage());
        }

        mDataQueue = new ArrayBlockingQueue<>(10);
        mExecutorService = Executors.newFixedThreadPool(2);
    }

    public void start(String filePath) {
        mFilePath = filePath;
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
        mAudioEncoder.start();
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
        byte[] pcmData;
        int inputIndex;
        ByteBuffer inputBuffer;
        ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();

        int outputIndex;
        ByteBuffer outputBuffer;
        ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        byte[] aacChunk;
        while (mIsRecording || !mDataQueue.isEmpty()) {
            pcmData = dequeueData();
            if (pcmData == null) {
                continue;
            }
            inputIndex = mAudioEncoder.dequeueInputBuffer(10_000);
            if (inputIndex >= 0) {
                inputBuffer = inputBuffers[inputIndex];
                inputBuffer.clear();
                inputBuffer.limit(pcmData.length);
                inputBuffer.put(pcmData);
                mAudioEncoder.queueInputBuffer(inputIndex, 0, pcmData.length, 0, 0);
            }

            outputIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 10_000);
            while (outputIndex >= 0) {
                outputBuffer = outputBuffers[outputIndex];
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                aacChunk = new byte[bufferInfo.size + 7];
                addADTStoPacket(mSampleRateInHz, aacChunk, aacChunk.length);
                outputBuffer.get(aacChunk, 7, bufferInfo.size);
                try {
                    mBufferedOutputStream.write(aacChunk);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mAudioEncoder.releaseOutputBuffer(outputIndex, false);
                outputIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 10_000);
            }
        }
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
