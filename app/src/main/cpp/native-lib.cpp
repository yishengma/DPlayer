#include <jni.h>
#include <string>

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

#include "golbal_define.h"

jobject initCreateAudioTrack(JNIEnv *env) {
    int streamType = 3;
    int sampleRateInHz = AUDIO_SAMPLE_RATE;
    int channelConfig = (0x4 | 0x8);
    int audioFormat = 2;
    int bufferSizeInBytes = 0;
    int mode = 1;
    jclass jAudioTrackClass = env->FindClass("android/media/AudioTrack");
    jmethodID jwriteMid = env->GetMethodID(jAudioTrackClass,"write","([BII)I");
    jmethodID jAudioTrackMid = env->GetMethodID(jAudioTrackClass, "<init>", "(IIIIII)V");

    jmethodID minBufferSizeMid = env->GetStaticMethodID(jAudioTrackClass, "getMinBufferSize",
                                                        "(III)I");
    bufferSizeInBytes = env->CallStaticIntMethod(jAudioTrackClass, minBufferSizeMid, sampleRateInHz,
                                                 channelConfig,
                                                 audioFormat);


    jobject jAudioTrackObj = env->NewObject(jAudioTrackClass, jAudioTrackMid, streamType,
                                            sampleRateInHz, channelConfig, audioFormat,
                                            bufferSizeInBytes, mode);
    jmethodID playMid = env->GetMethodID(jAudioTrackClass, "play", "()V");
    env->CallVoidMethod(jAudioTrackObj, playMid);
    return jAudioTrackObj;
}

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
    char *url = "http://file.kuyinyun.com/group1/M00/90/B7/rBBGdFPXJNeAM-nhABeMElAM6bY151.mp3";
    av_register_all();
    avformat_network_init();
    AVFormatContext *avFormatContext = NULL;
    int formatOPenInputRes = 0;
    int formatFindStreamInfoRes = 0;
    int audioStreamIndex = -1;
    AVCodecParameters *avCodecParameters = NULL;
    AVCodec *avCodec = NULL;
    AVCodecContext *avCodecContext = NULL;
    int result = 0;
    AVFrame *avFrame = NULL;
    AVPacket *avPacket = NULL;
    jobject audioTrack = NULL;
    jclass jAudioTrackClass = NULL;
    jmethodID jwriteMid = NULL;


    formatOPenInputRes = avformat_open_input(&avFormatContext, url, NULL, NULL);
    if (formatOPenInputRes != 0) {
        //回调
        //释放资源
        LOGE("avformat_open_input%s", av_err2str(formatOPenInputRes));
        //发现变量的声明必须放在任何goto前面， 不能再goto后边声明变量
        return;
    }

    formatFindStreamInfoRes = avformat_find_stream_info(avFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        LOGE("avformat_find_stream_info%s", av_err2str(formatFindStreamInfoRes));
        return;
    }

    audioStreamIndex = av_find_best_stream(avFormatContext, AVMediaType::AVMEDIA_TYPE_AUDIO, -1, -1,
                                           NULL, 0);
    if (audioStreamIndex < 0 || audioStreamIndex == AVERROR_STREAM_NOT_FOUND) {
        return;
    }

    avCodecParameters = avFormatContext->streams[audioStreamIndex]->codecpar;

    avCodec = avcodec_find_decoder(avCodecParameters->codec_id);
    if (avCodec == NULL) {
        return;
    }

    avCodecContext = avcodec_alloc_context3(avCodec);
    result = avcodec_open2(avCodecContext, avCodec, NULL);
    if (result < 0) {
        LOGE("avcodec_open2%s", av_err2str(result));
        return;
    }
    LOGE("sample_rate %d", avCodecParameters->sample_rate);

    avPacket = av_packet_alloc();
    avFrame = av_frame_alloc();

    //重采样 44100 -》 xxxx
//    int64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
//    enum AVSampleFormat out_sample_fmt = AVSampleFormat::AV_SAMPLE_FMT_S16;
//    int out_sample_rate = AUDIO_SAMPLE_RATE;
//    int64_t in_ch_layout = avCodecContext->channel_layout;
//    enum AVSampleFormat in_sample_fmt = avCodecContext->sample_fmt;
//    int in_sample_rate = avCodecContext->sample_rate;
//    SwrContext* swrContext = swr_alloc_set_opts(NULL, out_ch_layout, out_sample_fmt,
//                                     out_sample_rate, in_ch_layout, in_sample_fmt, in_sample_rate, 0, NULL);
//    if (swrContext == NULL) {
//        // 提示错误
//        LOGE("%s","swrContext == NULL");
//        return;
//    }
//    int swrInitRes = swr_init(swrContext);
//    if (swrInitRes < 0) {
//        LOGE("%s",av_err2str(swrInitRes));
//        return;
//    }

//    int channels = av_get_channel_layout_nb_channels(out_ch_layout);

    audioTrack = initCreateAudioTrack(env);
    int size = av_samples_get_buffer_size(NULL, avCodecParameters->channels, avCodecParameters->frame_size,
                                         avCodecContext->sample_fmt, 0);
    jbyteArray jPcmArr = env->NewByteArray(size);
    jbyte* jPcmBytes = env->GetByteArrayElements(jPcmArr, NULL);
    uint8_t * outBuff = static_cast<uint8_t *>(malloc(size));

    while (av_read_frame(avFormatContext, avPacket) == 0) {
        LOGE("解码：%d",0);
        if (avPacket->stream_index == audioStreamIndex) {
            LOGE("解码：%d",avPacket->stream_index);
            int res = avcodec_send_packet(avCodecContext, avPacket);
            LOGE("解码avcodec_send_packet：%s", av_err2str(res));
            if (res == 0) {
                int res = avcodec_receive_frame(avCodecContext, avFrame);
                LOGE("解码avcodec_receive_frame：成功%d", res);
                //write
                //重采样
//                swr_convert(swrContext, &outBuff, avFrame->nb_samples,
//                            (const uint8_t **)avFrame->data, avFrame->nb_samples);


                memcpy(jPcmBytes, outBuff, size);
                //同步，不释放内存
                env->ReleaseByteArrayElements(jPcmArr, jPcmBytes, JNI_COMMIT);
                jAudioTrackClass  = env->FindClass("android/media/AudioTrack");
                jwriteMid = env->GetMethodID(jAudioTrackClass,"write","([BII)I");
                env->CallIntMethod(audioTrack,jwriteMid,jPcmArr,0,size);
            }
        }
        //解引用，原来指向的内存释放掉
        av_frame_unref(avFrame);
        av_packet_unref(avPacket);
    }
    av_frame_free(&avFrame);
    av_packet_free(&avPacket);
    env->DeleteLocalRef(audioTrack);
    env->DeleteLocalRef(jPcmArr);


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