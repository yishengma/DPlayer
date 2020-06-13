//
// Created by 海盗的帽子 on 2020/6/13.
//

#include "dplayer_media.h"

DPlayerMedia::DPlayerMedia(int streamIndex, JNIDPlayer *jnidPlayer, DPlayerStatus *playerStatus)
        : streamIndex(streamIndex), jnidPlayer(jnidPlayer), playerStatus(playerStatus) {

    this->packetQueue = new PacketQueue();
}

DPlayerMedia::~DPlayerMedia() {
    release();
}

void DPlayerMedia::analysisStream(ThreadMode mode, AVFormatContext *avFormatContext) {
    findDecoder(mode, avFormatContext);
    if (avCodecContext == NULL) {
        LOGE("%s","analysisStream avCodecContext == NULL");
    }

    onAnalysisStream(mode, avFormatContext);
}

void DPlayerMedia::release() {
    if (packetQueue != NULL) {
        delete packetQueue;
        packetQueue = NULL;
    }
    if (avCodecContext != NULL) {
        delete avCodecContext;
        avCodecContext = NULL;
    }
}

void DPlayerMedia::callPlayerJniError(ThreadMode mode, int code, char *msg) {
    release();
    jnidPlayer->callPlayerError(mode, code, msg);
}

void DPlayerMedia::findDecoder(ThreadMode mode, AVFormatContext *avFormatContext) {

// 查找解码
    AVCodecParameters *pCodecParameters = avFormatContext->streams[streamIndex]->codecpar;
    AVCodec *pCodec = avcodec_find_decoder(pCodecParameters->codec_id);
    if (pCodec == NULL) {
        LOGE("codec find  decoder error");
        callPlayerJniError(mode, CODEC_FIND_DECODER_ERROR_CODE,
                           "codec find audio decoder error");
        return;
    }

    // 打开解码器
    avCodecContext = avcodec_alloc_context3(pCodec);
    if (avCodecContext == NULL) {
        LOGE("codec alloc context error");
        callPlayerJniError(mode, CODEC_ALLOC_CONTEXT_ERROR_CODE, "codec alloc context error");
        return;
    }

    int codecParametersToContextRes = avcodec_parameters_to_context(avCodecContext,
                                                                    pCodecParameters);
    if (codecParametersToContextRes < 0) {
        LOGE("codec parameters to context error: %s", av_err2str(codecParametersToContextRes));
        callPlayerJniError(mode, codecParametersToContextRes,
                           av_err2str(codecParametersToContextRes));
        return;
    }

    int codecOpenRes = avcodec_open2(avCodecContext, pCodec, NULL);
    if (codecOpenRes != 0) {
        LOGE("codec audio open error: %s", av_err2str(codecOpenRes));
        callPlayerJniError(mode, codecOpenRes, av_err2str(codecOpenRes));
        return;
    }
    duration = avFormatContext->duration;
    timebase = avFormatContext->streams[streamIndex]->time_base;

}

