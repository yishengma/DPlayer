//
// Created by 海盗的帽子 on 2020/6/10.
//

#ifndef DPLAYER_AUDIO_TRACK_H
#define DPLAYER_AUDIO_TRACK_H

#include <jni.h>

class AudioTrack {
public:
    jobject jAudioTrackObj;
    jmethodID jAudioTrackWriteMid;
    JavaVM *javaVm = NULL;
    JNIEnv *jniEnv = NULL;
//    jobject jPlayObj;不能保存外面传进来的 jobject

//    jmethodID jErrorMid;
public:

    AudioTrack(JavaVM *javaVm, JNIEnv *jniEnv);

    virtual ~AudioTrack();

private:
    void initCreateAudioTrack();

public:
    void callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes, bool main);
    void onErrorCallback(int code,char* msg, bool main);
};

#endif //DPLAYER_AUDIO_TRACK_H
