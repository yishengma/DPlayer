//
// Created by 海盗的帽子 on 2020/6/10.
//

#include "audio_ffmpeg.h"
#include "golbal_define.h"

AudioFFmpeg::AudioFFmpeg(AudioTrack *audioTrack, const char *url) {
    this->audioTrack = audioTrack;
    this->url = url;
}

AudioFFmpeg::~AudioFFmpeg() {
   release();
}

void AudioFFmpeg::play() {
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


    onErrorCallback(0,"play Success");
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

    audioStreamIndex = av_find_best_stream(avFormatContext, AVMediaType::AVMEDIA_TYPE_AUDIO, -1, -1,
                                           NULL, 0);
    if (audioStreamIndex < 0 || audioStreamIndex == AVERROR_STREAM_NOT_FOUND) {
        onErrorCallback(-1, "audioStreamIndex < 0 || audioStreamIndex == AVERROR_STREAM_NOT_FOUND");

        return;
    }

    avCodecParameters = avFormatContext->streams[audioStreamIndex]->codecpar;

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
    SwrContext* swrContext = swr_alloc_set_opts(NULL, out_ch_layout, out_sample_fmt,
                                                out_sample_rate, in_ch_layout, in_sample_fmt, in_sample_rate, 0, NULL);
    if (swrContext == NULL) {
        // 提示错误
        LOGE("%s","swrContext == NULL");
        onErrorCallback(-1, "swrContext == NULL");

        return;
    }

    int swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        LOGE("%s",av_err2str(swrInitRes));
        onErrorCallback(swrInitRes, av_err2str(swrInitRes));

        return;
    }

    int size = av_samples_get_buffer_size(NULL, avCodecParameters->channels, avCodecParameters->frame_size,
                                          avCodecContext->sample_fmt, 0);
    jbyteArray jPcmArr = audioTrack->jniEnv->NewByteArray(size);
    jbyte* jPcmBytes = audioTrack->jniEnv->GetByteArrayElements(jPcmArr, NULL);
    uint8_t * outBuff = static_cast<uint8_t *>(malloc(size));

    while (av_read_frame(avFormatContext, avPacket) == 0) {
        if (avPacket->stream_index == audioStreamIndex) {
            int res = avcodec_send_packet(avCodecContext, avPacket);
            if (res == 0) {
                int res = avcodec_receive_frame(avCodecContext, avFrame);
                //write
                //重采样
                size = swr_convert(swrContext, &outBuff, avFrame->nb_samples,
                                   (const uint8_t **)avFrame->data, avFrame->nb_samples);

                //这个要加，不加会声音变慢，而且重音
                size = size * 2 * 2;

                memcpy(jPcmBytes, outBuff, size);
                //同步，不释放内存
                audioTrack->jniEnv->ReleaseByteArrayElements(jPcmArr, jPcmBytes, JNI_COMMIT);
                audioTrack->callAudioTrackWrite(jPcmArr,0,size);

            }
        }
        //解引用，原来指向的内存释放掉
        av_frame_unref(avFrame);
        av_packet_unref(avPacket);
    }
    av_frame_free(&avFrame);
    av_packet_free(&avPacket);
    //回收
    audioTrack->jniEnv->ReleaseByteArrayElements(jPcmArr, jPcmBytes, 0);
    audioTrack->jniEnv->DeleteLocalRef(jPcmArr);

}

void AudioFFmpeg::onErrorCallback(int code, char *msg) {
    audioTrack->onErrorCallback(code,msg);
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

