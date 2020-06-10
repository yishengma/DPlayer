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
    JavaVM *javaVm;
    JNIEnv *jniEnv;
    jobject jPlayObj;

    jmethodID jErrorMid;
public:

    AudioTrack(JavaVM *javaVm, JNIEnv *jniEnv,jobject playObj);

    virtual ~AudioTrack();

private:
    void initCreateAudioTrack();

public:
    void callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes);
    void onErrorCallback(int code,char* msg);

};


#endif //DPLAYER_AUDIO_TRACK_H
