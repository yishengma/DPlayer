package com.example.dplayer.mediacodec.mp4extractor;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPresentation;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class AudioPlayer implements IPlayer {
    private int mAudioInputBufferSize;
    private AudioTrack mAudioTrack;
    private AssetFileDescriptor mFileDescriptor;
    private volatile boolean mIsPlaying = false;

    @Override
    public void setDataSource(AssetFileDescriptor fileDescriptor) {
        mFileDescriptor = fileDescriptor;
    }

    @Override
    public void stop() {
        mIsPlaying = true;
    }

    @Override
    public void start() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                onStart();
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onStart() {
        if (mFileDescriptor == null) {
            return;
        }
        mIsPlaying = true;
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaCodec audioCodec = null;
        try {
            audioExtractor.setDataSource(mFileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = audioExtractor.getTrackFormat(i);
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mimeType.startsWith("audio/")) {
                audioExtractor.selectTrack(i);
                int audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                int audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                mAudioInputBufferSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
                int frameSizeInBytes = audioChannels * 2;
                mAudioInputBufferSize = (mAudioInputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        44100,
                        (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT,
                        mAudioInputBufferSize,
                        AudioTrack.MODE_STREAM);
                mAudioTrack.play();

                //
                try {
                    audioCodec = MediaCodec.createDecoderByType(mimeType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                audioCodec.configure(mediaFormat, null, null, 0);
                break;
            }
        }
        if (audioCodec == null) {
            return;
        }
        audioCodec.start();
        MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
        while (mIsPlaying) {
            int inputIndex = audioCodec.dequeueInputBuffer(10_000);
            if (inputIndex < 0) {
                mIsPlaying = false;
            }
            ByteBuffer inputBuffer = audioCodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            int sampleSize = audioExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize > 0) {
                audioCodec.queueInputBuffer(inputIndex, 0, sampleSize, audioExtractor.getSampleTime(), 0);
                audioExtractor.advance();
            } else {
                mIsPlaying = false;
            }

            int outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, 10_000);
            ByteBuffer outputBuffer;
            byte[] chunkPCM;
            while (outputIndex >= 0) {
                outputBuffer = audioCodec.getOutputBuffer(outputIndex);
                chunkPCM = new byte[decodeBufferInfo.size];
                outputBuffer.get(chunkPCM);
                outputBuffer.clear();
                mAudioTrack.write(chunkPCM, 0, decodeBufferInfo.size);
                audioCodec.releaseOutputBuffer(outputIndex, false);
                outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, 10_000);

            }

        }
        mIsPlaying = false;
        audioCodec.stop();
        audioCodec.release();
        audioExtractor.release();
    }
}