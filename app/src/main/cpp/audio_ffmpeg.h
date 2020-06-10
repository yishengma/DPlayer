//
// Created by 海盗的帽子 on 2020/6/10.
//

#ifndef DPLAYER_AUDIO_FFMPEG_H
#define DPLAYER_AUDIO_FFMPEG_H

#include "audio_track.h"

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include "libavfilter/avfilter.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libswresample/swresample.h"
#include "libavutil/channel_layout.h"

}

class AudioFFmpeg {
public:
    AVFormatContext *avFormatContext = NULL;
    AVCodecContext *avCodecContext = NULL;
    SwrContext *swrContext = NULL;
    uint8_t *buffer = NULL;
    const char *url = NULL;
    AudioTrack *audioTrack = NULL;
public:
    AudioFFmpeg(AudioTrack *audioTrack, const char *url);

    virtual ~AudioFFmpeg();

public:
    void play();
    void onErrorCallback(int code,char* msg);
    void release();
};


#endif //DPLAYER_AUDIO_FFMPEG_H
