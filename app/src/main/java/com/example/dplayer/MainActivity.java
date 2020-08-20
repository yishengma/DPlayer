package com.example.dplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.camera.Camera3Activity;
import com.example.dplayer.camera.CameraActivity;
import com.example.dplayer.image.ImageActivity;
import com.example.dplayer.mediacodec.aac.AacActivity;
import com.example.dplayer.mediacodec.h264.H264Activity;
import com.example.dplayer.mediacodec.mp4.Mp4Activity;
import com.example.dplayer.opengles.GLShowImageActivity;
import com.example.dplayer.opengles.OpenglesActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mRecordView;
    private Button mImageView;
    private Button mCameraView;
    private Button mMuxerAndExtractorView;
    private Button mOpenGLEsView;
    private Button mOpenGLEsImageView;
    private Button mCodecAACView;
    private Button mCodecH264View;
    private Button mCodecMP4View;
    private Button mMediaMuxerView;
    private Button mCodecMp4View;
    private Button mCamera2View;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            //请求授权
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.CAMERA}, 2);
        }
        initView();
    }

    private void initView() {
        mRecordView = findViewById(R.id.record);
        mRecordView.setOnClickListener(this);
        mImageView = findViewById(R.id.image);
        mImageView.setOnClickListener(this);
        mCameraView = findViewById(R.id.camera);
        mCameraView.setOnClickListener(this);
        mOpenGLEsView = findViewById(R.id.btn_opengl);
        mOpenGLEsView.setOnClickListener(this);
        mOpenGLEsImageView = findViewById(R.id.btn_opengl_image);
        mOpenGLEsImageView.setOnClickListener(this);
        mCamera2View = findViewById(R.id.camera2);
        mCamera2View.setOnClickListener(this);
        mCodecAACView = findViewById(R.id.btn_codec_aac);
        mCodecAACView.setOnClickListener(this);
        mCodecH264View = findViewById(R.id.btn_codec_h264);
        mCodecH264View.setOnClickListener(this);
        mCodecMP4View = findViewById(R.id.btn_codec_mp4);
        mCodecMP4View.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record:
                AacActivity.startActivity(MainActivity.this);
                break;
            case R.id.image:
                ImageActivity.startActivity(MainActivity.this);
            case R.id.camera:
                CameraActivity.startActivity(MainActivity.this);
                break;
            case R.id.btn_opengl:
                OpenglesActivity.startActivity(MainActivity.this);
                break;
            case R.id.btn_opengl_image:
                GLShowImageActivity.startActivity(MainActivity.this);
                break;
            case R.id.camera2:
                Camera3Activity.startActivity(MainActivity.this);
                break;
            case R.id.btn_codec_aac:
                AacActivity.startActivity(MainActivity.this);
                break;
            case R.id.btn_codec_h264:
                H264Activity.startActivity(MainActivity.this);
                break;
            case R.id.btn_codec_mp4:
                Mp4Activity.startActivity(MainActivity.this);
                break;
        }
    }
}