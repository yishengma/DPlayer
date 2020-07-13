package com.example.dplayer.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.Format;

public class DCustomSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Matrix mMatrix;
    private Bitmap mBitmap;

    public DCustomSurfaceView(Context context) {
        this(context, null);
    }

    public DCustomSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DCustomSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            return;
        }
        mCanvas = mSurfaceHolder.lockCanvas();
        mMatrix.setScale(0.5f, 0.5f);
        mCanvas.drawBitmap(mBitmap, mMatrix, mPaint);
        mSurfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void initView() {
        setZOrderOnTop(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder.addCallback(this);

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
