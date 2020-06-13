//
// Created by 海盗的帽子 on 2020/6/13.
//

#ifndef DPLAYER_DPLAYER_MEDIA_H
#define DPLAYER_DPLAYER_MEDIA_H


#include "jni_dplayer.h"
#include "packet_queue.h"
#include "dplayer_status.h"

extern "C" {
#include "libavcodec/avcodec.h"
};

class DPlayerMedia {
public:
    int streamIndex = -1;
    AVCodecContext *avCodecContext = NULL;
    JNIDPlayer *jnidPlayer = NULL;
    PacketQueue *packetQueue = NULL;
    DPlayerStatus *playerStatus = NULL;

    long long duration = 0;

    double currentTime = 0;

    double lastUpdateTime = 0;

    AVRational timebase;
public:
    DPlayerMedia(int streamIndex, JNIDPlayer *jnidPlayer, DPlayerStatus *playerStatus);

    virtual ~DPlayerMedia();

public:
    virtual void play() = 0;

    void analysisStream(ThreadMode mode, AVFormatContext *avFormatContext);

    virtual void onAnalysisStream(ThreadMode mode, AVFormatContext *avFormatContext) = 0;

    virtual void release();


    void callPlayerJniError(ThreadMode mode, int code, char *msg);

private:
    void findDecoder(ThreadMode mode, AVFormatContext *avFormatContext);
};


#endif //DPLAYER_DPLAYER_MEDIA_H
