package com.founq.sdk.recordview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.founq.sdk.recordview.widget.VoiceLineView;

import java.lang.ref.WeakReference;

public class AudioRecordActivity extends AppCompatActivity {

    private ImageView mVolumeIv, mIvPauseContinue, mIvComplete;
    private VoiceLineView voicLine;
    private TextView mRecordHintTv;
    private boolean isStart = false;
    private boolean isCreate = false;
    private MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        mVolumeIv = findViewById(R.id.iv_voice);
        voicLine = findViewById(R.id.voicLine);
        mRecordHintTv = findViewById(R.id.tv_length);
        mIvPauseContinue = findViewById(R.id.iv_continue_or_pause);
        mIvComplete = findViewById(R.id.iv_complete);
        mHandler = new MyHandler(this);
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_continue_or_pause:
                if (isStart) {
                    isStart = false;
                    mIvPauseContinue.setImageResource(R.drawable.icon_continue);
                    voicLine.setPause();
                    AudioRecorderManager.getInstance().pauseRecording();
                } else {
                    if (!isCreate) {
                        String path = System.currentTimeMillis() + "";
                        AudioRecorderManager.getInstance().createAudio(path);
                        isCreate = true;
                    }
                    isStart = true;
                    mIvPauseContinue.setImageResource(R.drawable.icon_pause);
                    voicLine.setContunue();
                    AudioRecorderManager.getInstance().startRecording(mHandler);
                }
                break;
            case R.id.iv_complete:
                isStart = false;
                mIvPauseContinue.setImageResource(R.drawable.icon_continue);
                voicLine.setPause();
                AudioRecorderManager.getInstance().stopRecording();
                isCreate = false;
                mRecordHintTv.setText("00:00:00");
                break;
        }
    }

    private class MyHandler extends Handler {

        private WeakReference<AudioRecordActivity> mReference;

        public MyHandler(AudioRecordActivity activity) {
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            mReference.get().voicLine.setVolume(msg.arg1);
            int time = msg.arg2 / 10;
            Log.i("test", msg.arg1 + ", " + time);
            String showTime = "00:00:00";
            if (time < 10) {
                showTime = "00:00:0" + time;
            } else if (time < 60) {
                showTime = "00:00:" + time;
            } else if (time < 3600) {
                int minute = time / 60;
                int second = time % 60;
                showTime = "00:" + (minute < 10 ? ("0" + minute) : minute) + ":" + (second < 10 ? ("0" + second) : second);
            } else {
                int hour = time / 3600;
                int temp = time % 3600;
                int minute = temp / 60;
                int second = temp % 60;
                showTime = (hour < 10 ? ("0" + hour) : hour) + ":" + (minute < 10 ? ("0" + minute) : minute) + ":" + (second < 10 ? ("0" + second) : second);
            }
            mReference.get().mRecordHintTv.setText(showTime);
        }
    }
}
