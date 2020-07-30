package com.example.dplayer.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.dplayer.R;

public class Camera3Activity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, Camera3Activity.class);
        context.startActivity(intent);
    }


    private SurfaceView mSurfaceView;
    private ImageView mImageView;
    private Button mTakePicView;
    private CameraHelper mCameraHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera3);
        mSurfaceView = findViewById(R.id.surface_view);
        mImageView = findViewById(R.id.imv_image);
        mTakePicView = findViewById(R.id.btn_take_pic);
        mCameraHelper = new CameraHelper(mSurfaceView, this);
        mTakePicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraHelper.takePicture(mImageView);
            }
        });
    }
}