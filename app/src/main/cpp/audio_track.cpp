//
// Created by 海盗的帽子 on 2020/6/10.
//

#include "audio_track.h"
#include "golbal_define.h"

AudioTrack::AudioTrack(JavaVM *javaVm, JNIEnv *jniEnv) : javaVm(javaVm),jniEnv(jniEnv){
    initCreateAudioTrack();
//    jclass playClazz = jniEnv->GetObjectClass(jPlayObj);
//    jErrorMid = jniEnv->GetMethodID(playClazz, "onError", "(ILjava/lang/String;)V");
}

AudioTrack::~AudioTrack() {
    LOGE("%s","DeleteLocalRef");
    jniEnv->DeleteGlobalRef(jAudioTrackObj);
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
    bufferSizeInBytes = jniEnv->CallStaticIntMethod(jAudioTrackClass, minBufferSizeMid,
                                                    sampleRateInHz,
                                                    channelConfig,
                                                    audioFormat);
//大多数JNI函数会创建局部引用。例如，NewObject创建一个新的对象实例并返回一个对这个对象的局部引用。
//
//局部引用只有在创建它的本地方法返回前有效。本地方法返回后，局部引用会被自动释放。

    jobject  obj = jniEnv->NewObject(jAudioTrackClass, jAudioTrackMid, streamType,
                                       sampleRateInHz, channelConfig, audioFormat,
                                       bufferSizeInBytes, mode);
    jAudioTrackObj = jniEnv->NewGlobalRef(obj);


    //start method
    jmethodID playMid = jniEnv->GetMethodID(jAudioTrackClass, "play", "()V");
    jniEnv->CallVoidMethod(jAudioTrackObj, playMid);
    LOGE("%s","jAudioTrackObj");
}

void AudioTrack::callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes,bool main) {
//write method

    jclass jAudioTrackClass = jniEnv->FindClass("android/media/AudioTrack");
    jAudioTrackWriteMid = jniEnv->GetMethodID(jAudioTrackClass, "write", "([BII)I");
    LOGE("%s","jAudioTrackWriteMid");
    LOGE("CallIntMethod");
    jniEnv->CallIntMethod(jAudioTrackObj, jAudioTrackWriteMid, audioData, offsetInBytes, sizeInBytes);
    LOGE("CallIntMethod jAudioTrackObj");


}

void AudioTrack::onErrorCallback(int code, char *msg, bool main) {
    //子线程回调
//    JNIEnv *env;
//    if (!main) {
//        if (javaVm->AttachCurrentThread(&env, 0) != JNI_OK) {
//            return;
//        }
//    } else {
//        env = jniEnv;
//    }  LOGE("%d",111);
////    jstring jMsg = jniEnv->NewStringUTF(msg);
////    LOGE("%d",222);
////    jniEnv->CallVoidMethod(jPlayObj, jErrorMid, code, jMsg);
////    LOGE("%d",333);
//

//    jniEnv->DeleteLocalRef(jMsg);
//    if (!main) {
//        javaVm->DetachCurrentThread();
//    }
}


