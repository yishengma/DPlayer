package com.example.dplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    DPlayer player = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = new DPlayer();
        player.setErrorListener(new DPlayer.ErrorListener() {
            @Override
            public void onError(int code, String msg) {
                Log.e(TAG, String.format("code:%s,msg:%s", code, msg));
            }
        });
        player.setDataSource("http://file.kuyinyun.com/group1/M00/90/B7/rBBGdFPXJNeAM-nhABeMElAM6bY151.mp3");
        player.prepare();
        player.play();

//        player.play();
    }
}
