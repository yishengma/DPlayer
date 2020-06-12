//
// Created by 海盗的帽子 on 2020/6/11.
//

#include "dplayer_audio.h"
#include "../libs/include/libavformat/avformat.h"

DPlayerAudio::DPlayerAudio
        (AVFormatContext *avFormatContext,
         JNIDPlayer *jniDPlayer,
         int audioStreamIndex)
        : avFormatContext(avFormatContext),
          jniDPlayer(jniDPlayer),
          audioStreamIndex(audioStreamIndex) {
    LOGE("%s","DPlayerAudio");
    packetQueue = new PacketQueue();
    LOGE("%s","packetQueue");

    status = true;
}

DPlayerAudio::~DPlayerAudio() {
    release();
}

void *runOpenSLES(void *context) {
    DPlayerAudio *playerAudio = (DPlayerAudio *) context;
    playerAudio->initCreateOpenSLES();
    return 0;
}

void *runReadPacket(void *context) {
    DPlayerAudio *playerAudio = (DPlayerAudio *) context;
    while (playerAudio->status) {
        AVPacket *packet = av_packet_alloc();
        if (av_read_frame(playerAudio->avFormatContext, packet) == 0) {
            if (packet->stream_index == playerAudio->audioStreamIndex) {
                playerAudio->packetQueue->push(packet);
            } else {
                av_packet_free(&packet);
            }
        } else {
            av_packet_free(&packet);
        }
    }
    return 0;
}

void executePlay(SLAndroidSimpleBufferQueueItf caller, void *context) {
    DPlayerAudio *playerAudio = (DPlayerAudio *) context;
    int dataSize = playerAudio->resampleAudio();
    (*caller)->Enqueue(caller, playerAudio->resampleBuffer, dataSize);
}

void DPlayerAudio::play() {
    pthread_t readThread;
    pthread_create(&readThread, NULL, runReadPacket, NULL);
    pthread_detach(readThread);

    pthread_t initThread;
    pthread_create(&initThread, NULL, runOpenSLES, NULL);
    pthread_detach(initThread);
}

void DPlayerAudio::initCreateOpenSLES() {
    SLObjectItf engineObject = NULL;
    SLEngineItf engineEngine;
    slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    // realize the engine
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    // get the engine interface, which is needed in order to create other objects
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    // 3.2 设置混音器
    static SLObjectItf outputMixObject = NULL;
    const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, ids, req);
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;
    (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                     &outputMixEnvironmentalReverb);
    SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;
    (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb,
                                                                      &reverbSettings);
    // 3.3 创建播放器
    SLObjectItf pPlayer = NULL;
    SLPlayItf pPlayItf = NULL;
    SLDataLocator_AndroidSimpleBufferQueue simpleBufferQueue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM formatPcm = {
            SL_DATAFORMAT_PCM,
            2,
            SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN};
    SLDataSource audioSrc = {&simpleBufferQueue, &formatPcm};
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&outputMix, NULL};
    SLInterfaceID interfaceIds[3] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME, SL_IID_PLAYBACKRATE};
    SLboolean interfaceRequired[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    (*engineEngine)->CreateAudioPlayer(engineEngine, &pPlayer, &audioSrc, &audioSnk, 3,
                                       interfaceIds, interfaceRequired);
    (*pPlayer)->Realize(pPlayer, SL_BOOLEAN_FALSE);
    (*pPlayer)->GetInterface(pPlayer, SL_IID_PLAY, &pPlayItf);
    // 3.4 设置缓存队列和回调函数
    SLAndroidSimpleBufferQueueItf playerBufferQueue;
    (*pPlayer)->GetInterface(pPlayer, SL_IID_BUFFERQUEUE, &playerBufferQueue);
    // 每次回调 this 会被带给 playerCallback 里面的 context
    (*playerBufferQueue)->RegisterCallback(playerBufferQueue, executePlay, this);
    // 3.5 设置播放状态
    (*pPlayItf)->SetPlayState(pPlayItf, SL_PLAYSTATE_PLAYING);
    // 3.6 调用回调函数
    executePlay(playerBufferQueue, NULL);
}

int DPlayerAudio::resampleAudio() {
    int dataSize = 0;
    AVPacket *pPacket = NULL;
    AVFrame *pFrame = av_frame_alloc();

    while (status) {
        pPacket = packetQueue->pop();
        // Packet 包，压缩的数据，解码成 pcm 数据
        int codecSendPacketRes = avcodec_send_packet(avCodecContext, pPacket);
        if (codecSendPacketRes == 0) {
            int codecReceiveFrameRes = avcodec_receive_frame(avCodecContext, pFrame);
            if (codecReceiveFrameRes == 0) {
                dataSize = swr_convert(swrContext, &resampleBuffer, pFrame->nb_samples,
                                       (const uint8_t **) pFrame->data, pFrame->nb_samples);
                dataSize = dataSize * 2 * 2;
                break;
            }
        }
        av_packet_unref(pPacket);
        av_frame_unref(pFrame);
    }
    av_packet_free(&pPacket);
    av_frame_free(&pFrame);
    return dataSize;
}

void DPlayerAudio::analysisStream(ThreadMode mode, AVStream **avStream) {
    LOGE("%s","analysisStream");

// 查找解码
    AVCodecParameters *pCodecParameters = avFormatContext->streams[audioStreamIndex]->codecpar;
    AVCodec *pCodec = avcodec_find_decoder(pCodecParameters->codec_id);
    if (pCodec == NULL) {
        LOGE("codec find audio decoder error");
        callPlayerJniError(mode, CODEC_FIND_DECODER_ERROR_CODE,
                           "codec find audio decoder error");
        return;
    }
    LOGE("%s","avcodec_alloc_context3");

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
    LOGE("%s","codecParametersToContextRes");

    int codecOpenRes = avcodec_open2(avCodecContext, pCodec, NULL);
    if (codecOpenRes != 0) {
        LOGE("codec audio open error: %s", av_err2str(codecOpenRes));
        callPlayerJniError(mode, codecOpenRes, av_err2str(codecOpenRes));
        return;
    }
    LOGE("%s","start");

    // ---------- 重采样 start ----------
    int64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_sample_fmt = AVSampleFormat::AV_SAMPLE_FMT_S16;
    int out_sample_rate = AUDIO_SAMPLE_RATE;
    int64_t in_ch_layout = avCodecContext->channel_layout;
    enum AVSampleFormat in_sample_fmt = avCodecContext->sample_fmt;
    int in_sample_rate = avCodecContext->sample_rate;
    swrContext = swr_alloc_set_opts(NULL, out_ch_layout, out_sample_fmt,
                                     out_sample_rate, in_ch_layout, in_sample_fmt, in_sample_rate, 0, NULL);
    if (swrContext == NULL) {
        // 提示错误
        callPlayerJniError(mode, SWR_ALLOC_SET_OPTS_ERROR_CODE, "swr alloc set opts error");
        return;
    }
    int swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        callPlayerJniError(mode, SWR_CONTEXT_INIT_ERROR_CODE, "swr context swr init error");
        return;
    }

    resampleBuffer = (uint8_t *) malloc(avCodecContext->frame_size * 2 * 2);
}

void DPlayerAudio::callPlayerJniError(ThreadMode mode, int code, char *msg) {
    release();
    jniDPlayer->callPlayerError(mode, code, msg);
}

void DPlayerAudio::release() {
    if (packetQueue != NULL) {
        delete packetQueue;
        packetQueue = NULL;
    }
    if (resampleBuffer != NULL) {
        delete resampleBuffer;
        resampleBuffer = NULL;
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
}
