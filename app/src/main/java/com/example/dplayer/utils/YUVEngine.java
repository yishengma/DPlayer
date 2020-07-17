package com.example.dplayer.utils;

public class YUVEngine {

    static {
        System.loadLibrary("native-lib");
    }

    private static long mNativePtr;

    public static void startYUVEngine() {
        mNativePtr = nativeStartYuvEngine();
    }

    public static void Yv12ToI420(byte[] pYv12, byte[] pI420, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeYv12ToI420(mNativePtr, pYv12, pI420, width, height);
    }

    public static void I420ToYv12(byte[] pI420, byte[] pYv12, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeI420ToYv12(mNativePtr, pI420, pYv12, width, height);

    }

    public static void Nv21ToI420(byte[] pNv21, byte[] pI420, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv21ToI420(mNativePtr, pNv21, pI420, width, height);
    }

    public static void I420ToNv21(byte[] pI420, byte[] pNv21, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeI420ToNv21(mNativePtr, pI420, pNv21, width, height);
    }

    public static void Nv21ToYV12(byte[] pNv21, byte[] pYv12, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv21ToYV12(mNativePtr, pNv21, pYv12, width, height);
    }

    public static void YV12ToNv21(byte[] pYv12, byte[] pNv21, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeYV12ToNv21(mNativePtr, pYv12, pNv21, width, height);
    }

    public static void Nv21ToNv12(byte[] pNv21, byte[] pNv12, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv21ToNv12(mNativePtr, pNv21, pNv12, width, height);
    }

    public static void Nv12ToNv21(byte[] pNv12, byte[] pNv21, int width, int height) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv12ToNv21(mNativePtr, pNv12, pNv21, width, height);
    }

    public static void cutCommonYuv(int yuvType, int startX, int startY, byte[] srcYuv, int srcW, int srcH, byte[] tarYuv, int cutW, int cutH) {
        if (mNativePtr == 0) {
            return;
        }
        nativeCutCommonYuv(mNativePtr, yuvType, startX, startY, srcYuv, srcW, srcH, tarYuv, cutW, cutH);
    }

    public static void getSpecYuvBuffer(int yuvType, byte[] dstBuf, byte[] srcYuv, int srcW, int srcH, int dirty_Y, int dirty_UV) {
        if (mNativePtr == 0) {
            return;
        }
        nativeGetSpecYuvBuffer(mNativePtr, yuvType, dstBuf, srcYuv, srcW, srcH, dirty_Y, dirty_UV);
    }

    public static void yuvAddWaterMark(int yuvType, int startX, int startY, byte[] waterMarkData,
                                 int waterMarkW, int waterMarkH, byte[] yuvData, int yuvW, int yuvH) {
        if (mNativePtr == 0) {
            return;
        }
        nativeYuvAddWaterMark(mNativePtr, yuvType, startX, startY, waterMarkData, waterMarkW, waterMarkH, yuvData, yuvW, yuvH);
    }

    public static void Nv21ClockWiseRotate90(byte[] pNv21, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv21ClockWiseRotate90(mNativePtr, pNv21, srcWidth, srcHeight, outData, outWidth, outHeight);
    }

    public static void Nv12ClockWiseRotate90(byte[] pNv12, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv12ClockWiseRotate90(mNativePtr, pNv12, srcWidth, srcHeight, outData, outWidth, outHeight);
    }

    public static void Nv21ClockWiseRotate180(byte[] pNv21, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv21ClockWiseRotate180(mNativePtr, pNv21, srcWidth, srcHeight, outData, outWidth, outHeight);
    }

    public static void Nv21ClockWiseRotate270(byte[] pNv21, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight) {
        if (mNativePtr == 0) {
            return;
        }
        nativeNv21ClockWiseRotate270(mNativePtr, pNv21, srcWidth, srcHeight, outData, outWidth, outHeight);
    }

    //I420(YUV420P)图像顺时针旋转90度
    public static void I420ClockWiseRotate90(byte[] pI420, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight) {
        if (mNativePtr == 0) {
            return;
        }
        nativeI420ClockWiseRotate90(mNativePtr, pI420, srcWidth, srcHeight, outData, outWidth, outHeight);
    }

    //YV12图像顺时针旋转90度
    public static void Yv12ClockWiseRotate90(byte[] pYv12, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight) {
        if (mNativePtr == 0) {
            return;
        }
        nativeYv12ClockWiseRotate90(mNativePtr, pYv12, srcWidth, srcHeight, outData, outWidth, outHeight);
    }

    public static void stopYuvEngine() {
        if (mNativePtr == 0) {
            return;
        }
        nativeStopYuvEngine(mNativePtr);
        mNativePtr = 0;
    }


    private native static long nativeStartYuvEngine();

    private native static void nativeYv12ToI420(long cPtr, byte[] pYv12, byte[] pI420, int width, int height);

    private native static void nativeI420ToYv12(long cPtr, byte[] pI420, byte[] pYv12, int width, int height);

    private native static void nativeNv21ToI420(long cPtr, byte[] pNv21, byte[] pI420, int width, int height);

    private native static void nativeI420ToNv21(long cPtr, byte[] pI420, byte[] pNv21, int width, int height);

    private native static void nativeNv21ToYV12(long cPtr, byte[] pNv21, byte[] pYv12, int width, int height);

    private native static void nativeYV12ToNv21(long cPtr, byte[] pYv12, byte[] pNv21, int width, int height);

    private native static void nativeNv21ToNv12(long cPtr, byte[] pNv21, byte[] pNv12, int width, int height);

    private native static void nativeNv12ToNv21(long cPtr, byte[] pNv12, byte[] pNv21, int width, int height);

    private native static void nativeCutCommonYuv(long cPtr, int yuvType, int startX, int startY, byte[] srcYuv, int srcW, int srcH, byte[] tarYuv, int cutW, int cutH);

    private native static void nativeGetSpecYuvBuffer(long cPtr, int yuvType, byte[] dstBuf, byte[] srcYuv, int srcW, int srcH, int dirty_Y, int dirty_UV);

    private native static void nativeYuvAddWaterMark(long cPtr, int yuvType, int startX, int startY, byte[] waterMarkData,
                                                     int waterMarkW, int waterMarkH, byte[] yuvData, int yuvW, int yuvH);

    private native static void nativeNv21ClockWiseRotate90(long cPtr, byte[] pNv21, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight);

    private native static void nativeNv12ClockWiseRotate90(long cPtr, byte[] pNv12, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight);

    private native static void nativeNv21ClockWiseRotate180(long cPtr, byte[] pNv21, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight);

    private native static void nativeNv21ClockWiseRotate270(long cPtr, byte[] pNv21, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight);

    //I420(YUV420P)图像顺时针旋转90度
    private static native void nativeI420ClockWiseRotate90(long cPtr, byte[] pI420, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight);

    //YV12图像顺时针旋转90度
    private static native void nativeYv12ClockWiseRotate90(long cPtr, byte[] pYv12, int srcWidth, int srcHeight, byte[] outData, int[] outWidth, int[] outHeight);

    private native static void nativeStopYuvEngine(long cPtr);
}
