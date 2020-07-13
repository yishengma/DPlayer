package com.example.dplayer.audio;

import android.util.Log;

import com.example.dplayer.utils.IOUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class WavFile {
    public static void convertPcm2Wav(File pcmFile, File wavFile, int sampleRate, int channels, int bitNum) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(pcmFile);
            fileOutputStream = new FileOutputStream(wavFile);
            long byteRate = sampleRate * channels * bitNum / 8;
            long fileLen = fileInputStream.getChannel().size();
            long dataLen = fileLen + 36;//44-8
            writeWaveFileHeader(fileOutputStream, fileLen, dataLen, sampleRate, channels, byteRate);
            int length = 0;
            byte[] buffer = new byte[1024];
            while ((length = fileInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(fileInputStream);
            IOUtil.close(fileOutputStream);
        }
    }


    /**
     * 输出WAV文件
     *
     * @param out           WAV输出文件流
     * @param totalAudioLen 整个音频PCM数据大小
     * @param totalDataLen  整个数据大小
     * @param sampleRate    采样率
     * @param channels      声道数
     * @param byteRate      采样字节byte率
     * @throws IOException
     */
    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                            long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        // 4 字节 大端  RIFF，文件格式
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        //4 字节 小端 从下个地址开始到文件尾的总字节数（文件大小+44-8）
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        //4 字节 大端 WAV文件标志（WAVE）
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';

        //4 字节 大端 "fmt "波形格式标志（fmt）
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节

        //4字节 小端  过滤字节（一般为00 00 00 10H）H 表示 16 进制
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;


        //2 字节，小端 ，格式种类（值为1时，表示数据为线性PCM编码）
        header[20] = 1; // format = 1
        header[21] = 0;

        //2 字节，小端，声道数，单声道为1，双声道为2
        header[22] = (byte) channels;
        header[23] = 0;

        //4 字节，小端，采样率
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);


        //4 字节，小端，音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        //4 字节，小端，确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;

        //4 字节，小端，样本数据位数
        header[34] = 16;
        header[35] = 0;

        //4 字节，大端，数据标志符（data）
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';

        //4 字节，小端,采样数据总数
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
