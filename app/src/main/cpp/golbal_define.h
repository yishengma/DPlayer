//
// Created by 海盗的帽子 on 2020/6/9.
//


#ifndef DPLAYER_GOLBAL_DEFINE_H
#define DPLAYER_GOLBAL_DEFINE_H

#include <android/log.h>

#define TAG "JNI_TAG"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#define AUDIO_SAMPLE_RATE 44100
#define FIND_STREAM_ERROR_CODE -0x10
#define CODEC_FIND_DECODER_ERROR_CODE -0x11
#define CODEC_ALLOC_CONTEXT_ERROR_CODE -0x12
#define SWR_ALLOC_SET_OPTS_ERROR_CODE -0x13
#define SWR_CONTEXT_INIT_ERROR_CODE -0x14
enum ThreadMode {
    MAIN,SUB
};


#endif //DPLAYER_GOLBAL_DEFINE_H
