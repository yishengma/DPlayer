package com.example.dplayer.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DCustomView extends View {
    private Paint mPaint;
    private Bitmap mBitmap;
    private Matrix mMatrix;

    public DCustomView(Context context) {
        this(context, null);
    }

    public DCustomView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DCustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mMatrix = new Matrix();
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mMatrix.setScale(0.5f, 0.5f);
        canvas.drawBitmap(mBitmap, mMatrix, mPaint);
    }
}
