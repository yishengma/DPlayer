//
// Created by 海盗的帽子 on 2020/6/11.
//

#ifndef DPLAYER_DPLAYER_FFMPEG_H
#define DPLAYER_DPLAYER_FFMPEG_H

#include "jni_dplayer.h"
#include "dplayer_audio.h"

extern "C" {
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
};

class DPlayerFFmpeg {
public:
    AVFormatContext *avFormatContext = NULL;
    char *url = NULL;
    JNIDPlayer *jniDPlayer = NULL;
    DPlayerAudio *playerAudio = NULL;
public:
    DPlayerFFmpeg(const char *url, JNIDPlayer *jniDPlayer);

    virtual ~DPlayerFFmpeg();

public:
    void prepare();

    void prepareAsync();

    void prepare(ThreadMode mode);

    void play();

    void callPlayerJniError(ThreadMode mode, int code, char *msg);

    void release();


};


#endif //DPLAYER_DPLAYER_FFMPEG_H
