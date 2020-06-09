#include <jni.h>
#include <string>

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include "libavfilter/avfilter.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"

}

#include "GolbalDefine.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_dplayer_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {

    av_register_all();
    avformat_network_init();
    avformat_network_deinit();
    const char *info = avcodec_configuration();
    return env->NewStringUTF(info);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePlay(JNIEnv *env, jobject thiz) {
    char *url = "http://vjs.zencdn.net/v/oceans.mp4";
    av_register_all();
    avformat_network_init();
    AVFormatContext *avFormatContext = NULL;
    int formatOPenInputRes = 0;
    int formatFindStreamInfoRes = 0;
    int audioStreamIndex = -1;
    AVCodecParameters* avCodecParameters =  NULL;
    AVCodec* avCodec = NULL;
    AVCodecContext* avCodecContext = NULL;
    int result = 0;


    formatOPenInputRes = avformat_open_input(&avFormatContext, url, NULL, NULL);
    if (formatOPenInputRes != 0) {
        //回调
        //释放资源
        LOGE("avformat_open_input%s", av_err2str(formatOPenInputRes));
        //发现变量的声明必须放在任何goto前面， 不能再goto后边声明变量
        goto PlAY_FAIL;
    }

    formatFindStreamInfoRes = avformat_find_stream_info(avFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        LOGE("avformat_find_stream_info%s", av_err2str(formatFindStreamInfoRes));
        goto PlAY_FAIL;
    }

    audioStreamIndex = av_find_best_stream(avFormatContext,AVMediaType::AVMEDIA_TYPE_AUDIO,-1,-1,NULL,0);
    if (audioStreamIndex < 0 || audioStreamIndex == AVERROR_STREAM_NOT_FOUND) {
        goto PlAY_FAIL;
    }

    avCodecParameters = avFormatContext->streams[audioStreamIndex]->codecpar;

    avCodec = avcodec_find_decoder(avCodecParameters->codec_id);
    if (avCodec == NULL) {
        goto PlAY_FAIL;
    }

    avCodecContext = avcodec_alloc_context3(avCodec);
    result = avcodec_open2(avCodecContext,avCodec,NULL);
    if (result < 0) {
        LOGE("avcodec_open2%s", av_err2str(result));
        goto PlAY_FAIL;
    }
    LOGE("sample_rate %d",avCodecParameters->sample_rate);



    PlAY_FAIL:
    if (avFormatContext != NULL) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
    }
    if (avCodecContext != NULL) {
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
    }
    avformat_network_deinit();
    return;
}