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
#include "audio_track.h"
#include "audio_ffmpeg.h"

AudioTrack *audioTrack = NULL;
AudioFFmpeg *audioFFmpeg = NULL;

JavaVM *pjavaVm;

//so 被加载的时候调用的方法
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *reserved) {
    pjavaVm = javaVm;
    JNIEnv* env;
    if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

AudioTrack* getAudioTrack(JNIEnv* env) {
    if (audioTrack == NULL) {
        audioTrack = new AudioTrack(pjavaVm, env);
    }
    return audioTrack;
}

AudioFFmpeg* getAudioFFmpeg(JNIEnv* env,jobject thiz) {
    if (audioFFmpeg == NULL) {
        audioFFmpeg = new AudioFFmpeg(getAudioTrack(env),pjavaVm);
    }
    return audioFFmpeg;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePlay(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env,thiz)->play();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepare(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env,thiz)->prepare();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepareAsync(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env,thiz)->prepareAsync();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeRelease(JNIEnv *env, jobject thiz) {
    getAudioFFmpeg(env,thiz)->release();
   delete audioFFmpeg;
   delete audioTrack;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeSetDataSource(JNIEnv *env, jobject thiz, jstring url) {
    getAudioFFmpeg(env,thiz)->setDataSource(env->GetStringUTFChars(url,NULL));
}