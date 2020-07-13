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

public class DSurfaceView extends SurfaceView {

    public DSurfaceView(Context context) {
        super(context);
    }

    public DSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void drawWitSurfaceView(final Bitmap bitmap) {
        setZOrderOnTop(true);
        final Matrix matrix = new Matrix();
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (surfaceHolder == null) {
                    return;
                }
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                Canvas canvas = surfaceHolder.lockCanvas();
                matrix.setScale(0.5f, 0.5f);
                canvas.drawBitmap(bitmap, matrix, paint);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
    }


}
