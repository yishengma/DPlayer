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
enum PrepareMode {
    ASYNC, SYNC
};

class AudioFFmpeg {
public:
    AVFormatContext *avFormatContext = NULL;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *avCodecParameters;
    SwrContext *swrContext = NULL;
    AVCodec* avCodec = NULL;
    uint8_t *buffer = NULL;
    AVFrame *avFrame = NULL;
    AVPacket *avPacket = NULL;
    int bufferSize = 0;
    int audioStreamId = -1;
    char *url = NULL;
    AudioTrack *audioTrack = NULL;
    JavaVM *javaVm;
    PrepareMode prepareMode;
public:
    AudioFFmpeg(AudioTrack *audioTrack, JavaVM *pjavaVm);

    virtual ~AudioFFmpeg();

    void prepareFFmpeg();

    void playFFmpeg();

public:
    void setDataSource(const char *url);

    void prepare();

    void prepareAsync();

    void play();

    void onErrorCallback(int code, char *msg);

    void release();

};


#endif //DPLAYER_AUDIO_FFMPEG_H
