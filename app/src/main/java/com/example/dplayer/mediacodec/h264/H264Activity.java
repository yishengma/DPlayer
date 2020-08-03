package com.example.dplayer.mediacodec.h264;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.R;
import com.example.dplayer.mediacodec.aac.AacActivity;

public class H264Activity extends AppCompatActivity implements View.OnClickListener {
    public static void startActivity(Context context) {
        Intent intent = new Intent(context, H264Activity.class);
        context.startActivity(intent);
    }


    private SurfaceView mSurfaceView;
    private Button mStartView;
    private Button mStopView;
    private H264VideoRecord mH264VideoRecord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264);
        mSurfaceView = findViewById(R.id.surface_view);
        mStartView = findViewById(R.id.btn_start);
        mStopView = findViewById(R.id.btn_stop);

        mStartView.setOnClickListener(this);
        mStopView.setOnClickListener(this);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media_codec_video.mp4";

        mH264VideoRecord = new H264VideoRecord(this, path, mSurfaceView);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                mH264VideoRecord.start();
                break;
            case R.id.btn_stop:
                mH264VideoRecord.stop();
                break;
            default:
                break;
        }
    }
}