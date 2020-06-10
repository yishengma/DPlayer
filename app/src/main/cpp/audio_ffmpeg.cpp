//
// Created by 海盗的帽子 on 2020/6/10.
//

#include "audio_ffmpeg.h"
#include "golbal_define.h"
#include <pthread.h>

AudioFFmpeg::AudioFFmpeg(AudioTrack *track, JavaVM *pjavaVM):audioTrack(track),javaVm(pjavaVM) {


}

AudioFFmpeg::~AudioFFmpeg() {
    release();
}

void *threadPlay(void *context) {
    AudioFFmpeg *fFmpeg = (AudioFFmpeg *) context;
    fFmpeg->playFFmpeg();
}

void AudioFFmpeg::play() {
    if (prepareMode == ASYNC) {
        pthread_t pthreadId;
        pthread_create(&pthreadId, NULL, threadPlay, this);
        pthread_detach(pthreadId);
    }
    if (prepareMode == SYNC) {
        playFFmpeg();
    }
}

void AudioFFmpeg::onErrorCallback(int code, char *msg) {
    audioTrack->onErrorCallback(code, msg, prepareMode == SYNC);
}

void AudioFFmpeg::release() {
    if (avFormatContext != NULL) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }
    if (avCodecContext != NULL) {
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }
    if (swrContext != NULL) {
        swr_close(swrContext);
        swr_free(&swrContext);
        swrContext = NULL;
    }

    if (buffer != NULL) {
        delete buffer;
        buffer = NULL;
    }
}

void AudioFFmpeg::prepareAsync() {
    prepareMode = PrepareMode::ASYNC;
    prepareFFmpeg();
}

void AudioFFmpeg::prepare() {
    prepareMode = PrepareMode::SYNC;
    prepareFFmpeg();
}

void AudioFFmpeg::setDataSource(const char *url) {
    if (this->url != NULL) {
        free(this->url);
        this->url = NULL;
    }

    this->url = static_cast<char *>(malloc(strlen(url) + 1));
    strcpy(this->url, url);
}

void AudioFFmpeg::prepareFFmpeg() {
    LOGE("%s", "AudioFFmpeg::prepareFFmpeg");

    av_register_all();
    avformat_network_init();
    int formatOPenInputRes = 0;
    int formatFindStreamInfoRes = 0;
    int result = 0;


//    onErrorCallback(0, "play Success");
    formatOPenInputRes = avformat_open_input(&avFormatContext, url, NULL, NULL);
    if (formatOPenInputRes != 0) {
        //回调
        //释放资源
        LOGE("avformat_open_input%s", av_err2str(formatOPenInputRes));
        //发现变量的声明必须放在任何goto前面， 不能再goto后边声明变量
        onErrorCallback(formatFindStreamInfoRes, av_err2str(formatOPenInputRes));
        return;
    }

    formatFindStreamInfoRes = avformat_find_stream_info(avFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        LOGE("avformat_find_stream_info%s", av_err2str(formatFindStreamInfoRes));
        onErrorCallback(formatFindStreamInfoRes, av_err2str(formatOPenInputRes));

        return;
    }

    audioStreamId = av_find_best_stream(avFormatContext, AVMediaType::AVMEDIA_TYPE_AUDIO, -1, -1,
                                        NULL, 0);
    if (audioStreamId < 0 || audioStreamId == AVERROR_STREAM_NOT_FOUND) {
        onErrorCallback(-1, "audioStreamIndex < 0 || audioStreamIndex == AVERROR_STREAM_NOT_FOUND");

        return;
    }

    avCodecParameters = avFormatContext->streams[audioStreamId]->codecpar;

    avCodec = avcodec_find_decoder(avCodecParameters->codec_id);
    if (avCodec == NULL) {
        onErrorCallback(-1, "avCodec == NULL");
        return;
    }

    avCodecContext = avcodec_alloc_context3(avCodec);
    result = avcodec_open2(avCodecContext, avCodec, NULL);
    if (result < 0) {
        LOGE("avcodec_open2%s", av_err2str(result));
        onErrorCallback(result, "av_err2str(result)");

        return;
    }
    avPacket = av_packet_alloc();
    avFrame = av_frame_alloc();

    //重采样 44100 -》 xxxx
    int64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_sample_fmt = AVSampleFormat::AV_SAMPLE_FMT_S16;
    int out_sample_rate = AUDIO_SAMPLE_RATE;
    int64_t in_ch_layout = avCodecParameters->channel_layout;
    enum AVSampleFormat in_sample_fmt = avCodecContext->sample_fmt;
    int in_sample_rate = avCodecParameters->sample_rate;
    swrContext = swr_alloc_set_opts(NULL, out_ch_layout, out_sample_fmt,
                                    out_sample_rate, in_ch_layout, in_sample_fmt, in_sample_rate, 0,
                                    NULL);
    LOGE("%s", "pre swr_alloc_set_opts");

    if (swrContext == NULL) {
        // 提示错误
        LOGE("%s", "swrContext == NULL");
        onErrorCallback(-1, "swrContext == NULL");

        return;
    }
    LOGE("%s", "pre swr_init");

    int swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        LOGE("%s", av_err2str(swrInitRes));
        onErrorCallback(swrInitRes, av_err2str(swrInitRes));

        return;
    }

    bufferSize = av_samples_get_buffer_size(NULL, avCodecParameters->channels,
                                            avCodecParameters->frame_size,
                                            avCodecContext->sample_fmt, 0);
    onErrorCallback(-1, "onErrorCallback");

    LOGE("%s", "pre done");


}

void AudioFFmpeg::playFFmpeg() {
    JNIEnv *env = audioTrack->jniEnv;
//    if (javaVm->AttachCurrentThread(reinterpret_cast<JNIEnv **>(&env), 0) != JNI_OK) {
//        return;
//    }
    jbyteArray jPcmArr = env->NewByteArray(bufferSize);
    jbyte *jPcmBytes = env->GetByteArrayElements(jPcmArr, NULL);
    buffer = static_cast<uint8_t *>(malloc(bufferSize));
    while (av_read_frame(avFormatContext, avPacket) == 0) {
        if (avPacket->stream_index == audioStreamId) {
            int res = avcodec_send_packet(avCodecContext, avPacket);
            if (res == 0) {
                int res = avcodec_receive_frame(avCodecContext, avFrame);
                //write
                //重采样
                bufferSize = swr_convert(swrContext, &buffer, avFrame->nb_samples,
                                         (const uint8_t **) avFrame->data, avFrame->nb_samples);

                //这个要加，不加会声音变慢，而且重音
                bufferSize = bufferSize * 2 * 2;
                LOGE("%s", "playFFmpeg2");

                memcpy(jPcmBytes, buffer, bufferSize);
                //同步，不释放内存
                LOGE("%s", "playFFmpeg3");

                env->ReleaseByteArrayElements(jPcmArr, jPcmBytes, JNI_COMMIT);
                audioTrack->callAudioTrackWrite(jPcmArr, 0, bufferSize, prepareMode == SYNC);
            }
        }
        //解引用，原来指向的内存释放掉
        av_frame_unref(avFrame);
        av_packet_unref(avPacket);
    }
    av_frame_free(&avFrame);
    av_packet_free(&avPacket);
    //回收
    env->ReleaseByteArrayElements(jPcmArr, jPcmBytes, 0);
    env->DeleteLocalRef(jPcmArr);
//    javaVm->DetachCurrentThread();

}


