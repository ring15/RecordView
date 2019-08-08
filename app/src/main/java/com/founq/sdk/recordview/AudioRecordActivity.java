package com.founq.sdk.recordview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.founq.sdk.recordview.widget.VoiceLineView;

public class AudioRecordActivity extends AppCompatActivity {

    private ImageView mVolumeIv, mIvPauseContinue, mIvComplete;
    private VoiceLineView voicLine;
    private TextView mRecordHintTv;
    private boolean isStart = false;
    private boolean isCreate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        mVolumeIv = findViewById(R.id.iv_voice);
        voicLine = findViewById(R.id.voicLine);
        mRecordHintTv = findViewById(R.id.tv_length);
        mIvPauseContinue = findViewById(R.id.iv_continue_or_pause);
        mIvComplete = findViewById(R.id.iv_complete);
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
                    if (!isCreate){
                        String path = System.currentTimeMillis() + "";
                        AudioRecorderManager.getInstance().createAudio(path);
                        isCreate = true;
                    }
                    isStart = true;
                    mIvPauseContinue.setImageResource(R.drawable.icon_pause);
                    voicLine.setContunue();
                    AudioRecorderManager.getInstance().startRecording();
                }
                break;
            case R.id.iv_complete:
                if (isStart) {
                    isStart = false;
                    mIvPauseContinue.setImageResource(R.drawable.icon_continue);
                    voicLine.setPause();
                    AudioRecorderManager.getInstance().stopRecording();
                    isCreate = false;
                }
                break;
        }
    }

}
