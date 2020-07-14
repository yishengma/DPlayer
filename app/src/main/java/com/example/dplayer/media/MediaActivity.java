package com.example.dplayer.media;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.R;
import com.example.dplayer.utils.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaActivity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, MediaActivity.class);
        context.startActivity(intent);
    }

    public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();
    private Button mExactorView;
    private Button mMuxerVideoView;
    private Button mMuxerAudioView;
    private Button mCombineView;

    private MediaExtractor mMediaExtractor;
    private MediaExtractor mMediaExtractor2;
    private MediaMuxer mMediaMuxer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        mExactorView = findViewById(R.id.btn_exactor);
        mMuxerVideoView = findViewById(R.id.btn_muxer_video);
        mMuxerAudioView = findViewById(R.id.btn_muxer_audio);
        mCombineView = findViewById(R.id.btn_combine);


        mExactorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                extractorMedia();
            }
        });

        mMuxerVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                muxerVideo();
            }
        });

        mMuxerAudioView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                muxerAudio();
            }
        });

        mCombineView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                combineVideo();

            }
        });

    }

    private void extractorMedia() {
        mMediaExtractor = new MediaExtractor();
        FileOutputStream videoOutputStream = null;
        FileOutputStream audioOutputStream = null;
        try {

            File videoFile = new File(Environment.getExternalStorageDirectory().getPath(), "output_video");
            File audioFile = new File(Environment.getExternalStorageDirectory().getPath(), "output_audio");
            if (!videoFile.exists()) {
                videoFile.createNewFile();
            }
            if (!audioFile.exists()) {
                audioFile.createNewFile();
            }
            videoOutputStream = new FileOutputStream(videoFile);
            audioOutputStream = new FileOutputStream(audioFile);
            mMediaExtractor.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/input.mp4");
            int trackCount = mMediaExtractor.getTrackCount();
            int videoTrackIndex = -1;
            int audioTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(i);
                String mineType = mediaFormat.getString(MediaFormat.KEY_MIME);
                //视频
                if (mineType.startsWith("video/")) {
                    videoTrackIndex = i;
                }
                //音频
                if (mineType.startsWith("audio/")) {
                    audioTrackIndex = i;
                }
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            //切换到视频信道
            mMediaExtractor.selectTrack(videoTrackIndex);
            while (true) {
                int readSampleCount = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleCount < 0) {
                    break;
                }
                //保存视频信道信息
                byte[] buffer = new byte[readSampleCount];
                byteBuffer.get(buffer);
                videoOutputStream.write(buffer);
                byteBuffer.clear();
                mMediaExtractor.advance();
            }

            mMediaExtractor.selectTrack(audioTrackIndex);
            while (true) {
                int readSampleCount = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleCount < 0) {
                    break;
                }
                //保存音频信息
                byte[] buffer = new byte[readSampleCount];
                byteBuffer.get(buffer);
                audioOutputStream.write(buffer);
                byteBuffer.clear();
                mMediaExtractor.advance();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mMediaExtractor.release();
            IOUtil.close(videoOutputStream);
            IOUtil.close(audioOutputStream);
        }
    }

    private void muxerVideo() {
        mMediaExtractor = new MediaExtractor();
        int videoIndex = -1;
        try {
            mMediaExtractor.setDataSource(SDCARD_PATH + "/input.mp4");
            int trackCount = mMediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoIndex = i;
                }
            }

            mMediaExtractor.selectTrack(videoIndex);
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(videoIndex);
            mMediaMuxer = new MediaMuxer(SDCARD_PATH + "/output_video.h264", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int trackIndex = mMediaMuxer.addTrack(trackFormat);
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 500);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mMediaMuxer.start();
            long videoSampleTime;
            {
                mMediaExtractor.readSampleData(byteBuffer, 0);
                //skip first I frame
                if (mMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC)
                    mMediaExtractor.advance();
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long firstVideoPTS = mMediaExtractor.getSampleTime();
                mMediaExtractor.advance();
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long SecondVideoPTS = mMediaExtractor.getSampleTime();
                videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
            }

            mMediaExtractor.unselectTrack(videoIndex);
            mMediaExtractor.selectTrack(videoIndex);
            while (true) {
                int readSampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mMediaExtractor.advance();
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += videoSampleTime;

                mMediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
            }
            mMediaMuxer.stop();
            mMediaExtractor.release();
            mMediaMuxer.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void muxerAudio() {
        mMediaExtractor = new MediaExtractor();
        int audioIndex = -1;
        try {
            mMediaExtractor.setDataSource(SDCARD_PATH + "/input.mp4");
            int trackCount = mMediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioIndex = i;
                }
            }
            mMediaExtractor.selectTrack(audioIndex);
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(audioIndex);
            mMediaMuxer = new MediaMuxer(SDCARD_PATH + "/output_audio.aac", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeAudioIndex = mMediaMuxer.addTrack(trackFormat);
            mMediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            long stampTime = 0;
            //获取帧之间的间隔时间
            {
                mMediaExtractor.readSampleData(byteBuffer, 0);
                if (mMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    mMediaExtractor.advance();
                }
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long secondTime = mMediaExtractor.getSampleTime();
                mMediaExtractor.advance();
                mMediaExtractor.readSampleData(byteBuffer, 0);
                long thirdTime = mMediaExtractor.getSampleTime();
                stampTime = Math.abs(thirdTime - secondTime);
            }

            mMediaExtractor.unselectTrack(audioIndex);
            mMediaExtractor.selectTrack(audioIndex);
            while (true) {
                int readSampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mMediaExtractor.advance();

                bufferInfo.size = readSampleSize;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs += stampTime;

                mMediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
            }
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void combineVideo() {

        try {
            mMediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getPath() + "/output_muxer.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int mVideoTrackIndex = 0;
        int mAudioTrackIndex = 0;
        long frameRate1 = 0;
        long frameRate2 = 0;

        MediaFormat format1;
        MediaFormat format2;
        try {
            mMediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mMediaExtractor.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/output_video.h264");//媒体文件的位置
            for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
                format1 = mMediaExtractor.getTrackFormat(i);
                String mime = format1.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video")) {
                    mMediaExtractor.selectTrack(i);//选择此视频轨道
                    frameRate1 = format1.getInteger(MediaFormat.KEY_FRAME_RATE);
                    mVideoTrackIndex = mMediaMuxer.addTrack(format1);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mMediaExtractor2 = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mMediaExtractor2.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/output_audio.aac");//媒体文件的位置
            for (int i = 0; i < mMediaExtractor2.getTrackCount(); i++) {
                format2 = mMediaExtractor2.getTrackFormat(i);
                String mime = format2.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
                    ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
                    {
                        mMediaExtractor2.selectTrack(i);//选择此音频轨道
                        mMediaExtractor2.readSampleData(buffer, 0);
                        long first_sampletime = mMediaExtractor2.getSampleTime();
                        mMediaExtractor2.advance();
                        long second_sampletime = mMediaExtractor2.getSampleTime();
                        frameRate2 = Math.abs(second_sampletime - first_sampletime);//时间戳
                        mMediaExtractor2.unselectTrack(i);
                    }
                    mMediaExtractor2.selectTrack(i);
                    mAudioTrackIndex = mMediaMuxer.addTrack(format2);

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        mMediaMuxer.start();
        MediaCodec.BufferInfo info1 = new MediaCodec.BufferInfo();
        info1.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
        int sampleSize1 = 0;
        while ((sampleSize1 = mMediaExtractor.readSampleData(buffer, 0)) > 0) {

            info1.offset = 0;
            info1.size = sampleSize1;
            info1.flags = mMediaExtractor.getSampleFlags();
            info1.presentationTimeUs += 1000 * 1000 / frameRate1;
            mMediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info1);
            mMediaExtractor.advance();
        }


        MediaCodec.BufferInfo info2 = new MediaCodec.BufferInfo();
        info2.presentationTimeUs = 0;

        int sampleSize2 = 0;
        while ((sampleSize2 = mMediaExtractor2.readSampleData(buffer, 0)) > 0) {
            info2.offset = 0;
            info2.size = sampleSize2;
            info2.flags = mMediaExtractor2.getSampleFlags();
            info2.presentationTimeUs += frameRate2;
            mMediaMuxer.writeSampleData(mAudioTrackIndex, buffer, info2);
            mMediaExtractor2.advance();
        }

        try {
            mMediaExtractor.release();
            mMediaExtractor = null;
            mMediaExtractor2.release();
            mMediaExtractor2 = null;
            mMediaMuxer.stop();
            mMediaMuxer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}