
package com.example.dplayer.mediacodec;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.dplayer.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AacCodecActivity extends AppCompatActivity implements View.OnClickListener {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, AacCodecActivity.class);
        context.startActivity(intent);
    }

    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private MediaCodec mAudioEncoder;
    private MediaCodec mAudioDecoder;

    private MediaExtractor mMediaExtractor;

    private byte[] mBuffer;
    private FileOutputStream mFileOutputStream;
    private BufferedOutputStream mAudioBos;
    private File mAudioFile;
    private String mFilePath;

    private volatile boolean mIsReading;
    private volatile boolean mIsPlaying;
    private volatile boolean mCodeOver;

    private ArrayBlockingQueue<byte[]> mDataQueue;
    private static final int MAX_BUFFER_SIZE = 8192;
    private MediaCodec.BufferInfo mAudioEncodeBufferInfo;

    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;


    private TextView mContentView;
    private Button mRecorderView;
    private Button mPlayView;


    private ExecutorService mExecutorService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac_codec);
        initView();
    }

    private void initView() {
        mContentView = findViewById(R.id.tv_content);
        mRecorderView = findViewById(R.id.btn_recorder);
        mPlayView = findViewById(R.id.btn_play);
        mRecorderView.setOnClickListener(this);
        mPlayView.setOnClickListener(this);
        mDataQueue = new ArrayBlockingQueue<>(10);
        mExecutorService = Executors.newFixedThreadPool(2);
        mBuffer = new byte[2048];
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_recorder:
                record();
                break;
            case R.id.btn_play:
                streamPlay();
                break;
        }
    }

    private void record() {
        if (mIsReading) {
            mIsReading = false;
        } else {
            initAudioEncoder();
            initAudioRecord();
            mIsReading = true;
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    startRecorder();
                }
            });
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    recordAndEncode();
                }
            });
        }
    }

    private void initAudioEncoder() {
        try {
            mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_BUFFER_SIZE);
            mAudioEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {

        }
        if (mAudioEncoder == null) {
            return;
        }
        mAudioEncoder.start();
        encodeInputBuffers = mAudioEncoder.getInputBuffers();
        encodeOutputBuffers = mAudioEncoder.getOutputBuffers();

        mAudioEncodeBufferInfo = new MediaCodec.BufferInfo();
    }

    private void initAudioDecoder() {
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mFilePath);

            MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(0);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio")) {
                mMediaExtractor.selectTrack(0);
                mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
                mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
                mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 0);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
                mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, 0);
                mAudioDecoder = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                mAudioDecoder.configure(mediaFormat, null, null, 0);
            } else {
                return;
            }
        } catch (IOException e) {
            return;
        }
        if (mAudioDecoder == null) {
            return;
        }
        mAudioDecoder.start();
    }

    private void initAudioRecord() {
        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        mAudioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, 2048));
    }

    private void initAudioTrack() {
        int streamType = AudioManager.STREAM_MUSIC;
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int mode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        mAudioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFormat,
                Math.max(minBufferSize, 2048), mode);
        mAudioTrack.play();
    }


    private void streamPlay() {
        if (mAudioFile == null) {
            return;
        }
        if (!mIsPlaying) {
            mIsPlaying = true;
            initAudioDecoder();
            initAudioTrack();
            mExecutorService.submit(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    decodeAndPlay();
                }
            });
        } else {
            mIsPlaying = false;
        }
    }

    private void putPCMData(byte[] pcmChunk) {
        try {
            mDataQueue.put(pcmChunk);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[] getPCMData() {
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

    private void recordAndEncode() {
        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;

        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        byte[] chunkPCM;

        while (mIsReading || !mDataQueue.isEmpty()) {
            chunkPCM = getPCMData();
            if (chunkPCM == null) {
                continue;
            }
            inputIndex = mAudioEncoder.dequeueInputBuffer(-1);
            if (inputIndex >= 0) {
                inputBuffer = encodeInputBuffers[inputIndex];
                inputBuffer.clear();
                inputBuffer.limit(chunkPCM.length);
                inputBuffer.put(chunkPCM);
                mAudioEncoder.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);
            }

            outputIndex = mAudioEncoder.dequeueOutputBuffer(mAudioEncodeBufferInfo, 10000);
            while (outputIndex >= 0) {
                outBitSize = mAudioEncodeBufferInfo.size;
                outPacketSize = outBitSize + 7;//7为ADTS头部的大小
                outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
                outputBuffer.position(mAudioEncodeBufferInfo.offset);
                outputBuffer.limit(mAudioEncodeBufferInfo.offset + outBitSize);
                chunkAudio = new byte[outPacketSize];
                addADTStoPacket(44100, chunkAudio, outPacketSize);//添加ADTS
                outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中 偏移量offset=7
                outputBuffer.position(mAudioEncodeBufferInfo.offset);
                try {
                    mAudioBos.write(chunkAudio, 0, chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mAudioEncoder.releaseOutputBuffer(outputIndex, false);
                outputIndex = mAudioEncoder.dequeueOutputBuffer(mAudioEncodeBufferInfo, 10000);
            }
        }
        stopRecord();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void decodeAndPlay() {
        boolean isFinish = false;
        MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
        while (!isFinish && mIsPlaying) {
            int inputIdex = mAudioDecoder.dequeueInputBuffer(10000);
            if (inputIdex < 0) {
                isFinish = true;
            }
            ByteBuffer inputBuffer = mAudioDecoder.getInputBuffer(inputIdex);
            inputBuffer.clear();
            int samplesize = mMediaExtractor.readSampleData(inputBuffer, 0);
            if (samplesize > 0) {
                mAudioDecoder.queueInputBuffer(inputIdex, 0, samplesize, 0, 0);
                mMediaExtractor.advance();
            } else {
                isFinish = true;
            }
            int outputIndex = mAudioDecoder.dequeueOutputBuffer(decodeBufferInfo, 10000);

            ByteBuffer outputBuffer;
            byte[] chunkPCM;
            //每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
            while (outputIndex >= 0) {
                outputBuffer = mAudioDecoder.getOutputBuffer(outputIndex);
                chunkPCM = new byte[decodeBufferInfo.size];
                outputBuffer.get(chunkPCM);
                outputBuffer.clear();
                mAudioTrack.write(chunkPCM, 0, decodeBufferInfo.size);
                mAudioDecoder.releaseOutputBuffer(outputIndex, false);
                outputIndex = mAudioDecoder.dequeueOutputBuffer(decodeBufferInfo, 10000);
            }
        }

        stopPlay();
    }

    private void startRecorder() {
        try {
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media_codec_audio.aac";
            mAudioFile = new File(mFilePath);
            if (!mAudioFile.getParentFile().exists()) {
                mAudioFile.getParentFile().mkdirs();
            }
            mAudioFile.createNewFile();
            mFileOutputStream = new FileOutputStream(mAudioFile);
            mAudioBos = new BufferedOutputStream(mFileOutputStream, 200 * 1024);
            mAudioRecord.startRecording();

            while (mIsReading) {
                int read = mAudioRecord.read(mBuffer, 0, 2048);
                if (read > 0) {
                    byte[] audio = new byte[read];
                    System.arraycopy(mBuffer, 0, audio, 0, read);
                    putPCMData(audio); // PCM数据放入队列，等待编码
                }
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
    }


    private void stopRecord() {
        try {
            if (mAudioBos != null) {
                mAudioBos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mAudioBos != null) {
                try {
                    mAudioBos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mAudioBos = null;
                }
            }
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }

        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
    }

    private void stopPlay() {
        mIsPlaying = false;
        if (mAudioDecoder != null) {
            mAudioDecoder.stop();
            mAudioDecoder.release();
            mAudioDecoder = null;
        }
    }

    public static void addADTStoPacket(int sampleRateType, byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (sampleRateType << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


}