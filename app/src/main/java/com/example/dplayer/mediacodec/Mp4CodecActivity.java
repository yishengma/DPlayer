package com.example.dplayer.mediacodec;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.R;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4CodecActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, Mp4CodecActivity.class);
        context.startActivity(intent);
    }

    private SurfaceView mSurfaceView;
    private AudioPlayer mAudioPlayer;
    private VideoPlayer mVideoPlayer;

    private Button mBtnPlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4_codec);
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = getAssets().openFd("input.mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);

        mBtnPlayView = findViewById(R.id.btn_play);
        mBtnPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAudioPlayer.start();
                mVideoPlayer.start();
            }
        });

        mAudioPlayer = new AudioPlayer();
        mVideoPlayer = new VideoPlayer();

        mAudioPlayer.setDataSource(assetFileDescriptor);
        mVideoPlayer.setDataSource(assetFileDescriptor);
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mVideoPlayer.setSurface(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    class AudioPlayer extends Thread {
        private int mAudioInputBufferSize;
        private AudioTrack mAudioTrack;
        private AssetFileDescriptor mSource;
        private volatile boolean mIsPlaying = false;

        public void setDataSource(AssetFileDescriptor source) {
            mSource = source;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if (mSource == null) {
                return;
            }
            mIsPlaying = true;
            MediaExtractor audioExtractor = new MediaExtractor();
            MediaCodec audioCodec = null;
            try {
                audioExtractor.setDataSource(mSource);
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
                            audioSampleRate,
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
                    audioCodec.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);
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
            if (audioCodec != null) {
                audioCodec.stop();
                audioCodec.release();
                audioCodec = null;
            }
            if (audioExtractor != null) {
                audioExtractor.release();
            }
            Log.e("Dplayer","audio release");
        }
    }

    class VideoPlayer extends Thread {
        private AssetFileDescriptor mSource;
        private volatile boolean mIsPlaying = false;
        private Surface mSurface;
        private volatile boolean mEOF = false;

        public void setDataSource(AssetFileDescriptor source) {
            mSource = source;
        }

        public void setSurface(Surface surface) {
            mSurface = surface;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            MediaExtractor videoExtractor = new MediaExtractor();
            MediaCodec videoCodec = null;
            long startWhen = 0;
            try {
                videoExtractor.setDataSource(mSource);
            } catch (IOException e) {
                e.printStackTrace();
            }
            boolean firstFrame = false;
            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoExtractor.selectTrack(i);
                    try {
                        videoCodec = MediaCodec.createDecoderByType(mimeType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    videoCodec.configure(mediaFormat, mSurface, null, 0);
                    break;
                }
            }
            if (videoCodec == null) {
                return;
            }
            videoCodec.start();
            while (!mEOF) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                ByteBuffer[] inputBuffers = videoCodec.getInputBuffers();
                int inputIndex = videoCodec.dequeueInputBuffer(10_000);
                if (inputIndex > 0) {
                    ByteBuffer byteBuffer = inputBuffers[inputIndex];
                    int sampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                    if (sampleSize > 0) {
                        videoCodec.queueInputBuffer(inputIndex, 0, sampleSize, videoExtractor.getSampleTime(), 0);
                        videoExtractor.advance();
                    } else {
                        videoCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                int outputIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10_000);
                switch (outputIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        videoCodec.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    default:
                        if (!firstFrame) {
                            startWhen = System.currentTimeMillis();
                            firstFrame = true;
                        }
                        long sleepTime = (bufferInfo.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                        if (sleepTime > 0) {
                            SystemClock.sleep(sleepTime);
                        }
                        videoCodec.releaseOutputBuffer(outputIndex, true);
                        break;

                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    mEOF = true;
                    break;
                }
            }

            videoCodec.stop();
            videoCodec.release();
            videoExtractor.release();
            Log.e("Dplayer","video release");
        }
    }

}