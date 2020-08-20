package com.example.dplayer.mediacodec.mp4extractor;

import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.view.Surface;

import java.io.IOException;

public class Mp4Player implements IPlayer {
    private AudioPlayer mAudioPlayer;
    private VideoPlayer mVideoPlayer;

    public Mp4Player() {
        mAudioPlayer = new AudioPlayer();
        mVideoPlayer = new VideoPlayer();
    }

    public void setSurface(Surface surface) {
        mVideoPlayer.setSurface(surface);
    }

    @Override
    public void setDataSource(AssetFileDescriptor fileDescriptor) {
        mVideoPlayer.setDataSource(fileDescriptor);
        mAudioPlayer.setDataSource(fileDescriptor);
    }

    @Override
    public void start() {
        mVideoPlayer.start();
        mAudioPlayer.start();
    }

    @Override
    public void stop() {
        mVideoPlayer.stop();
        mAudioPlayer.stop();
    }
}
