//
// Created by 海盗的帽子 on 2020/6/9.
//


#ifndef DPLAYER_GOLBAL_DEFINE_H
#define DPLAYER_GOLBAL_DEFINE_H
#include <android/log.h>
#define TAG "JNI_TAG"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#define AUDIO_SAMPLE_RATE 44100


#define FUNC_CPY_JSTRING(name,len,jstr) \
	char name[len+1] = {0}; \
	if(jstr != NULL)\
	{ \
		const char* __tmp_dn__  = env->GetStringUTFChars(jstr, NULL);\
		strncpy(name,__tmp_dn__,len);\
		env->ReleaseStringUTFChars(jstr,__tmp_dn__);\
	}


#endif //DPLAYER_GOLBAL_DEFINE_H
