package com.example.dplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DPlayerActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private Button mStartRecordBtn;
    private Button mStopRecordBtn;
    private Button mPlayAudioBtn;
    private Button mStopAudioBtn;

    private AudioUtil mAudioUtil;
    private static final int BUFFER_SIZE = 1024 * 2;
    private byte[] mBuffer;
    private File mAudioFile;
    private ExecutorService mExecutorService;
    private DPlayer mDPlayer;
    private DVideoView mDVideoView;
    private Button mPlayView;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
//检查权限 是否授权
        //授权了就 进行请求
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_DENIED) {

            //请求授权
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS}, 2);
        }
        mStartRecordBtn = (Button) findViewById(R.id.start_record_button);
        mStopRecordBtn = (Button) findViewById(R.id.stop_record_button);
        mPlayAudioBtn = (Button) findViewById(R.id.play_audio_button);
        mStopAudioBtn = (Button) findViewById(R.id.stop_audio_button);
        mBuffer = new byte[BUFFER_SIZE];
        mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/record/encode.pcm");
        mExecutorService = Executors.newSingleThreadExecutor();

//        mDPlayer = new DPlayer();
//        mDPlayer.setOnErrorListener(new DPlayer.OnErrorListener() {
//            @Override
//            public void onError(int code, String msg) {
//                Log.e(TAG, String.format("%s,%s", code, msg));
//            }
//        });
//        mDPlayer.setOnPrepareListener(new DPlayer.OnPrepareListener() {
//            @Override
//            public void onPrepared() {
//                Log.e(TAG,"onPrepared");
//                mDPlayer.play();
//            }
//        });
//        mDPlayer.setDataSource("http://file.kuyinyun.com/group1/M00/90/B7/rBBGdFPXJNeAM-nhABeMElAM6bY151.mp3");
//        mDPlayer.prepareAsync();

        mDVideoView = findViewById(R.id.video_view);
        //https 不能播放
        //mDVideoView.setDataSource("https://media.w3.org/2010/05/sintel/trailer.mp4");

        //
        mPlayView = findViewById(R.id.btn_play);
        mPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDVideoView.play("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mAudioUtil = AudioUtil.getInstance();
        initEvent();
    }

    private void initEvent() {
        mStartRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAudioUtil.startRecord();
                mAudioUtil.recordData();
            }
        });
        mStopRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAudioUtil.stopRecord();
                mAudioUtil.convertWavFile();
            }
        });

        mPlayAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAudioFile != null) {
                    mExecutorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "" + mAudioFile.getAbsolutePath());
//                            playAudio(mAudioFile);
//                            DOpenSLES sles = new DOpenSLES();
//                            sles.playPcm();
                        }
                    });
                }
            }
        });

        mStopAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }

    private void playAudio(File audioFile) {
        Log.d("MainActivity", "lu yin kaishi");
        int streamType = AudioManager.STREAM_MUSIC;
        int simpleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int mode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(simpleRate, channelConfig, audioFormat);
        AudioTrack audioTrack = new AudioTrack(streamType, simpleRate, channelConfig, audioFormat,
                Math.max(minBufferSize, BUFFER_SIZE), mode);
        audioTrack.play();
        Log.d(TAG, minBufferSize + " is the min buffer size , " + BUFFER_SIZE + " is the read buffer size");

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(audioFile);
            int read;
            while ((read = inputStream.read(mBuffer)) > 0) {
                Log.d("MainActivity", "lu yin kaishi11111");

                audioTrack.write(mBuffer, 0, read);
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
//    DPlayer player = null;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_player);
//
//        player = new DPlayer();
//        player.setErrorListener(new DPlayer.ErrorListener() {
//            @Override
//            public void onError(int code, String msg) {
//                Log.e(TAG, String.format("code:%s,msg:%s", code, msg));
//            }
//        });
//        player.setDataSource("http://file.kuyinyun.com/group1/M00/90/B7/rBBGdFPXJNeAM-nhABeMElAM6bY151.mp3");
//        player.prepare();
//        player.play();
//
////        player.play();
//    }
}
