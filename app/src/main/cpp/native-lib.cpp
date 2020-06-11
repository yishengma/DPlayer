#include <jni.h>
#include <string>
//adb logcat | ndk-stack -sym app/build/intermediates/cmake/debug/obj/armeabi
//fau adr 0x0 空指针
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

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "golbal_define.h"
#include "audio_track.h"
#include "audio_ffmpeg.h"

AudioTrack *audioTrack = NULL;
AudioFFmpeg *audioFFmpeg = NULL;

JavaVM *pjavaVm;

//so 被加载的时候调用的方法
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *reserved) {
    pjavaVm = javaVm;
    JNIEnv *env;
    if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

AudioTrack *getAudioTrack(JNIEnv *env) {
    if (audioTrack == NULL) {
        audioTrack = new AudioTrack(pjavaVm, env);
    }
    return audioTrack;
}

AudioFFmpeg *getAudioFFmpeg(JNIEnv *env, jobject thiz) {
    if (audioFFmpeg == NULL) {
        audioFFmpeg = new AudioFFmpeg(getAudioTrack(env), pjavaVm);
    }
    return audioFFmpeg;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePlay(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env, thiz)->play();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepare(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env, thiz)->prepare();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepareAsync(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env, thiz)->prepareAsync();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeRelease(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env, thiz)->release();
    delete audioFFmpeg;
    delete audioTrack;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeSetDataSource(JNIEnv *env, jobject thiz, jstring url) {
    getAudioFFmpeg(env, thiz)->setDataSource(env->GetStringUTFChars(url, NULL));
}
FILE* pcmFile = NULL;
void * buffer = NULL;

void playerCallback(SLAndroidSimpleBufferQueueItf caller, void *pContext) {
   if (!feof(pcmFile)) {
       fread(buffer,1,44100 * 2 * 2,pcmFile);
       (*caller)->Enqueue(caller,buffer,44100*2*2);
       LOGE("e%s","Enqueue");

   } else{
       fclose(pcmFile);
       free(buffer);
       LOGE("e%s","pcmFile");
   }

}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DOpenSLES_playPcm(JNIEnv *env, jobject thiz) {
    const char * path = "/storage/emulated/0/record/encode.pcm";
    pcmFile = fopen(path,"r");
    buffer = malloc(44100*2*2);
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
    (*playerBufferQueue)->RegisterCallback(playerBufferQueue, playerCallback, NULL);
    // 3.5 设置播放状态
    (*pPlayItf)->SetPlayState(pPlayItf, SL_PLAYSTATE_PLAYING);
    // 3.6 调用回调函数
    playerCallback(playerBufferQueue, NULL);
}