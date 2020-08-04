package com.example.dplayer.mediacodec.mp4;


import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;

public class CameraHelper implements Camera.PreviewCallback {
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int mDisplayOrientation = 0;
    public int DEFAULT_WIDTH = 1280;
    public int DEFAULT_HEIGHT = 720;
    public int mPreviewWidth;
    public int mPreviewHeight;
    private Activity mContext;
    private PreviewCallback mPreviewCallback;
    public int mFrameRate;

    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public interface PreviewCallback {
        public void onFrame(byte[] data);
    }

    public CameraHelper(SurfaceView surfaceView, Activity context) {
        mSurfaceView = surfaceView;
        mContext = context;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (mCamera == null) {
                    return;
                }

                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
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
        openCamera(mCameraFacing);
    }

    private void openCamera(int cameraFacing) {
        if (!supportCameraFacing(cameraFacing)) {

            return;
        }
        mCamera = Camera.open(cameraFacing);
        setParameters(mCamera);
        mCamera.setPreviewCallback(this);
    }

    private void setParameters(Camera camera) {
        mParameters = camera.getParameters();
        mParameters.setPreviewFormat(ImageFormat.NV21);

        Camera.Size previewSize = getBestSize(DEFAULT_WIDTH, DEFAULT_HEIGHT, mParameters.getSupportedPreviewSizes());
        mParameters.setPreviewSize(previewSize.width, previewSize.height);
        Camera.Size pictureSize = getBestSize(DEFAULT_WIDTH, DEFAULT_HEIGHT, mParameters.getSupportedPreviewSizes());
        mParameters.setPictureSize(pictureSize.width, pictureSize.height);

        if (supportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;

        //set fps range.
        int defminFps = 0;
        int defmaxFps = 0;
        List<int[]> supportedPreviewFpsRange = mParameters.getSupportedPreviewFpsRange();
        for (int[] fps : supportedPreviewFpsRange) {
            if (defminFps <= fps[PREVIEW_FPS_MIN_INDEX] && defmaxFps <= fps[PREVIEW_FPS_MAX_INDEX]) {
                defminFps = fps[PREVIEW_FPS_MIN_INDEX];
                defmaxFps = fps[PREVIEW_FPS_MAX_INDEX];
            }
        }
        //设置相机预览帧率
        mParameters.setPreviewFpsRange(defminFps, defmaxFps);
        mFrameRate = defmaxFps / 1000;

        setCameraDisplayOrientation(mContext);

        camera.setParameters(mParameters);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mPreviewCallback == null) {
            return;
        }
        mPreviewCallback.onFrame(bytes);
    }

    private boolean supportCameraFacing(int cameraFacing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == cameraFacing) {
                return true;
            }
        }
        return false;
    }

    private void setCameraDisplayOrientation(Activity activity) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraFacing, info);
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
        mDisplayOrientation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + degrees) % 360;
            mDisplayOrientation = (360 - mDisplayOrientation) % 360;
        } else {
            mDisplayOrientation = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(mDisplayOrientation);
    }

    private boolean supportFocus(String focus) {
        List<String> focusModes = mCamera.getParameters().getSupportedFocusModes();
        if (focusModes.contains(focus)) {
            return true;
        }
        return false;
    }

    private Camera.Size getBestSize(int width, int height, List<Camera.Size> sizes) {
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            if (width == size.width) {
                return size;
            }
        }
        return sizes.get(0);
    }

    public void stop() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}
