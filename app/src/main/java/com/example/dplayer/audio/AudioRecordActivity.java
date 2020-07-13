package com.example.dplayer.audio;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.dplayer.R;
import com.example.dplayer.utils.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecordActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "AudioRecordActivity";
    private TextView mLogContent;
    private Button mStartView;
    private Button mResumeView;
    private Button mPauseView;
    private Button mStopView;
    private Button mPlayView;
    private DAudioRecord mDAudioRecord;
    private DAudioTrack mDAudioTrack;
    private File mFile;
    private FileOutputStream mFileOutputStream;
    private FileInputStream mFileInputStream;
    private String mFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        initView();
        createFile();
        mDAudioRecord = new DAudioRecord.Builder().build();
        mDAudioRecord.setRecordCallback(new DAudioRecord.RecordCallback() {
            @Override
            public void onSuccess(byte[] buffer) {
                writeToFile(buffer);
            }

            @Override
            public void onError(int code, Exception e) {

            }
        });
        mDAudioTrack = new DAudioTrack.Builder().build();
        mDAudioRecord.setOnStateListener(new DAudioRecord.OnStateListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onPause() {

            }

            @Override
            public void onResume() {

            }

            @Override
            public void onStop() {
                closeFile();
            }
        });
    }

    private void initView() {
        mLogContent = findViewById(R.id.tv_log_content);
        mStartView = findViewById(R.id.btn_start);
        mPauseView = findViewById(R.id.btn_pause);
        mResumeView = findViewById(R.id.btn_resume);
        mStopView = findViewById(R.id.btn_stop);
        mPlayView = findViewById(R.id.btn_play);
        mStartView.setOnClickListener(this);
        mResumeView.setOnClickListener(this);
        mPauseView.setOnClickListener(this);
        mStopView.setOnClickListener(this);
        mPlayView.setOnClickListener(this);
        mLogContent.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    private void createFile() {
        mFileName = Environment.getExternalStorageDirectory().getPath() + "/test.pcm";
        mFile = new File(mFileName);
        if (mFile.exists()) {
            mFile.delete();
        }
        try {
            mFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mFileOutputStream = new FileOutputStream(mFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void writeToFile(byte[] buffer) {
        if (mFileOutputStream == null) {
            return;
        }
        try {
            mFileOutputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeFile() {
        IOUtil.close(mFileOutputStream);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                checkAudioRecordPermission();
                break;
            case R.id.btn_resume:
                mDAudioRecord.resume();
                break;
            case R.id.btn_pause:
                mDAudioRecord.pause();
                break;
            case R.id.btn_stop:
                mDAudioRecord.stop();
                break;
            case R.id.btn_play:
                Log.i(TAG,""+mFileName);
                new Thread(playPCMRecord).start();
                break;
            default:
                break;
        }
    }

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, AudioRecordActivity.class);
        context.startActivity(intent);
    }

    private void checkAudioRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 9);
        } else {
            mDAudioRecord.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 9 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mDAudioRecord.start();
        }
    }

    private Runnable playPCMRecord = new Runnable() {
        @Override
        public void run() {
            try {
                mDAudioTrack.play();
                mFileInputStream = new FileInputStream(mFile.getAbsolutePath());
                byte[] buffer = new byte[mDAudioTrack.getBufferSize()];
                int len = 0;
                while ((len = mFileInputStream.read(buffer)) != -1) {
                    mDAudioTrack.write(buffer, 0, len);
                }
            } catch (Exception e) {

            } finally {
                mDAudioTrack.stop();
                IOUtil.close(mFileInputStream);
            }
        }
    };

}
