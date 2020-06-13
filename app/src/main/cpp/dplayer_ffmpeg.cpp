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
    this->url = static_cast<char *>(malloc((strlen(url) + 1)));
    memcpy(this->url, url, strlen(url) + 1);
    playerStatus = new DPlayerStatus();
}

DPlayerFFmpeg::~DPlayerFFmpeg() {
    release();
}

void DPlayerFFmpeg::prepare() {
    prepare(MAIN);
}

void DPlayerFFmpeg::prepareAsync() {
    pthread_t prepareThread;
    pthread_create(&prepareThread, NULL, runPrepare, this);
    pthread_detach(prepareThread);
}

void DPlayerFFmpeg::prepare(ThreadMode mode) {

    av_register_all();
    avformat_network_init();

    int formatOpenInputRes = 0;
    int formatFindStreamInfoRes = 0;

    formatOpenInputRes = avformat_open_input(&avFormatContext, url, NULL, NULL);

    if (formatOpenInputRes != 0) {
        callPlayerJniError(mode, formatOpenInputRes, av_err2str(formatOpenInputRes));
        return;
    }

    formatFindStreamInfoRes = avformat_find_stream_info(avFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        callPlayerJniError(mode, formatFindStreamInfoRes, av_err2str(formatFindStreamInfoRes));
        return;
    }

    int audioStreamIndex = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_AUDIO, -1, -1, NULL,
                                               0);
    if (audioStreamIndex < 0) {

        callPlayerJniError(mode, -1, "can not find the best stream");
        return;
    }
    playerAudio = new DPlayerAudio(audioStreamIndex, jniDPlayer, playerStatus);

    playerAudio->analysisStream(mode, avFormatContext);

    int videoStreamIndex = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_VIDEO, -1, -1, NULL,
                                               0);
    if (videoStreamIndex < 0) {

        callPlayerJniError(mode, -1, "can not find the best stream");
        return;
    }

    playerVideo = new DPlayerVideo(videoStreamIndex, jniDPlayer, playerStatus, playerAudio);

    playerVideo->analysisStream(mode, avFormatContext);

    jniDPlayer->callPlayerPrepared(mode);

}

void *readPacket(void *context) {
    DPlayerFFmpeg *dPlayerFFmpeg = (DPlayerFFmpeg *) context;
    while (dPlayerFFmpeg->playerStatus != NULL && !dPlayerFFmpeg->playerStatus->isExit) {
        AVPacket *avPacket = av_packet_alloc();
        if (av_read_frame(dPlayerFFmpeg->avFormatContext, avPacket) >= 0) {
            if (avPacket->stream_index == dPlayerFFmpeg->playerAudio->streamIndex) {
                dPlayerFFmpeg->playerAudio->packetQueue->push(avPacket);
            } else if (avPacket->stream_index == dPlayerFFmpeg->playerAudio->streamIndex) {
                dPlayerFFmpeg->playerVideo->packetQueue->push(avPacket);
            }
        } else {
            av_packet_free(&avPacket);
        }
    }
    return 0;
}

void DPlayerFFmpeg::play() {
    pthread_t readPacketThread;
    pthread_create(&readPacketThread, NULL, readPacket, this);
    pthread_detach(readPacketThread);
    if (playerAudio != NULL) {
        playerAudio->play();
    }
    if (playerVideo != NULL) {
        playerVideo->play();
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

void DPlayerFFmpeg::setSurface(jobject surface) {
    if (playerVideo != NULL) {
        playerVideo->set(surface);
    }
}

void *runPrepare(void *context) {
    DPlayerFFmpeg *playerFFmpeg = (DPlayerFFmpeg *) context;
    playerFFmpeg->prepare(SUB);
    return 0;
}
