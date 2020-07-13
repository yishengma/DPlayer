package com.example.dplayer.utils;

import java.io.Closeable;
import java.io.IOException;

public class IOUtil {
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
