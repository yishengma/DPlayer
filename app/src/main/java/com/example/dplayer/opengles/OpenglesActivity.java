package com.example.dplayer.opengles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.dplayer.R;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenglesActivity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, OpenglesActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengles);

        createGLSurfaceView();
        createSurfaceView();
    }

    private void createGLSurfaceView() {
        GLSurfaceView surfaceView = findViewById(R.id.gl_surface_view);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setRenderer(new GLRenderer());
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private ELGRenderer elgRenderer;

    private void createSurfaceView() {
        SurfaceView surfaceView = findViewById(R.id.surface_view);
        elgRenderer = new ELGRenderer();
        elgRenderer.start();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                elgRenderer.render(surfaceHolder.getSurface(), i1, i2);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        elgRenderer.release();
        elgRenderer = null;
    }
}