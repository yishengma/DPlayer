package com.example.dplayer.mediacodec;

public class YUVUtil {


    /**
     * 适合 YV12 , YU12 的 格式
     *
     * @param width  图片的宽
     * @param height 图片的高
     * @return 字节填充后的大小
     * Y  Y  Y  Y
     * Y  Y  Y  Y
     * Y  Y  Y  Y
     * Y  Y  Y  Y
     * U  U
     * U  U
     * V  V
     * V  V
     * <p>
     * 内存对齐 16 字节，即 最小的 stride 为 16 字节
     * 如果 width <= 16 height <= 16 , 则 y_stride : uv_stride = 1 : 1
     * 其他 y_stride : uv_stride = 2 : 1
     */
    public static int getYUVBuffer(int width, int height) {
        int stride = (int) Math.ceil(width / 16.0) * 16;
        int y_size = stride * height;
        int c_stride = (int) Math.ceil(width / 32.0) * 16; // c_stride 由上图可知是 stride 的一半，但不小于 16
        int c_size = c_stride * height / 2;
        return y_size + c_size * 2;
    }
}
