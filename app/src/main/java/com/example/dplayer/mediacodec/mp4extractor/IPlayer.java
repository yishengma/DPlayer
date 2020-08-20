package com.example.dplayer.mediacodec.mp4extractor;

import android.content.res.AssetFileDescriptor;

public interface IPlayer {
    void setDataSource(AssetFileDescriptor fileDescriptor);

    void start();

    void stop();
}
