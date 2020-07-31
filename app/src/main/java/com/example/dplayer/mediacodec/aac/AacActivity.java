package com.example.dplayer.mediacodec.aac;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import com.example.dplayer.R;
import com.example.dplayer.mediacodec.AacCodecActivity;

public class AacActivity extends AppCompatActivity implements View.OnClickListener {


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, AacActivity.class);
        context.startActivity(intent);
    }

    public final static int DEFAULT_INPUT = MediaRecorder.AudioSource.MIC;
    public final static int DEFAULT_SAMPLE_RATE_IN_HZ = 44_100;
    public final static int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public final static int DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public final static int DEFAULT_BUFFER_SIZE_IN_BYTES = 2048;


    private Button mRecordView;
    private Button mPlayView;
    private Button mStopView;
    private AacAudioRecord mAudioRecord;
    private AacAudioPlayer mAacAudioPlayer;
    private String mFilePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac);

        mRecordView = findViewById(R.id.btn_record);
        mPlayView = findViewById(R.id.btn_play);
        mStopView = findViewById(R.id.btn_stop);
        mAudioRecord = new AacAudioRecord(DEFAULT_INPUT, DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_CHANNEL_CONFIG, DEFAULT_ENCODING, AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_CHANNEL_CONFIG, DEFAULT_ENCODING));
        mRecordView.setOnClickListener(this);
        mPlayView.setOnClickListener(this);
        mStopView.setOnClickListener(this);
        mAacAudioPlayer = new AacAudioPlayer();
        mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/media_codec_audio.aac";
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_record:
                mAudioRecord.start(mFilePath);
                break;
            case R.id.btn_play:
                mAacAudioPlayer.play(mFilePath);
                break;
            case R.id.btn_stop:
                mAudioRecord.stop();
                break;
        }
    }
}