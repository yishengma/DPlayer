package com.example.dplayer.mediacodec;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.dplayer.R;
import com.example.dplayer.utils.YUVEngine;

import java.util.List;

public class H264CodecActivity extends AppCompatActivity implements View.OnClickListener, VideoRecorder.CameraOperateCallback, SurfacePreview.PermissionNotify {
    public static void startActivity(Context context) {
        Intent intent = new Intent(context, H264CodecActivity.class);
        context.startActivity(intent);
    }
    private static final String TAG = "H264CodecActivity";

    private Button mStartView;
    private SurfaceView mSurfaceView;
    private SurfacePreview mSurfacePreview;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsStarted;
    private AVMediaMuxer mAVMediaMuxer;
    private int width;
    private int height;
    private int frameRate;
    private boolean mHasPermission;
    private static final int TARGET_PERMISSION_REQUEST = 100;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_h264_codec);
        mIsStarted = false;
        mHasPermission = false;
        mStartView = findViewById(R.id.btn_start);
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfacePreview = new SurfacePreview(this, this);
        mSurfaceHolder.addCallback(mSurfacePreview);
        mStartView.setOnClickListener(this);

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + String.format("%1$tY-%1$tm-%1$td_%1$tH_%1$tM_%1$tS_%1$tL.mp4", System.currentTimeMillis());

        mAVMediaMuxer = AVMediaMuxer.getInstance();
        mAVMediaMuxer.initMediaMuxer(filePath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    mHasPermission = false;
                    break;
                } else
                    mHasPermission = true;
            }
            if (!mHasPermission) {
                ActivityCompat.requestPermissions(this,
                        permissions, TARGET_PERMISSION_REQUEST);
            }
        }
        YUVEngine.startYUVEngine();
    }

    @Override
    public void onClick(View view) {
        codecToggle();
    }

    private void codecToggle() {
        if (mIsStarted) {
            mIsStarted = false;
            mAVMediaMuxer.stopEncoder();
            mAVMediaMuxer.stopAudioRecord();
            mAVMediaMuxer.release();
            mAVMediaMuxer = null;
        } else {
            mIsStarted = true;
            if(mAVMediaMuxer == null){

                String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + String.format("%1$tY-%1$tm-%1$td_%1$tH_%1$tM_%1$tS_%1$tL.mp4", System.currentTimeMillis());
                mAVMediaMuxer = AVMediaMuxer.getInstance();
                mAVMediaMuxer.initMediaMuxer(filePath);
            }

            //采集音频
            mAVMediaMuxer.startAudioRecord();
            //初始化音频编码器
            mAVMediaMuxer.initAudioEncoder();
            //初始化视频编码器
            mAVMediaMuxer.initVideoEncoder(width, height, frameRate);
            //启动编码
            mAVMediaMuxer.startEncoder();
        }
        mStartView.setText(mIsStarted ? "停止" : "开始");
    }

    @Override
    public boolean hasPermission() {
        return mHasPermission;
    }

    @Override
    public void cameraHasOpened() {
        VideoRecorder.getInstance().doStartPreview(this, mSurfaceHolder);
    }

    @Override
    public void cameraHasPreview(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.frameRate = fps;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
            if(requestCode == TARGET_PERMISSION_REQUEST){
                mStartView.setEnabled(true);
                mHasPermission = true;
                // 打开摄像头
               VideoRecorder.getInstance().doOpenCamera(this);
            }
        }else{
            mStartView.setEnabled(false);
            mHasPermission = false;
            Toast.makeText(this, "莫得权限！", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsStarted) {
            mIsStarted = false;
            if (mAVMediaMuxer != null) {
                mAVMediaMuxer.stopAudioRecord();
                mAVMediaMuxer.stopMediaMuxer();
                mAVMediaMuxer.release();
                mAVMediaMuxer =  null;
            }
        }
    }
}