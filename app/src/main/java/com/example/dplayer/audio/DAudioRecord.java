package com.example.dplayer.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class DAudioRecord implements IAudioRecord {
    private static final String TAG = "DAudioRecord";

    public final static int DEFAULT_INPUT = MediaRecorder.AudioSource.MIC;
    public final static int DEFAULT_SAMPLE_RATE_IN_HZ = 16_000;
    public final static int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public final static int DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public final static int DEFAULT_BUFFER_SIZE_IN_BYTES = 2048;

    @IntDef({Status.NO_READY, Status.READY, Status.RECORDING, Status.PAUSE, Status.STOP, Status.DESTROY})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
    public @interface Status {
        int NO_READY = 0;
        int READY = 1;
        int RECORDING = 2;
        int PAUSE = 3;
        int STOP = 4;
        int DESTROY = 5;
    }

    private final static int MSG_START = 1;
    private final static int MSG_RESUME = 2;
    private final static int MSG_PAUSE = 3;
    private final static int MSG_STOP = 4;
    private final static int MSG_DESTROY = 5;

    @Status
    private int mStatus;
    private int mAudioSource;
    private int mSampleRateInHz;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSizeInBytes;
    private byte[] mBuffer;
    private AudioRecord mAudioRecord;
    private RecordCallback mRecordCallback;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mStopReading = false;

    public interface RecordCallback {
        void onSuccess(byte[] data);

        void onError(int code, Exception e);
    }

    public interface OnStateListener {
        void onStart();
        void onPause();
        void onResume();
        void onStop();
    }

    private OnStateListener mOnStateListener;

    public void setOnStateListener(OnStateListener onStateListener) {
        mOnStateListener = onStateListener;
    }

    public void setRecordCallback(RecordCallback recordCallback) {
        mRecordCallback = recordCallback;
    }

    protected DAudioRecord(
            int audioSource,
            int sampleRateInHz,
            int channelConfig,
            int audioFormat,
            int bufferSizeInBytes,
            byte[] buffer) {
        mAudioSource = audioSource;
        mSampleRateInHz = sampleRateInHz;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mBufferSizeInBytes = bufferSizeInBytes;
        mBuffer = buffer;
        mStatus = Status.NO_READY;
        mBufferSizeInBytes = Math.max(mBufferSizeInBytes, AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat));
        createAudioRecord();
        createThread();
    }

    private void createAudioRecord() {
        if (mStatus != Status.NO_READY) {
            Log.e(TAG, "status is not no ready!");
            return;
        }
        mAudioRecord = new AudioRecord(
                mAudioSource,
                mSampleRateInHz,
                mChannelConfig,
                mAudioFormat,
                mBufferSizeInBytes);
        mStatus = Status.READY;
        Log.i(TAG, "createAudioRecord, audio record ready");
    }

    private void createThread() {
        mHandlerThread = new HandlerThread("audio-thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_START:
                        onStart();
                        break;
                    case MSG_RESUME:
                        onResume();
                        break;
                    case MSG_PAUSE:
                        onPause();
                        break;
                    case MSG_STOP:
                        onStop();
                        break;
                    case MSG_DESTROY:
                        onDestroy();
                        break;
                }
                return true;
            }
        });
    }


    @Override
    public void start() {
        mStopReading = false;
        mHandler.sendEmptyMessage(MSG_START);
    }

    private void onStart() {
        if (mStatus != Status.STOP && mStatus != Status.READY) {
            return;
        }
        mStatus = Status.RECORDING;
        while (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.w(TAG,"STATE_UNINITIALIZED");
        }
        mAudioRecord.startRecording();
        Log.i(TAG, "start");
        if (mOnStateListener != null) {
            mOnStateListener.onStart();
        }
        doRecording();
    }

    @Override
    public void resume() {
        mStopReading = false;
        mHandler.sendEmptyMessage(MSG_RESUME);
    }

    private void onResume() {
        if (mStatus != Status.PAUSE) {
            return;
        }
        mStatus = Status.RECORDING;
        mAudioRecord.startRecording();
        Log.d(TAG, "resume");
        doRecording();
    }

    @Override
    public void pause() {
        mStopReading = true;
        mHandler.sendEmptyMessage(MSG_PAUSE);
    }

    private void onPause() {
        if (mStatus != Status.RECORDING) {
            return;
        }
        mStatus = Status.PAUSE;
        mAudioRecord.stop();
        Log.d(TAG, "pause");
    }

    @Override
    public void stop() {
        mStopReading = true;
        mHandler.sendEmptyMessage(MSG_STOP);
    }

    private void onStop() {
        if (mStatus != Status.RECORDING && mStatus != Status.PAUSE) {
            return;
        }
        mStatus = Status.STOP;
        mAudioRecord.stop();
        Log.i(TAG, "stop");
    }

    @Override
    public void destroy() {
        mStopReading = true;
        mHandler.sendEmptyMessage(MSG_DESTROY);
    }

    private void onDestroy() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        mStatus = Status.DESTROY;
        Log.i(TAG, "destroy");
    }

    private void doRecording() {
        try {
            onReading();
        } catch (Exception e) {
            handleError(AudioRecord.ERROR, e);
        }
    }

    private void onReading() {
        int ret;
        while (mAudioRecord != null && !mStopReading && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING
                && mStatus == Status.RECORDING) {
            ret = mAudioRecord.read(mBuffer, 0, mBufferSizeInBytes);
            if (ret >= 0) {
                handleSuccess(mBuffer);
                continue;
            }
            handleErrorRead(ret);
        }
        Log.i(TAG,"stop reading");
    }

    private void handleErrorRead(int ret) {
        switch (ret) {
            case AudioRecord.ERROR_INVALID_OPERATION:
                handleError(ret, new Exception("the object isn't properly initialized"));
                break;
            case AudioRecord.ERROR_BAD_VALUE:
                handleError(ret, new Exception("the parameters don't resolve to valid data and indexes"));
                break;
            case AudioRecord.ERROR_DEAD_OBJECT:
                handleError(ret, new Exception("the object is not valid anymore and" +
                        "needs to be recreated. The dead object error code is not returned if some data was" +
                        "successfully transferred. In this case, the error is returned at the next read"));
                break;
            case AudioRecord.ERROR:
                handleError(ret, new Exception("unknown error"));
                break;
        }
    }

    private void handleSuccess(byte[] buffer) {
        if (mRecordCallback != null) {
            mRecordCallback.onSuccess(buffer);
        }
    }

    private void handleError(int code, Exception e) {
        if (mRecordCallback != null) {
            mRecordCallback.onError(code, e);
        }
    }

    public static final class Builder {
        private int mAudioSource;
        private int mSampleRateInHz;
        private int mChannelConfig;
        private int mAudioFormat;
        private int mBufferSizeInBytes;
        private byte[] mBuffer;

        public Builder() {
            this(DEFAULT_INPUT,
                    DEFAULT_SAMPLE_RATE_IN_HZ,
                    DEFAULT_CHANNEL_CONFIG,
                    DEFAULT_ENCODING,
                    DEFAULT_BUFFER_SIZE_IN_BYTES,
                    new byte[DEFAULT_BUFFER_SIZE_IN_BYTES]);
        }

        public Builder(
                int audioSource,
                int sampleRateInHz,
                int channelConfig,
                int audioFormat,
                int bufferSizeInBytes,
                byte[] buffer) {
            mAudioSource = audioSource;
            mSampleRateInHz = sampleRateInHz;
            mChannelConfig = channelConfig;
            mAudioFormat = audioFormat;
            mBufferSizeInBytes = bufferSizeInBytes;
            mBuffer = buffer;
        }

        public Builder setAudioSource(int audioSource) {
            mAudioSource = audioSource;
            return this;
        }

        public Builder setSampleRateInHz(int sampleRateInHz) {
            mSampleRateInHz = sampleRateInHz;
            return this;
        }

        public Builder setChannelConfig(int channelConfig) {
            mChannelConfig = channelConfig;
            return this;
        }

        public Builder setAudioFormat(int audioFormat) {
            mAudioFormat = audioFormat;
            return this;
        }

        public Builder setBufferSizeInBytes(int bufferSizeInBytes) {
            mBufferSizeInBytes = bufferSizeInBytes;
            return this;
        }

        public DAudioRecord build() {
            return new DAudioRecord(
                    mAudioSource,
                    mSampleRateInHz,
                    mChannelConfig,
                    mAudioFormat,
                    mBufferSizeInBytes,
                    mBuffer);
        }
    }
}
