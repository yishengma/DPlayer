package com.example.dplayer.opengles;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.math.MathUtils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.example.dplayer.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLShowImageActivity extends AppCompatActivity {


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, GLShowImageActivity.class);
        context.startActivity(intent);
    }

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_g_l_show_image);

        mGLSurfaceView = findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(new Renderer());
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    private static final float CUBE[] = {
            //按三角形方向绘制
            -1.0f, -1.0f, //v1
            1.0f, -1.0f,  //v2
            -1.0f, 1.0f,  //v3
            1.0f, 1.0f   //v4
    };

    public static final float TEXTURE_NO_ROTATION[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    private int mGLTextureId = OpenGlUtil.NO_TEXTURE;
    private GLImageHandler mGLImageHandler = new GLImageHandler();

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGlTextureBuffer;

    private int mOutputWidth, mOutputHeight;
    private int mImageWidth, mImageHeight;


    class Renderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            mGLImageHandler.init();

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.we);
            mImageWidth = bitmap.getWidth();
            mImageHeight = bitmap.getHeight();

            mGLTextureId = OpenGlUtil.loadTexture(bitmap, mGLTextureId, true);

            //顶点数组缓冲
            mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            mGLCubeBuffer.put(CUBE).position(0);

            mGlTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            mGlTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);


        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {
            mOutputWidth = i;
            mOutputHeight = i1;

            GLES20.glViewport(0, 0, i, i1);
            adjustImageScaling();
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            // 根据纹理id，顶点和纹理坐标数据绘制图片
            mGLImageHandler.onDraw(mGLTextureId, mGLCubeBuffer, mGlTextureBuffer);
        }

        private void adjustImageScaling() {
            float outputWidth = mOutputWidth;
            float outputHeight = mOutputHeight;

            float ratio1 = outputWidth / mImageWidth;
            float ratio2 = outputHeight / mImageHeight;
            float ratioMax = Math.min(ratio1, ratio2);

            int imageWidth = Math.round(mImageWidth * ratioMax);
            int imageHeight = Math.round(mImageHeight * ratioMax);

            float ratioWidth = outputWidth / imageWidth;
            float ratioHeight = outputHeight / imageHeight;

            // 根据拉伸比例还原顶点
            float[] cube = new float[]{
                    CUBE[0] / ratioWidth, CUBE[1] / ratioHeight,
                    CUBE[2] / ratioWidth, CUBE[3] / ratioHeight,
                    CUBE[4] / ratioWidth, CUBE[5] / ratioHeight,
                    CUBE[6] / ratioWidth, CUBE[7] / ratioHeight,
            };

            mGLCubeBuffer.clear();
            mGLCubeBuffer.put(cube).position(0);
        }
    }
}