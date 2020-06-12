#include <jni.h>
#include <string>
#include "jni_dplayer.h"
#include "dplayer_ffmpeg.h"
//adb logcat | ndk-stack -sym app/build/intermediates/cmake/debug/obj/armeabi
//fau adr 0x0 空指针


JavaVM *pJavaVM = NULL;
JNIDPlayer *jniDPlayer;
DPlayerFFmpeg* playerFFmpeg;
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
        playerFFmpeg = new DPlayerFFmpeg(url,jniDPlayer);
        playerFFmpeg->prepare();
    }
    env->ReleaseStringUTFChars(_url,url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepareAsync(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    if (playerFFmpeg == NULL) {
        jniDPlayer = new JNIDPlayer(pJavaVM, env, thiz);
        playerFFmpeg = new DPlayerFFmpeg(url,jniDPlayer);
        playerFFmpeg->prepareAsync();
    }
    env->ReleaseStringUTFChars(_url,url);
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