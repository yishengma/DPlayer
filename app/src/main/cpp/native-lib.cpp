#include <jni.h>
#include <string>
#include "jni_dplayer.h"
#include "dplayer_ffmpeg.h"
#include "yuvengine.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}
//adb logcat | ndk-stack -sym app/build/intermediates/cmake/debug/obj/armeabi
//fau adr 0x0 空指针


JavaVM *pJavaVM = NULL;
JNIDPlayer *jniDPlayer;
DPlayerFFmpeg *playerFFmpeg;
//
//so 被加载的时候调用的方法
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *reserved) {
    pJavaVM = javaVm;
    JNIEnv *env;
    if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepare(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    if (playerFFmpeg == NULL) {
        jniDPlayer = new JNIDPlayer(pJavaVM, env, thiz);
        playerFFmpeg = new DPlayerFFmpeg(url, jniDPlayer);
        playerFFmpeg->prepare();
    }
    env->ReleaseStringUTFChars(_url, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePrepareAsync(JNIEnv *env, jobject thiz, jstring _url) {
    const char *url = env->GetStringUTFChars(_url, 0);
    if (playerFFmpeg == NULL) {
        jniDPlayer = new JNIDPlayer(pJavaVM, env, thiz);
        playerFFmpeg = new DPlayerFFmpeg(url, jniDPlayer);
        playerFFmpeg->prepareAsync();
    }
    env->ReleaseStringUTFChars(_url, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativePlay(JNIEnv *env, jobject thiz) {
    if (playerFFmpeg != NULL) {
        playerFFmpeg->play();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeRelease(JNIEnv *env, jobject thiz) {
    if (playerFFmpeg != NULL) {
        playerFFmpeg->release();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_DPlayer_nativeSetSurface(JNIEnv *env, jobject thiz, jobject surface) {
    if (playerFFmpeg != NULL) {
        playerFFmpeg->setSurface(surface);
    }
}

////
extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeStartYuvEngine(JNIEnv *env, jclass clazz) {
    YuvEngine *pYuvEngine = new YuvEngine;
    if (pYuvEngine != NULL) {
        return reinterpret_cast<long> (pYuvEngine);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeI420ToYv12(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_i420, jbyteArray p_yv12,
                                                          jint width, jint height) {
    jbyte *jI420 = env->GetByteArrayElements(p_i420, NULL);
    jbyte *jYv12 = env->GetByteArrayElements(p_yv12, NULL);

    unsigned char *pI420 = (unsigned char *) jI420;
    unsigned char *pYv12 = (unsigned char *) jYv12;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->I420ToYv12(pI420, pYv12, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_i420, jI420, 0);
    env->ReleaseByteArrayElements(p_yv12, jYv12, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeYv12ToI420(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_yv12, jbyteArray p_i420,
                                                          jint width, jint height) {
    jbyte *jYv12 = env->GetByteArrayElements(p_yv12, NULL);
    jbyte *jI420 = env->GetByteArrayElements(p_i420, NULL);
    unsigned char *pYv12 = (unsigned char *) jYv12;
    unsigned char *pI420 = (unsigned char *) jI420;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Yv12ToI420(pYv12, pI420, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_yv12, jYv12, 0);
    env->ReleaseByteArrayElements(p_i420, jI420, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv21ToI420(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_nv21, jbyteArray p_i420,
                                                          jint width, jint height) {
    jbyte *jNv21 = env->GetByteArrayElements(p_nv21, NULL);
    jbyte *jI420 = env->GetByteArrayElements(p_i420, NULL);

    unsigned char *pNv21 = (unsigned char *) jNv21;
    unsigned char *pI420 = (unsigned char *) jI420;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv21ToI420(pNv21, pI420, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_nv21, jNv21, 0);
    env->ReleaseByteArrayElements(p_i420, jI420, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeI420ToNv21(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_i420, jbyteArray p_nv21,
                                                          jint width, jint height) {
    jbyte *jI420 = env->GetByteArrayElements(p_i420, NULL);
    jbyte *jNv21 = env->GetByteArrayElements(p_nv21, NULL);

    unsigned char *pI420 = (unsigned char *) jI420;
    unsigned char *pNv21 = (unsigned char *) jNv21;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->I420ToNv21(pI420, pNv21, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_i420, jI420, 0);
    env->ReleaseByteArrayElements(p_nv21, jNv21, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv21ToYV12(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_nv21, jbyteArray p_yv12,
                                                          jint width, jint height) {
    jbyte *jNv21 = env->GetByteArrayElements(p_nv21, NULL);
    jbyte *jYv12 = env->GetByteArrayElements(p_yv12, NULL);

    unsigned char *pNv21 = (unsigned char *) jNv21;
    unsigned char *pYv12 = (unsigned char *) jYv12;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv21ToYv12(pNv21, pYv12, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_nv21, jNv21, 0);
    env->ReleaseByteArrayElements(p_yv12, jYv12, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeYV12ToNv21(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_yv12, jbyteArray p_nv21,
                                                          jint width, jint height) {
    jbyte *jYv12 = env->GetByteArrayElements(p_yv12, NULL);
    jbyte *jNv21 = env->GetByteArrayElements(p_nv21, NULL);

    unsigned char *pYv12 = (unsigned char *) jYv12;
    unsigned char *pNv21 = (unsigned char *) jNv21;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Yv12ToNv21(pYv12, pNv21, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_yv12, jYv12, 0);
    env->ReleaseByteArrayElements(p_nv21, jNv21, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv21ToNv12(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_nv21, jbyteArray p_nv12,
                                                          jint width, jint height) {
    jbyte *jNv21 = env->GetByteArrayElements(p_nv21, NULL);
    jbyte *jNv12 = env->GetByteArrayElements(p_nv12, NULL);

    unsigned char *pNv21 = (unsigned char *) jNv21;
    unsigned char *pNv12 = (unsigned char *) jNv12;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv21ToNv12(pNv21, pNv12, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_nv21, jNv21, 0);
    env->ReleaseByteArrayElements(p_nv12, jNv12, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv12ToNv21(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                          jbyteArray p_nv12, jbyteArray p_nv21,
                                                          jint width, jint height) {
    jbyte *jNv12 = env->GetByteArrayElements(p_nv12, NULL);
    jbyte *jNv21 = env->GetByteArrayElements(p_nv21, NULL);

    unsigned char *pNv12 = (unsigned char *) jNv12;
    unsigned char *pNv21 = (unsigned char *) jNv21;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv12ToNv21(pNv12, pNv21, (int) width, (int) height);
    env->ReleaseByteArrayElements(p_nv12, jNv12, 0);
    env->ReleaseByteArrayElements(p_nv21, jNv21, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeCutCommonYuv(JNIEnv *env, jclass clazz, jlong c_ptr,
                                                            jint yuv_type, jint start_x,
                                                            jint start_y, jbyteArray src_yuv,
                                                            jint src_w, jint src_h,
                                                            jbyteArray tar_yuv, jint cut_w,
                                                            jint cut_h) {
    jbyte *jsrcYuv = env->GetByteArrayElements(src_yuv, NULL);
    jbyte *jtarYuv = env->GetByteArrayElements(tar_yuv, NULL);

    unsigned char *pSrcYuv = (unsigned char *) jsrcYuv;
    unsigned char *pTarYuv = (unsigned char *) jtarYuv;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->cutCommonYuv((int) yuv_type, (int) start_x, (int) start_y, pSrcYuv, (int) src_w,
                            (int) src_h, pTarYuv, (int) cut_w, (int) cut_h);
    env->ReleaseByteArrayElements(src_yuv, jsrcYuv, 0);
    env->ReleaseByteArrayElements(tar_yuv, jtarYuv, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeGetSpecYuvBuffer(JNIEnv *env, jclass clazz,
                                                                jlong c_ptr, jint yuv_type,
                                                                jbyteArray dst_buf,
                                                                jbyteArray src_yuv, jint src_w,
                                                                jint src_h, jint dirty__y,
                                                                jint dirty__uv) {
    jbyte *jdstBuf = env->GetByteArrayElements(dst_buf, NULL);
    jbyte *jsrcYuv = env->GetByteArrayElements(src_yuv, NULL);

    unsigned char *pDstBuf = (unsigned char *) jdstBuf;
    unsigned char *pSrcYuv = (unsigned char *) jsrcYuv;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->getSpecYuvBuffer((int) yuv_type, pDstBuf, pSrcYuv, (int) src_w, (int) src_h,
                                (int) dirty__y, (int) dirty__uv);
    env->ReleaseByteArrayElements(dst_buf, jdstBuf, 0);
    env->ReleaseByteArrayElements(src_yuv, jsrcYuv, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeYuvAddWaterMark(JNIEnv *env, jclass clazz,
                                                               jlong c_ptr, jint yuv_type,
                                                               jint start_x, jint start_y,
                                                               jbyteArray water_mark_data,
                                                               jint water_mark_w, jint water_mark_h,
                                                               jbyteArray yuv_data, jint yuv_w,
                                                               jint yuv_h) {
    jbyte *jwaterMark = env->GetByteArrayElements(water_mark_data, NULL);
    jbyte *jyuv = env->GetByteArrayElements(yuv_data, NULL);

    unsigned char *pWaterMark = (unsigned char *) jwaterMark;
    unsigned char *pYuv = (unsigned char *) jyuv;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->yuvAddWaterMark((int) yuv_type, (int) start_x, (int) start_y, pWaterMark,
                               (int) water_mark_w, (int) water_mark_h, pYuv, (int) yuv_w,
                               (int) yuv_h);
    env->ReleaseByteArrayElements(water_mark_data, jwaterMark, 0);
    env->ReleaseByteArrayElements(yuv_data, jyuv, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv21ClockWiseRotate90(JNIEnv *env, jclass clazz,
                                                                     jlong c_ptr, jbyteArray p_nv21,
                                                                     jint src_width,
                                                                     jint src_height,
                                                                     jbyteArray out_data,
                                                                     jintArray out_width,
                                                                     jintArray out_height) {
    jbyte *jsrcNv21Byte = env->GetByteArrayElements(p_nv21, NULL);
    jbyte *joutDataByte = env->GetByteArrayElements(out_data, NULL);

    jint *joutWidthInt = env->GetIntArrayElements(out_width, NULL);
    jint *joutHeightInt = env->GetIntArrayElements(out_height, NULL);

    int *poutWidth = (int *) joutWidthInt;
    int *poutHeight = (int *) joutHeightInt;

    unsigned char *pSrcNv21 = (unsigned char *) jsrcNv21Byte;
    unsigned char *pOutData = (unsigned char *) joutDataByte;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv21ClockWiseRotate90(pSrcNv21, (int) src_width, (int) src_height, pOutData,
                                     poutWidth, poutHeight);

    env->ReleaseIntArrayElements(out_width, joutWidthInt, 0);
    env->ReleaseIntArrayElements(out_height, joutHeightInt, 0);
    env->ReleaseByteArrayElements(p_nv21, jsrcNv21Byte, 0);
    env->ReleaseByteArrayElements(out_data, joutDataByte, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv12ClockWiseRotate90(JNIEnv *env, jclass clazz,
                                                                     jlong c_ptr, jbyteArray p_nv12,
                                                                     jint src_width,
                                                                     jint src_height,
                                                                     jbyteArray out_data,
                                                                     jintArray out_width,
                                                                     jintArray out_height) {
    jbyte *jsrcNv12Byte = env->GetByteArrayElements(p_nv12, NULL);
    jbyte *joutDataByte = env->GetByteArrayElements(out_data, NULL);

    jint *joutWidthInt = env->GetIntArrayElements(out_width, NULL);
    jint *joutHeightInt = env->GetIntArrayElements(out_height, NULL);

    int *poutWidth = (int *) joutWidthInt;
    int *poutHeight = (int *) joutHeightInt;

    unsigned char *pSrcNv12 = (unsigned char *) jsrcNv12Byte;
    unsigned char *pOutData = (unsigned char *) joutDataByte;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv12ClockWiseRotate90(pSrcNv12, (int) src_width, (int) src_height, pOutData,
                                     poutWidth, poutHeight);

    env->ReleaseIntArrayElements(out_width, joutWidthInt, 0);
    env->ReleaseIntArrayElements(out_height, joutHeightInt, 0);
    env->ReleaseByteArrayElements(p_nv12, jsrcNv12Byte, 0);
    env->ReleaseByteArrayElements(out_data, joutDataByte, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv21ClockWiseRotate180(JNIEnv *env, jclass clazz,
                                                                      jlong c_ptr,
                                                                      jbyteArray p_nv21,
                                                                      jint src_width,
                                                                      jint src_height,
                                                                      jbyteArray out_data,
                                                                      jintArray out_width,
                                                                      jintArray out_height) {
    jbyte *jsrcNv21Byte = env->GetByteArrayElements(p_nv21, NULL);
    jbyte *joutDataByte = env->GetByteArrayElements(out_data, NULL);

    jint *joutWidthInt = env->GetIntArrayElements(out_width, NULL);
    jint *joutHeightInt = env->GetIntArrayElements(out_height, NULL);

    int *poutWidth = (int *) joutWidthInt;
    int *poutHeight = (int *) joutHeightInt;

    unsigned char *pSrcNv21 = (unsigned char *) jsrcNv21Byte;
    unsigned char *pOutData = (unsigned char *) joutDataByte;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv21ClockWiseRotate180(pSrcNv21, (int) src_width, (int) src_height, pOutData,
                                      poutWidth, poutHeight);

    env->ReleaseIntArrayElements(out_width, joutWidthInt, 0);
    env->ReleaseIntArrayElements(out_height, joutHeightInt, 0);
    env->ReleaseByteArrayElements(p_nv21, jsrcNv21Byte, 0);
    env->ReleaseByteArrayElements(out_data, joutDataByte, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeNv21ClockWiseRotate270(JNIEnv *env, jclass clazz,
                                                                      jlong c_ptr,
                                                                      jbyteArray p_nv21,
                                                                      jint src_width,
                                                                      jint src_height,
                                                                      jbyteArray out_data,
                                                                      jintArray out_width,
                                                                      jintArray out_height) {
    jbyte *jsrcNv21Byte = env->GetByteArrayElements(p_nv21, NULL);
    jbyte *joutDataByte = env->GetByteArrayElements(out_data, NULL);

    jint *joutWidthInt = env->GetIntArrayElements(out_width, NULL);
    jint *joutHeightInt = env->GetIntArrayElements(out_height, NULL);

    int *poutWidth = (int *) joutWidthInt;
    int *poutHeight = (int *) joutHeightInt;

    unsigned char *pSrcNv21 = (unsigned char *) jsrcNv21Byte;
    unsigned char *pOutData = (unsigned char *) joutDataByte;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Nv21ClockWiseRotate270(pSrcNv21, (int) src_width, (int) src_height, pOutData,
                                      poutWidth, poutHeight);

    env->ReleaseIntArrayElements(out_width, joutWidthInt, 0);
    env->ReleaseIntArrayElements(out_height, joutHeightInt, 0);
    env->ReleaseByteArrayElements(p_nv21, jsrcNv21Byte, 0);
    env->ReleaseByteArrayElements(out_data, joutDataByte, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeI420ClockWiseRotate90(JNIEnv *env, jclass clazz,
                                                                     jlong c_ptr, jbyteArray p_i420,
                                                                     jint src_width,
                                                                     jint src_height,
                                                                     jbyteArray out_data,
                                                                     jintArray out_width,
                                                                     jintArray out_height) {
    jbyte *jsrcI420Byte = env->GetByteArrayElements(p_i420, NULL);
    jbyte *joutDataByte = env->GetByteArrayElements(out_data, NULL);

    jint *joutWidthInt = env->GetIntArrayElements(out_width, NULL);
    jint *joutHeightInt = env->GetIntArrayElements(out_height, NULL);

    int *poutWidth = (int *) joutWidthInt;
    int *poutHeight = (int *) joutHeightInt;

    unsigned char *pSrcI420 = (unsigned char *) jsrcI420Byte;
    unsigned char *pOutData = (unsigned char *) joutDataByte;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->I420ClockWiseRotate90(pSrcI420, (int) src_width, (int) src_height, pOutData,
                                     poutWidth, poutHeight);

    env->ReleaseIntArrayElements(out_width, joutWidthInt, 0);
    env->ReleaseIntArrayElements(out_height, joutHeightInt, 0);
    env->ReleaseByteArrayElements(p_i420, jsrcI420Byte, 0);
    env->ReleaseByteArrayElements(out_data, joutDataByte, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeYv12ClockWiseRotate90(JNIEnv *env, jclass clazz,
                                                                     jlong c_ptr, jbyteArray p_yv12,
                                                                     jint src_width,
                                                                     jint src_height,
                                                                     jbyteArray out_data,
                                                                     jintArray out_width,
                                                                     jintArray out_height) {
    jbyte *jsrcYv12Byte = env->GetByteArrayElements(p_yv12, NULL);
    jbyte *joutDataByte = env->GetByteArrayElements(out_data, NULL);

    jint *joutWidthInt = env->GetIntArrayElements(out_width, NULL);
    jint *joutHeightInt = env->GetIntArrayElements(out_height, NULL);

    int *poutWidth = (int *) joutWidthInt;
    int *poutHeight = (int *) joutHeightInt;

    unsigned char *pSrcYv12 = (unsigned char *) jsrcYv12Byte;
    unsigned char *pOutData = (unsigned char *) joutDataByte;

    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    pYuvWater->Yv12ClockWiseRotate90(pSrcYv12, (int) src_width, (int) src_height, pOutData,
                                     poutWidth, poutHeight);

    env->ReleaseIntArrayElements(out_width, joutWidthInt, 0);
    env->ReleaseIntArrayElements(out_height, joutHeightInt, 0);
    env->ReleaseByteArrayElements(p_yv12, jsrcYv12Byte, 0);
    env->ReleaseByteArrayElements(out_data, joutDataByte, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dplayer_utils_YUVEngine_nativeStopYuvEngine(JNIEnv *env, jclass clazz,
                                                             jlong c_ptr) {
    YuvEngine *pYuvWater = reinterpret_cast<YuvEngine *> (c_ptr);
    delete pYuvWater;
}