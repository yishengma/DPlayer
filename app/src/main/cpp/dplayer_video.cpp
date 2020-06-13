//
// Created by 海盗的帽子 on 2020/6/13.
//

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "dplayer_video.h"


DPlayerVideo::DPlayerVideo(int audioStreamIndex, JNIDPlayer *jnidPlayer,
                           DPlayerStatus *dPlayerStatus, DPlayerAudio *dPlayerAudio) : DPlayerMedia(
        audioStreamIndex,
        jnidPlayer, dPlayerStatus) {
    this->playerAudio = dPlayerAudio;
}

DPlayerVideo::~DPlayerVideo() {
    release();
}

void *runOpenGLES(void *context) {
    DPlayerVideo *dPlayerVideo = (DPlayerVideo *) context;
    JNIEnv *env;
    if (dPlayerVideo->jnidPlayer->javaVm->AttachCurrentThread(&env, 0) != JNI_OK) {
        return 0;
    }
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, dPlayerVideo->surface);

    AVCodecContext *avCodecContext = dPlayerVideo->avCodecContext;

    ANativeWindow_setBuffersGeometry(nativeWindow, avCodecContext->width, avCodecContext->height,
                                     WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer outBuffer;

    AVPacket *avPacket = av_packet_alloc();
    AVFrame *avFrame = av_frame_alloc();
    while (dPlayerVideo->playerStatus != NULL && dPlayerVideo->playerStatus->isExit) {
        avPacket = dPlayerVideo->packetQueue->pop();
        int codeSendPackets = avcodec_send_packet(avCodecContext, avPacket);
        if (codeSendPackets == 0) {
            int codeReceiveFrames = avcodec_receive_frame(avCodecContext, avFrame);
            if (codeReceiveFrames == 0) {

                //yuv 转 rgb
                sws_scale(dPlayerVideo->swsContext, avFrame->data, avFrame->linesize, 0,
                          avCodecContext->height,
                          dPlayerVideo->rgbFrame->data, dPlayerVideo->rgbFrame->linesize);

                //
                double frameSleepTime = dPlayerVideo->getFrameSleepTime(avFrame);
                av_usleep(frameSleepTime * 1000);
                ANativeWindow_lock(nativeWindow, &outBuffer, NULL);
                memcpy(outBuffer.bits, dPlayerVideo->pFrameBuffer, dPlayerVideo->frameSize);
                ANativeWindow_unlockAndPost(nativeWindow);
            }
        }
        av_packet_unref(avPacket);
        av_frame_unref(avFrame);
    }
    av_packet_free(&avPacket);
    av_frame_free(&avFrame);
    return 0;

}


void DPlayerVideo::play() {
    pthread_t initThread;
    pthread_create(&initThread, NULL, runOpenGLES, this);
    pthread_detach(initThread);
}

void DPlayerVideo::onAnalysisStream(ThreadMode mode, AVFormatContext *avFormatContext) {
    swsContext = sws_getContext(avCodecContext->width, avCodecContext->height,
                                avCodecContext->pix_fmt, avCodecContext->width,
                                avCodecContext->height,
                                AV_PIX_FMT_RGBA, SWS_BILINEAR, NULL, NULL, NULL);

    rgbFrame = av_frame_alloc();
    frameSize = av_image_get_buffer_size(AV_PIX_FMT_RGBA, avCodecContext->width,
                                         avCodecContext->height, 1);
    uint8_t *frameBuffer = (uint8_t *) malloc(frameSize);

    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, frameBuffer, AV_PIX_FMT_RGBA,
                         avCodecContext->width, avCodecContext->height, 1);

    int num = avFormatContext->streams[streamIndex]->avg_frame_rate.num;
    int den = avFormatContext->streams[streamIndex]->avg_frame_rate.den;

    if (den != 0 && num != 0) {
        defaultDelayTime = 1.0f * den / num;
    }

}

void DPlayerVideo::set(jobject source) {

    JNIEnv* env;
    if (jnidPlayer->javaVm->AttachCurrentThread(&env,0) != JNI_OK) {
        return;
    }
    this->surface = env->NewGlobalRef(source);
//    jnidPlayer->javaVm->DetachCurrentThread();
}

double DPlayerVideo::getFrameSleepTime(AVFrame *avFrame) {
    double times = av_frame_get_best_effort_timestamp(avFrame) * av_q2d(timebase);
    if (times > currentTime) {
        currentTime = times;
    }

    double diffTime = this->playerAudio->currentTime - currentTime;

    if (diffTime > 0.016 || diffTime < -0.016) {
        if (diffTime > 0.016) {
            delayTime = delayTime * 2 / 3;
        } else if (diffTime < -0.016) {
            delayTime = delayTime * 3 / 2;
        }

        if (delayTime < defaultDelayTime / 2) {
            delayTime = defaultDelayTime * 2 / 3;
        } else if (delayTime > defaultDelayTime * 2) {
            delayTime = defaultDelayTime * 3 / 2;
        }
    }

    if (diffTime >= 0.25) {
        delayTime = 0;
    } else if (diffTime <= -0.25) {
        delayTime = defaultDelayTime * 2;
    }
    return delayTime;
}

void DPlayerVideo::release() {
    DPlayerMedia::release();
    if (swsContext != NULL) {
        sws_freeContext(swsContext);
        free(swsContext);
        swsContext = NULL;
    }

    if (pFrameBuffer != NULL) {
        free(pFrameBuffer);
        pFrameBuffer = NULL;
    }
    if (rgbFrame != NULL) {
        av_frame_free(&rgbFrame);
        rgbFrame = NULL;
    }

    if (jnidPlayer != NULL) {
        jnidPlayer->jniEnv->DeleteGlobalRef(surface);
    }

}

