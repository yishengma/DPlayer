#include <jni.h>
#include <string>

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/avutil.h"
#include "libavfilter/avfilter.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libswresample/swresample.h"
#include "libavutil/channel_layout.h"

}

#include "golbal_define.h"
#include "audio_track.h"
#include "audio_ffmpeg.h"

AudioTrack* audioTrack;
AudioFFmpeg* audioFFmpeg;



extern "C" JNIEXPORT jstring JNICALL
Java_com_example_dplayer_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {

    av_register_all();
    avformat_network_init();
    avformat_network_deinit();
    const char *info = avcodec_configuration();
    return env->NewStringUTF(info);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePlay(JNIEnv *env, jobject thiz) {
    audioTrack = new AudioTrack(NULL,env,thiz);
    const char *url = "http://file.kuyinyun.com/group1/M00/90/B7/rBBGdFPXJNeAM-nhABeMElAM6bY151.mp3";
    audioFFmpeg = new AudioFFmpeg(audioTrack,url);
    audioFFmpeg->play();
    delete audioTrack;
    delete audioFFmpeg;
}