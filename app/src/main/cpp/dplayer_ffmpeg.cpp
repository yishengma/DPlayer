//
// Created by 海盗的帽子 on 2020/6/11.
//

#include <pthread.h>
#include "dplayer_ffmpeg.h"

void *runPrepare(void *context);

DPlayerFFmpeg::DPlayerFFmpeg(
        const char *url,
        JNIDPlayer *jniDPlayer
) : jniDPlayer(jniDPlayer) {
    this->url = static_cast<char *>(malloc(sizeof(strlen(url) + 1)));
    strcpy(this->url, url);
    LOGE("%s",this->url);
}

DPlayerFFmpeg::~DPlayerFFmpeg() {
    release();
}

void DPlayerFFmpeg::prepare() {
    prepare(MAIN);
}

void DPlayerFFmpeg::prepareAsync() {
    LOGE("%s","prepareAsync");
    pthread_t prepareThread;
    pthread_create(&prepareThread, NULL, runPrepare, this);
    pthread_detach(prepareThread);
}

void DPlayerFFmpeg::prepare(ThreadMode mode) {
    LOGE("%s","prepare");

    av_register_all();
    LOGE("%s","av_register_all");

    avformat_network_init();
    LOGE("%s","avformat_network_init");

    int formatOpenInputRes = 0;
    int formatFindStreamInfoRes = 0;
    LOGE("%s",this->url);

    formatOpenInputRes = avformat_open_input(&avFormatContext, url, NULL, NULL);
    LOGE("%s","avformat_open_input");

    if (formatOpenInputRes != 0) {
        LOGE("%s","formatOpenInputRes");
        callPlayerJniError(mode, formatOpenInputRes, av_err2str(formatFindStreamInfoRes));
        return;
    }
    LOGE("%s","avformat_find_stream_info");

    formatFindStreamInfoRes = avformat_find_stream_info(avFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        LOGE("%s","formatFindStreamInfoRes");
        callPlayerJniError(mode, formatFindStreamInfoRes, av_err2str(formatFindStreamInfoRes));
        return;
    }
    LOGE("%s","av_find_best_stream");

    int audioStreamIndex = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_AUDIO, -1, -1, NULL,
                                               0);
    if (audioStreamIndex < 0) {
        LOGE("%s","audioStreamIndex");

        callPlayerJniError(mode, -1, "can not find the best stream");
        return;
    }
    LOGE("%d",audioStreamIndex);
    playerAudio = new DPlayerAudio(avFormatContext, jniDPlayer, audioStreamIndex);
    LOGE("%s","DPlayerAudio");

    playerAudio->analysisStream(mode, avFormatContext->streams);


    jniDPlayer->callPlayerPrepared(mode);

}

void DPlayerFFmpeg::play() {
    if (playerAudio != NULL) {
        playerAudio->play();
    }
}

void DPlayerFFmpeg::callPlayerJniError(ThreadMode mode, int code, char *msg) {
    release();
    jniDPlayer->callPlayerError(mode, code, msg);
}

void DPlayerFFmpeg::release() {
    if (avFormatContext != NULL) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }
    avformat_network_deinit();

    if (url != NULL) {
        free(url);
        delete url;
    }

}

void *runPrepare(void *context) {
    DPlayerFFmpeg *playerFFmpeg = (DPlayerFFmpeg *) context;
    playerFFmpeg->prepare(SUB);
    return 0;
}
