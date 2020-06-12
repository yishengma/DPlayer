//
// Created by 海盗的帽子 on 2020/6/11.
//

#ifndef DPLAYER_DPLAYER_AUDIO_H
#define DPLAYER_DPLAYER_AUDIO_H

#include <pthread.h>
#include "jni_dplayer.h"
#include "packet_queue.h"
#include <SLES/OpenSLES.h>"
#include <SLES/OpenSLES_Android.h>
extern "C" {
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
};

typedef bool PlayerStatus;


class DPlayerAudio {
public:
    AVFormatContext *avFormatContext = NULL;
    AVCodecContext *avCodecContext = NULL;
    SwrContext *swrContext = NULL;
    uint8_t *resampleBuffer = NULL;
    JNIDPlayer *jniDPlayer = NULL;
    int audioStreamIndex = NULL;
    PacketQueue *packetQueue = NULL;
    PlayerStatus status;
public:

    DPlayerAudio(AVFormatContext *avFormatContext, JNIDPlayer *jniDPlayer, int audioStreamIndex);

    virtual ~DPlayerAudio();

public:
    void play();

    void initCreateOpenSLES();

    int resampleAudio();

    void analysisStream(ThreadMode mode, AVStream **avStream);

    void callPlayerJniError(ThreadMode mode, int code, char *msg);

    void release();
};


#endif //DPLAYER_DPLAYER_AUDIO_H
