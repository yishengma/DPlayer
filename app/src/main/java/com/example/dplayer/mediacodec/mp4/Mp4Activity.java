package com.example.dplayer.mediacodec.mp4;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.R;
import com.example.dplayer.mediacodec.h264.H264Activity;

public class Mp4Activity extends AppCompatActivity implements View.OnClickListener {
    public static void startActivity(Context context) {
        Intent intent = new Intent(context, Mp4Activity.class);
        context.startActivity(intent);
    }

    public final static int DEFAULT_INPUT = MediaRecorder.AudioSource.MIC;
    public final static int DEFAULT_SAMPLE_RATE_IN_HZ = 44_100;
    public final static int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public final static int DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public final static int DEFAULT_BUFFER_SIZE_IN_BYTES = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT);
    private Mp4Record mMp4Record;
    private SurfaceView mSurfaceView;
    private Button mStartView;
    private Button mStopView;
    private String mPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4);
        mSurfaceView = findViewById(R.id.surface_view);
        mStartView = findViewById(R.id.btn_start);
        mStopView = findViewById(R.id.btn_stop);

        mStartView.setOnClickListener(this);
        mStopView.setOnClickListener(this);
        mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media_muxer.mp4";
        mMp4Record = new Mp4Record(this, mSurfaceView, DEFAULT_INPUT, DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_CHANNEL_CONFIG, DEFAULT_ENCODING, DEFAULT_BUFFER_SIZE_IN_BYTES, mPath);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                mMp4Record.start();
                break;
            case R.id.btn_stop:
                mMp4Record.stop();
                break;
            default:
                break;
        }
    }
}