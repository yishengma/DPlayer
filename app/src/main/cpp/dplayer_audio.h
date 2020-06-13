//
// Created by 海盗的帽子 on 2020/6/11.
//

#ifndef DPLAYER_DPLAYER_AUDIO_H
#define DPLAYER_DPLAYER_AUDIO_H

#include <pthread.h>
#include "jni_dplayer.h"
#include "packet_queue.h"
#include "dplayer_media.h"
#include <SLES/OpenSLES.h>"
#include <SLES/OpenSLES_Android.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
};


class DPlayerAudio : public DPlayerMedia {
public:
    AVFormatContext *avFormatContext = NULL;
    SwrContext *swrContext = NULL;
    uint8_t *resampleBuffer = NULL;
public:

    DPlayerAudio(int audioStreamIndex, JNIDPlayer *jnidPlayer, DPlayerStatus *dPlayerStatus);

    virtual ~DPlayerAudio();

public:
    void play() override;

    void onAnalysisStream(ThreadMode mode, AVFormatContext *avFormatContext) override;

    void initCreateOpenSLES();

    int resampleAudio();

    void release();
};


#endif //DPLAYER_DPLAYER_AUDIO_H
