package com.founq.sdk.recordview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.founq.sdk.recordview.widget.VoiceLineView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaRecorderActivity extends AppCompatActivity {

    private ImageView mVolumeIv, mIvPauseContinue, mIvComplete;
    private VoiceLineView voicLine;
    private TextView mRecordHintTv;
    private boolean isStart = false;
    private boolean isCreate = false;
    private File file;

    private MyHandler mMyHandler;

    private MediaRecorder mMediaRecorder;
    private int toatleTime = 0;

    private ObtainDecibelThread mThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);
        mVolumeIv = findViewById(R.id.iv_voice);
        voicLine = findViewById(R.id.voicLine);
        mRecordHintTv = findViewById(R.id.tv_length);
        mIvPauseContinue = findViewById(R.id.iv_continue_or_pause);
        mIvComplete = findViewById(R.id.iv_complete);
        init();
    }

    private void init() {
        mRecordHintTv.setText("00:00:00");
        mMyHandler = new MyHandler(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_0:
                voicLine.setVolume(0);
                break;
            case R.id.btn_30:
                voicLine.setVolume(30);
                break;
            case R.id.btn_40:
                voicLine.setVolume(40);
                break;
            case R.id.btn_120:
                voicLine.setVolume(120);
                break;
            case R.id.iv_continue_or_pause:
                if (isStart) {
                    isStart = false;
                    mIvPauseContinue.setImageResource(R.drawable.icon_continue);
                    voicLine.setPause();
                    stopRecorder(true);
                } else {
                    isStart = true;
                    mIvPauseContinue.setImageResource(R.drawable.icon_pause);
                    voicLine.setContunue();
                    starRecorder();
                }
                break;
            case R.id.iv_complete:
                if (isStart) {
                    isStart = false;
                    mIvPauseContinue.setImageResource(R.drawable.icon_continue);
                    voicLine.setPause();
                    stopRecorder(true);
                }
                break;
        }
    }


    public static File recAudioDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private void starRecorder() {
        mMediaRecorder = null;
        mMediaRecorder = new MediaRecorder();//初始化MediaRecorder
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//设置音频来源（MIC表示麦克风，需要权限）
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);//设置输出格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);//设置编码方式
            String path = recAudioDir(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio")
                    .getAbsolutePath();//存储路径，recAudioDir是判断路径是否存在，如果不存在则新建
            file = new File(path, System.currentTimeMillis() + ".amr");//存储文件名
            mMediaRecorder.setOutputFile(file.getAbsolutePath());//指定输出路径
            mMediaRecorder.prepare();//调用prepare方法
            if (isStart) {//这个是一个判断是否要录音的标志，可忽略
                mMediaRecorder.start();//开始录音
                isCreate = true;//这个是一个判断录音是否开始的标志
                if (mThread == null){
                    mThread = new ObtainDecibelThread();//开启线程，用来获取录音信息，包括录音时长、分贝
                    mThread.start();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 监听录音声音频率大小
     */
    private class ObtainDecibelThread extends Thread {

        private volatile boolean running = true;

        public void exit() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(100);
                    toatleTime += 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mMediaRecorder == null || !running) {
                    break;
                }
                try {
                    final double ratio = mMediaRecorder.getMaxAmplitude() / 150;
                    if (ratio != 0) {
                        double db = 0;// 分贝
                        if (ratio > 1)
                            db = (int) (20 * Math.log10(ratio));
                        Message message = new Message();
                        message.arg1 = (int) db;
                        message.arg2 = toatleTime;
                        mMyHandler.sendMessage(message);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /**
     * 释放所有资源
     * @param release
     */
    private void stopRecorder(boolean release) {
        if (mMediaRecorder != null && isCreate) {
            mMediaRecorder.stop();
            if (release) {
                mMediaRecorder.release();
            }
            isCreate = false;
        }
        if (mThread != null){
            mThread.exit();
            mThread = null;
        }
        toatleTime = 0;
    }

    private class MyHandler extends Handler {

        private WeakReference<MediaRecorderActivity> mReference;

        public MyHandler(MediaRecorderActivity activity) {
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            mReference.get().voicLine.setVolume(msg.arg1);
            int time = msg.arg2 / 10;
            String showTime = "00:00:00";
            if (time < 10) {
                showTime = "00:00:0" + time;
            } else if (time < 60) {
                showTime = "00:00:" + time;
            } else if (time < 3600) {
                int minute = time / 60;
                int second = time % 60;
                showTime = "00:" + (minute < 10 ? ("0" + minute) : minute) + ":" + (second < 10 ? ("0" + second) : second);
            } else{
                int hour = time / 3600;
                int temp = time % 3600;
                int minute = temp / 60;
                int second = temp % 60;
                showTime = (hour < 10 ? ("0" + hour) : hour) + ":" + (minute < 10 ? ("0" + minute) : minute) + ":" + (second < 10 ? ("0" + second) : second);
            }
            mReference.get().mRecordHintTv.setText(showTime);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaRecorder != null && isCreate) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            isCreate = false;
        }
        if (mThread != null){
            mThread.exit();
            mThread = null;
        }
        toatleTime = 0;
    }
}
