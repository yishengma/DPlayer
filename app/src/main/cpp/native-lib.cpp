#include <jni.h>
#include <string>
#include "jni_dplayer.h"
#include "dplayer_ffmpeg.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}
//adb logcat | ndk-stack -sym app/build/intermediates/cmake/debug/obj/armeabi
//fau adr 0x0 空指针


JavaVM *pJavaVM = NULL;
JNIDPlayer *jniDPlayer;
DPlayerFFmpeg *playerFFmpeg;
//
//so 被加载的时候调用的方法
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *reserved) {
    pJavaVM = javaVm;
    JNIEnv *env;
    if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepare(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    if (playerFFmpeg == NULL) {
        jniDPlayer = new JNIDPlayer(pJavaVM, env, thiz);
        playerFFmpeg = new DPlayerFFmpeg(url, jniDPlayer);
        playerFFmpeg->prepare();
    }
    env->ReleaseStringUTFChars(_url, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepareAsync(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    if (playerFFmpeg == NULL) {
        jniDPlayer = new JNIDPlayer(pJavaVM, env, thiz);
        playerFFmpeg = new DPlayerFFmpeg(url, jniDPlayer);
        playerFFmpeg->prepareAsync();
    }
    env->ReleaseStringUTFChars(_url, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePlay(JNIEnv *env, jobject thiz) {
    if (playerFFmpeg != NULL) {
        playerFFmpeg->play();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeRelease(JNIEnv *env, jobject thiz) {
    if (playerFFmpeg != NULL) {
        playerFFmpeg->release();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DVideoView_decodeVideo(JNIEnv *env, jobject thiz, jstring url,


                                                jobject surface) {
    const char *uri = env->GetStringUTFChars(url, 0);
    av_register_all();
    avformat_network_init();
    AVFormatContext *avFormatContext = NULL;
    int formatOpenInputRes = 0;
    int formatFindStreamInfoRes = 0;
    int audioStreamIndex = -1;
    AVCodecParameters *avCodecParameters = NULL;
    AVCodec *avCodec = NULL;
    AVCodecContext *avCodecContext = NULL;
    int codecParametersToContextRes = 0;
    int codeOpenRes = -1;
    int index = 0;
    AVPacket *avPacket = NULL;
    AVFrame *avFrame = NULL;


    formatOpenInputRes = avformat_open_input(&avFormatContext, uri, NULL, NULL);

    formatFindStreamInfoRes = avformat_find_stream_info(avFormatContext, NULL);

    audioStreamIndex = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);

    avCodecParameters = avFormatContext->streams[audioStreamIndex]->codecpar;

    avCodec = avcodec_find_decoder(avCodecParameters->codec_id);

    avCodecContext = avcodec_alloc_context3(avCodec);

    codecParametersToContextRes = avcodec_parameters_to_context(avCodecContext, avCodecParameters);

    codeOpenRes = avcodec_open2(avCodecContext, avCodec, NULL);


    LOGE("%d",avCodecContext->width);
    LOGE("%d",avCodecContext->height);

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);


    ANativeWindow_setBuffersGeometry(nativeWindow, avCodecContext->width, avCodecContext->height,
                                     WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer outBuffer;
    //SwrContext和SwsContext
    LOGE("%d",11);

    SwsContext *swsContext = sws_getContext(avCodecContext->width, avCodecContext->height,
                                            avCodecContext->pix_fmt, avCodecContext->width,
                                            avCodecContext->height,
                                            AV_PIX_FMT_RGBA, SWS_BILINEAR, NULL, NULL, NULL);

    AVFrame *rgbaFrame = av_frame_alloc();
    int frameSize = av_image_get_buffer_size(AV_PIX_FMT_RGBA, avCodecContext->width,
                                             avCodecContext->height, 1);
    uint8_t *frameBuffer = (uint8_t *) malloc(frameSize);

    av_image_fill_arrays(rgbaFrame->data, rgbaFrame->linesize, frameBuffer, AV_PIX_FMT_RGBA,
                         avCodecContext->width, avCodecContext->height, 1);

    avPacket = av_packet_alloc();
    avFrame = av_frame_alloc();

    while (av_read_frame(avFormatContext, avPacket) >= 0) {
        if (avPacket->stream_index == audioStreamIndex) {
            int codeSendPackets = avcodec_send_packet(avCodecContext, avPacket);
            if (codeSendPackets == 0) {
                int codeReceiveFrames = avcodec_receive_frame(avCodecContext, avFrame);
                if (codeReceiveFrames == 0) {

                    //yuv 转 rgb
                    sws_scale(swsContext, avFrame->data, avFrame->linesize, 0,
                              avCodecContext->height,
                              rgbaFrame->data, rgbaFrame->linesize);

                    //
                    ANativeWindow_lock(nativeWindow, &outBuffer, NULL);
                    memcpy(outBuffer.bits, frameBuffer, frameSize);
                    ANativeWindow_unlockAndPost(nativeWindow);
                }
            }
        }
        av_packet_unref(avPacket);
        av_frame_unref(avFrame);
    }

    av_packet_free(&avPacket);
    av_frame_free(&avFrame);




}