package com.example.dplayer.mediacodec.mp4extractor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.R;
import com.example.dplayer.mediacodec.mp4.Mp4Activity;

import java.io.IOException;

public class ExtractorMp4Activity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, ExtractorMp4Activity.class);
        context.startActivity(intent);
    }

    private Button mPlayView;
    private SurfaceView mSurfaceView;
    private Mp4Player mMp4Player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extractor_mp4);
        mPlayView = findViewById(R.id.btn_play_mp4);
        mSurfaceView = findViewById(R.id.surface_view);
        mMp4Player = new Mp4Player();
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = getAssets().openFd("input.mp4");
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        mSurfaceView.getHolder().addCallback(this);
        mMp4Player.setDataSource(assetFileDescriptor);
        mPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMp4Player.start();
            }
        });

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mMp4Player.setSurface(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}