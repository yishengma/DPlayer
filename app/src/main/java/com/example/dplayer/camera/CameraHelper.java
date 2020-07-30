package com.example.dplayer.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.example.dplayer.utils.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class CameraHelper implements Camera.PreviewCallback {
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int mDisplayOrientation = 0;
    private int DEFAULT_WIDTH = 1280;
    private int DEFAULT_HEIGHT = 720;
    private byte[] mData;
    private Activity mContext;

    public CameraHelper(SurfaceView surfaceView, Activity context) {
        mSurfaceView = surfaceView;
        mContext = context;
        mSurfaceHolder = mSurfaceView.getHolder();
        init();
    }

    private void init() {
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (mCamera != null) {
                    return;
                }
                openCamera(mCameraFacing);
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
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        });
    }


    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        mData = new byte[bytes.length];
        System.arraycopy(bytes, 0, mData, 0, bytes.length);
    }

    public void takePicture(ImageView view) {
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        byte[] rotateData;
        rotateData = rotateYUV420SP(mData, size.width, size.height);
        YuvImage yuvImage = new YuvImage(rotateData, ImageFormat.NV21, size.height, size.width, null);//旋转后宽高对调
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, size.height, size.width), 80, stream);//旋转后宽高对调
        Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
        view.setImageBitmap(bitmap);
        IOUtil.close(stream);
    }

    private void openCamera(int cameraFacing) {
        if (supportCameraFacing(cameraFacing)) {
            mCamera = Camera.open(cameraFacing);
            initParameters(mCamera);
            mCamera.setPreviewCallback(this);
        }
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

    private void initParameters(Camera camera) {
        mParameters = camera.getParameters();
        mParameters.setPreviewFormat(ImageFormat.NV21);

        Camera.Size previewSize = getBestSize(DEFAULT_WIDTH, DEFAULT_HEIGHT, mParameters.getSupportedPreviewSizes());
        mParameters.setPreviewSize(previewSize.width, previewSize.height);
        Camera.Size pictureSize = getBestSize(DEFAULT_WIDTH, DEFAULT_HEIGHT, mParameters.getSupportedPreviewSizes());
        mParameters.setPictureSize(pictureSize.width, pictureSize.height);

        if (supportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        setCameraDisplayOrientation(mContext);

        mCamera.setParameters(mParameters);
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

    public static byte[] rotateYUV420SP(byte[] src, int width, int height) {
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        byte[] dst = new byte[src.length];
        if (width != nWidth || height != nHeight) {
            nWidth = width;
            nHeight = height;
            wh = width * height;
            uvHeight = height >> 1;//uvHeight = height / 2
        }

        //旋转Y
        int k = 0;
        for (int i = 0; i < width; i++) {
            int nPos = 0;
            for (int j = 0; j < height; j++) {
                dst[k] = src[nPos + i];
                k++;
                nPos += width;
            }
        }

        for (int i = 0; i < width; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                dst[k] = src[nPos + i];
                dst[k + 1] = src[nPos + i + 1];
                k += 2;
                nPos += width;
            }
        }
        return dst;
    }


}
