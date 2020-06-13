//
// Created by 海盗的帽子 on 2020/6/13.
//

#ifndef DPLAYER_DPLAYER_VIDEO_H
#define DPLAYER_DPLAYER_VIDEO_H
extern "C" {
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libavutil/time.h>
};

#include "dplayer_media.h"
#include "dplayer_audio.h"

class DPlayerVideo : public DPlayerMedia {
public:
    SwsContext *swsContext;
    uint8_t *pFrameBuffer;
    int frameSize;
    AVFrame *rgbFrame;
    jobject surface;
    DPlayerAudio *playerAudio;

    double delayTime = 0;
    double defaultDelayTime = 0;

public:
    DPlayerVideo(int audioStreamIndex, JNIDPlayer *jnidPlayer, DPlayerStatus *dPlayerStatus,
                 DPlayerAudio *dPlayerAudio);


    virtual ~DPlayerVideo();

public:
    void play() override;

    void onAnalysisStream(ThreadMode mode, AVFormatContext *avFormatContext) override;

    void set(jobject source);

    double getFrameSleepTime(AVFrame *avFrame);

    void release();
};


#endif //DPLAYER_DPLAYER_VIDEO_H
