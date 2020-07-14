package com.example.dplayer.camera;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.dplayer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CameraActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CameraActivity";

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, CameraActivity.class);
        context.startActivity(intent);
    }

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            mCamera = android.hardware.Camera.open(0);
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
    };
    private SurfaceView mSurfaceView;
    private Button mFocusView;
    private Button mTakePictureView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(mCallback);
        initView();
    }

    private void initView() {
        mTakePictureView = findViewById(R.id.btn_capture);
        mFocusView = findViewById(R.id.btn_focus);
        mTakePictureView.setOnClickListener(this);
        mFocusView.setOnClickListener(this);
    }


    private void takePicture() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, mPictureCallback);
        }
    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] bytes, Camera camera) {
// 写在指定目录 sdcard 中去
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(Environment.getExternalStorageDirectory(), "pictureCallback.jpg");
                    try {
                        // 字节文件输出流，把byte[]数据写入到文件里面去
                        OutputStream os = new FileOutputStream(file);
                        os.write(bytes);

                        // 关闭字节文件输出流
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "保存失败");
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCamera.startPreview();
                        }
                    });
                }
            }).start();
        }
    };

    private void focus() {
        mCamera.autoFocus(null);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_capture:
                takePicture();
                break;
            case R.id.btn_focus:
                focus();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCallback != null) {
            mSurfaceHolder.removeCallback(mCallback);
        }
    }
}