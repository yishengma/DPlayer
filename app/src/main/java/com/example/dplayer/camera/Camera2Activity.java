package com.example.dplayer.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.dplayer.R;
import com.example.dplayer.utils.IOUtil;
import com.example.dplayer.utils.YUVEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


public class Camera2Activity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "Camera2Activity";

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, Camera2Activity.class);
        context.startActivity(intent);
    }

    private TextureView mTextureView;
    private ImageView mImageView;
    private Button mTakePicView;
    private Camera mCamera;
    private byte[] mData;
    private static final int DEFAULT_WIDTH = 1280;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        initView();
        initListener();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        mCamera = Camera.open(0);
        if (mCamera == null) {
            return;
        }
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                //Camera 输出的 NV21 默认是横屏的，即使
                //setDisplayOrientation 为 90 度
                //因此如果是竖屏的就需要旋转 90 度
                Camera.Size size = camera.getParameters().getPreviewSize();
                mData = new byte[bytes.length];
                mData = rotateYUV420SP(bytes,size.width,size.height);
            }
        });
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        for (int j = 0; j < sizes.size(); j++) {
            Camera.Size size = sizes.get(j);
            if (size.width == DEFAULT_WIDTH) {
                //setPreviewSize 只能是 getSupportedPreviewSizes 中的一种
                parameters.setPreviewSize(size.width, size.height);
            }
        }

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        mCamera.toString();
        mCamera.release();
        return false;
    }

    private void initView() {
        mTextureView = findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(this);
        mImageView = findViewById(R.id.imv_image);
        mTakePicView = findViewById(R.id.btn_take_pic);
        mTakePicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Camera.Size size = mCamera.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(mData, ImageFormat.NV21, size.height,size.width, null);//旋转后宽高对调
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, size.height,size.width), 80, stream);//旋转后宽高对调
                Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                mImageView.setImageBitmap(bitmap);
                IOUtil.close(stream);
            }
        });
    }

    private void initListener() {

    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public static byte[] rotateYUV420SP(byte[] src, int width, int height) {
         int nWidth = 0, nHeight = 0;
         int wh = 0;
         int uvHeight = 0;
         byte[] dst = new byte[src.length];
        if(width != nWidth || height != nHeight)
        {
            nWidth = width;
            nHeight = height;
            wh = width * height;
            uvHeight = height >> 1;//uvHeight = height / 2
        }

        //旋转Y
        int k = 0;
        for(int i = 0; i < width; i++) {
            int nPos = 0;
            for(int j = 0; j < height; j++) {
                dst[k] = src[nPos + i];
                k++;
                nPos += width;
            }
        }

        for(int i = 0; i < width; i+=2){
            int nPos = wh;
            for(int j = 0; j < uvHeight; j++) {
                dst[k] = src[nPos + i];
                dst[k + 1] = src[nPos + i + 1];
                k += 2;
                nPos += width;
            }
        }
        return dst;
    }
}