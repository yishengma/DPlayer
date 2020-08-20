package com.example.dplayer.mediacodec.mp4;


import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;

public class CameraHelper {

    private int mPreWidth;
    private int mPreHeight;
    private int mFrameRate;


    private Camera mCamera;
    private Camera.Size mPreviewSize;
    private Camera.Parameters mCameraParameters;
    private boolean mIsPreviewing = false;
    private Activity mContext;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private CameraPreviewCallback mCameraPreviewCallback;
    private PreviewCallback mPreviewCallback;


    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public interface PreviewCallback {
        void onFrame(byte[] data);

        void onOperate(int width, int height, int fps);
    }

    public CameraHelper(SurfaceView surfaceView, Activity context) {
        mSurfaceView = surfaceView;
        mContext = context;
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                doOpenCamera();
                doStartPreview(mContext, surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mCamera == null) {
                    return;
                }
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        });

    }

    private void doOpenCamera() {
        if (mCamera != null) {
            return;
        }
        mCamera = Camera.open();
    }

    private void doStartPreview(Activity activity, SurfaceHolder surfaceHolder) {
        if (mIsPreviewing) {
            return;
        }
        mContext = activity;
        setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK);
        setCameraParameters(surfaceHolder);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mIsPreviewing = true;
        mPreviewCallback.onOperate(mPreWidth, mPreHeight, mFrameRate);

    }

    public void stop() {
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            if (mIsPreviewing) {
                mCamera.stopPreview();
            }
            mIsPreviewing = false;
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCameraDisplayOrientation(Activity activity, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private void setCameraParameters(SurfaceHolder surfaceHolder) {
        if (!mIsPreviewing && mCamera != null) {
            mCameraParameters = mCamera.getParameters();
            mCameraParameters.setPreviewFormat(ImageFormat.NV21);
            List<Camera.Size> supportedPreviewSizes = mCameraParameters.getSupportedPreviewSizes();
            Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size o1, Camera.Size o2) {
                    Integer left = o1.width;
                    Integer right = o2.width;
                    return left.compareTo(right);
                }
            });

            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            for (Camera.Size size : supportedPreviewSizes) {
                if (size.width >= displayMetrics.heightPixels && size.height >= displayMetrics.widthPixels) {
                    if ((1.0f * size.width / size.height) == (1.0f * displayMetrics.heightPixels / displayMetrics.widthPixels)) {
                        mPreviewSize = size;
                        break;
                    }
                }
            }
            if (mPreviewSize != null) {
                mPreWidth = mPreviewSize.width;
                mPreHeight = mPreviewSize.height;
            } else {
                mPreWidth = 1280;
                mPreHeight = 720;
            }


            mCameraParameters.setPreviewSize(mPreWidth, mPreHeight);

            //set fps range.
            int defminFps = 0;
            int defmaxFps = 0;
            List<int[]> supportedPreviewFpsRange = mCameraParameters.getSupportedPreviewFpsRange();
            for (int[] fps : supportedPreviewFpsRange) {
                if (defminFps <= fps[PREVIEW_FPS_MIN_INDEX] && defmaxFps <= fps[PREVIEW_FPS_MAX_INDEX]) {
                    defminFps = fps[PREVIEW_FPS_MIN_INDEX];
                    defmaxFps = fps[PREVIEW_FPS_MAX_INDEX];
                }
            }
            //设置相机预览帧率
            mCameraParameters.setPreviewFpsRange(defminFps, defmaxFps);
            mFrameRate = defmaxFps / 1000;
            surfaceHolder.setFixedSize(mPreWidth, mPreHeight);
            mCameraPreviewCallback = new CameraPreviewCallback();
            mCamera.addCallbackBuffer(new byte[mPreHeight * mPreWidth * 3 / 2]);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            List<String> focusModes = mCameraParameters.getSupportedFocusModes();
            for (String focusMode : focusModes) {//检查支持的对焦
                if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            }
            mCamera.setParameters(mCameraParameters);
        }
    }


    class CameraPreviewCallback implements Camera.PreviewCallback {
        private CameraPreviewCallback() {

        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!mIsPreviewing || mCamera == null) {
                return;
            }
            Camera.Size size = camera.getParameters().getPreviewSize();
            //通过回调,拿到的data数据是原始数据
            //丢给VideoRunnable线程,使用MediaCodec进行h264编码操作
            if (data != null) {
                if (mPreviewCallback != null) {
                    mPreviewCallback.onFrame(data);
                }
                camera.addCallbackBuffer(data);
            } else {
                camera.addCallbackBuffer(new byte[size.width * size.height * 3 / 2]);
            }
        }
    }
}
