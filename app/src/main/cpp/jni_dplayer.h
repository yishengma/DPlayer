//
// Created by 海盗的帽子 on 2020/6/11.
//

#ifndef DPLAYER_JNI_DPLAYER_H
#define DPLAYER_JNI_DPLAYER_H

#include <jni.h>
#include "golbal_define.h"

class JNIDPlayer {
public:
    JavaVM *javaVm = NULL;
    JNIEnv *jniEnv = NULL;
    jmethodID jPlayerErrorMid;
    jmethodID jPlayerPreparedMid;
    jobject jPlayObj;
public:
    JNIDPlayer(JavaVM *javaVm, JNIEnv *jniEnv, jobject jPlayObj);

    virtual ~JNIDPlayer();

public:
    void callPlayerError(ThreadMode mode, int code, char *msg);

    void callPlayerPrepared(ThreadMode mode);

};


#endif //DPLAYER_JNI_DPLAYER_H
