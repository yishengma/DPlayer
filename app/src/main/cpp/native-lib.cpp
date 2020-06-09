#include <jni.h>
#include <string>
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include "libavfilter/avfilter.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"

}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_dplayer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    av_register_all();
    const char * info = avcodec_configuration();
    return env->NewStringUTF(info);
}
