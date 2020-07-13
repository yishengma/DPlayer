package com.example.dplayer.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.TextureView;

public class DCustomTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private Canvas mCanvas;
    private Paint mPaint;
    private Matrix mMatrix;
    private Bitmap mBitmap;


    public DCustomTextureView(Context context) {
        super(context);
    }

    public DCustomTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DCustomTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        mCanvas = lockCanvas();
        mMatrix.setScale(0.5f, 0.5f);
        mCanvas.drawBitmap(mBitmap, mMatrix, mPaint);
        unlockCanvasAndPost(mCanvas);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }


    private void initView() {
        setSurfaceTextureListener(this);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mMatrix = new Matrix();
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }
}
