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

public class DTextureView extends TextureView {
    public DTextureView(Context context) {
        super(context);
    }

    public DTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void drawWitSurfaceView(final Bitmap bitmap) {
        final Matrix matrix = new Matrix();
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                Canvas canvas = lockCanvas();
                matrix.setScale(0.5f, 0.5f);
                canvas.drawBitmap(bitmap, matrix, paint);
                unlockCanvasAndPost(canvas);
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
        });
    }
}
