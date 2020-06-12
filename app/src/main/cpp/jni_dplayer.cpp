//
// Created by 海盗的帽子 on 2020/6/11.
//

#include "jni_dplayer.h"

JNIDPlayer::~JNIDPlayer() {
    this->jniEnv->DeleteGlobalRef(jPlayObj);
}

JNIDPlayer::JNIDPlayer(JavaVM *javaVm, JNIEnv *jniEnv, const jobject jPlayObj) : javaVm(javaVm),
                                                                                 jniEnv(jniEnv) {
    this->jPlayObj = this->jniEnv->NewGlobalRef(jPlayObj);
    jclass clazz = this->jniEnv->GetObjectClass(jPlayObj);
    //javap -p -s xx.class
    jPlayerErrorMid = this->jniEnv->GetMethodID(clazz, "onError", "(ILjava/lang/String;)V");
    jPlayerPreparedMid = this->jniEnv->GetMethodID(clazz, "onPrepared", "()V");

}

void JNIDPlayer::callPlayerError(ThreadMode mode, int code, char *msg) {
    if (mode == ThreadMode::MAIN) {
        jstring jMsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jPlayObj, jPlayerErrorMid, code, msg);
        jniEnv->DeleteLocalRef(jMsg);
    }
    if (mode == ThreadMode::SUB) {
        JNIEnv* env;
        if (javaVm->AttachCurrentThread(&env,0) != JNI_OK) {
            return;
        }
        jstring jMsg = env->NewStringUTF(msg);
        env->CallVoidMethod(jPlayObj,jPlayerErrorMid);
        env->DeleteLocalRef(jMsg);
        javaVm->DetachCurrentThread();
    }
}

void JNIDPlayer::callPlayerPrepared(ThreadMode mode) {
    if (mode == ThreadMode::MAIN) {
        jniEnv->CallVoidMethod(jPlayObj, jPlayerPreparedMid);
    }
    if (mode == ThreadMode::SUB) {
        JNIEnv* env;
        if (javaVm->AttachCurrentThread(&env,0) != JNI_OK) {
            return;
        }
        env->CallVoidMethod(jPlayObj,jPlayerPreparedMid);
        javaVm->DetachCurrentThread();
    }
}

