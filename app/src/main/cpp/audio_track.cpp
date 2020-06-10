//
// Created by 海盗的帽子 on 2020/6/10.
//

#include "audio_track.h"
#include "golbal_define.h"

AudioTrack::AudioTrack(JavaVM *javaVm, JNIEnv *jniEnv) : javaVm(javaVm), jniEnv(jniEnv) {
    initCreateAudioTrack();
}

AudioTrack::~AudioTrack() {
    jniEnv->DeleteLocalRef(jAudioTrackObj);
}

void AudioTrack::initCreateAudioTrack() {
    int streamType = 3;
    int sampleRateInHz = AUDIO_SAMPLE_RATE;
    int channelConfig = (0x4 | 0x8);
    int audioFormat = 2;
    int bufferSizeInBytes = 0;
    int mode = 1;
    jclass jAudioTrackClass = jniEnv->FindClass("android/media/AudioTrack");
    jmethodID jAudioTrackMid = jniEnv->GetMethodID(jAudioTrackClass, "<init>", "(IIIIII)V");

    jmethodID minBufferSizeMid = jniEnv->GetStaticMethodID(jAudioTrackClass, "getMinBufferSize",
                                                        "(III)I");
    bufferSizeInBytes = jniEnv->CallStaticIntMethod(jAudioTrackClass, minBufferSizeMid, sampleRateInHz,
                                                 channelConfig,
                                                 audioFormat);


    jAudioTrackObj = jniEnv->NewObject(jAudioTrackClass, jAudioTrackMid, streamType,
                                            sampleRateInHz, channelConfig, audioFormat,
                                            bufferSizeInBytes, mode);

    //start method
    jmethodID playMid = jniEnv->GetMethodID(jAudioTrackClass, "play", "()V");
    jniEnv->CallVoidMethod(jAudioTrackObj, playMid);


    //write method
    jAudioTrackWriteMid = jniEnv->GetMethodID(jAudioTrackClass,"write","([BII)I");
}

void AudioTrack::callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes) {
    jniEnv->CallIntMethod(jAudioTrackObj,jAudioTrackWriteMid,audioData,offsetInBytes,sizeInBytes);
}

