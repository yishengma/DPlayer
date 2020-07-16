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

import com.example.dplayer.audio.AudioRecordActivity;
import com.example.dplayer.camera.CameraActivity;
import com.example.dplayer.image.ImageActivity;
import com.example.dplayer.media.MediaActivity;
import com.example.dplayer.mediacodec.MediaCodecActivity;
import com.example.dplayer.opengles.GLShowImageActivity;
import com.example.dplayer.opengles.OpenglesActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mRecordView;
    private Button mImageView;
    private Button mCameraView;
    private Button mMuxerAndExtractorView;
    private Button mOpenGLEsView;
    private Button mOpenGLEsImageView;
    private Button mMediaCodecView;


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
        mMuxerAndExtractorView = findViewById(R.id.extractor_muxer);
        mMuxerAndExtractorView.setOnClickListener(this);
        mOpenGLEsView = findViewById(R.id.btn_opengl);
        mOpenGLEsView.setOnClickListener(this);
        mOpenGLEsImageView = findViewById(R.id.btn_opengl_image);
        mOpenGLEsImageView.setOnClickListener(this);
        mMediaCodecView = findViewById(R.id.btn_media_codec);
        mMediaCodecView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record:
                AudioRecordActivity.startActivity(MainActivity.this);
                break;
            case R.id.image:
                ImageActivity.startActivity(MainActivity.this);
            case R.id.camera:
                CameraActivity.startActivity(MainActivity.this);
                break;
            case R.id.extractor_muxer:
                MediaActivity.startActivity(MainActivity.this);
                break;
            case R.id.btn_opengl:
                OpenglesActivity.startActivity(MainActivity.this);
                break;
            case R.id.btn_opengl_image:
                GLShowImageActivity.startActivity(MainActivity.this);
                break;
            case R.id.btn_media_codec:
                MediaCodecActivity.startActivity(MainActivity.this);
                break;
        }
    }
}