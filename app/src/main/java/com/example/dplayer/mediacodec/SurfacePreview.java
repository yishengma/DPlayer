package com.example.dplayer.mediacodec;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfacePreview implements SurfaceHolder.Callback {
    private VideoRecorder.CameraOperateCallback mCameraOperateCallback;
    private PermissionNotify mPermissionNotify;

    public SurfacePreview(VideoRecorder.CameraOperateCallback cameraOperateCallback, PermissionNotify permissionNotify) {
        mCameraOperateCallback = cameraOperateCallback;
        mPermissionNotify = permissionNotify;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (mPermissionNotify == null) {
            return;
        }
        if (mPermissionNotify.hasPermission()) {
            VideoRecorder.getInstance().doOpenCamera(mCameraOperateCallback);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mPermissionNotify == null) {
            return;
        }
        if (mPermissionNotify.hasPermission()) {
            VideoRecorder.getInstance().doOpenCamera(mCameraOperateCallback);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            VideoRecorder.getInstance().doStopCamera();
    }

    public interface PermissionNotify {
        boolean hasPermission();
    }
}
