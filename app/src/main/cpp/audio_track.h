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
public:
    AudioTrack(JavaVM *javaVm, JNIEnv *jniEnv);

    virtual ~AudioTrack();

private:
    void initCreateAudioTrack();

public:
    void callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes);
};


#endif //DPLAYER_AUDIO_TRACK_H
