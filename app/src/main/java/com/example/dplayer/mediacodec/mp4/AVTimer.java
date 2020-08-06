package com.example.dplayer.mediacodec.mp4;

public class AVTimer {
    private static long baseTimestampUs = -1;

    public static long getBaseTimestampUs () {
        if (baseTimestampUs  == -1) {
            baseTimestampUs  = System.currentTimeMillis() * 1000;
        }
        return baseTimestampUs ;
    }

}
