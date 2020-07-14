
package com.example.dplayer.image;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.example.dplayer.R;

public class ImageActivity extends AppCompatActivity {

    private DImageView mDImageView;
    private DCustomView mDCustomView;
    private DSurfaceView mDSurfaceView;
    private DCustomSurfaceView mDCustomSurfaceView;
    private DTextureView mDTextureView;
    private DCustomTextureView mDCustomTextureView;

    private LinearLayout mParentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        mParentView = findViewById(R.id.ll_parent);

        addImageView();
        addCustomView();
        addSurfaceView();
        addCustomSurfaceView();
        addTextureView();
        addCustomTextureView();

    }

    private void addImageView() {
        mDImageView = new DImageView(this);
        mDImageView.setImageBitmap(getBitmap());
        addView(mDImageView);
    }

    private void addCustomView() {
        mDCustomView = new DCustomView(this);
        mDCustomView.setBitmap(getBitmap());
        addView(mDCustomView);
    }

    private void addSurfaceView() {
        mDSurfaceView = new DSurfaceView(this);
        mDSurfaceView.drawWitSurfaceView(getBitmap());
        addView(mDSurfaceView);
    }

    private void addCustomSurfaceView() {
        mDCustomSurfaceView = new DCustomSurfaceView(this);
        mDCustomSurfaceView.setBitmap(getBitmap());
        addView(mDCustomSurfaceView);
    }

    private void addTextureView() {
        mDTextureView = new DTextureView(this);
        mDTextureView.drawWitSurfaceView(getBitmap());
        addView(mDTextureView);
    }

    private void addCustomTextureView() {
        mDCustomTextureView = new DCustomTextureView(this);
        mDCustomTextureView.setBitmap(getBitmap());
        addView(mDCustomTextureView);
    }


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, ImageActivity.class);
        context.startActivity(intent);
    }


    private void addView(View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        mParentView.addView(view, params);
    }

    public Bitmap getBitmap(){
        Drawable db = getResources().getDrawable(R.drawable.we);
        BitmapDrawable drawable = (BitmapDrawable)db;

        Bitmap bitmap = drawable.getBitmap();
        return bitmap;
    }
}