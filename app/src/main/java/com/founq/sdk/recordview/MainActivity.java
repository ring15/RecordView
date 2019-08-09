package com.founq.sdk.recordview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private String[] PERMISSIONS_STORAGE = new String[]{Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    private RecyclerView mRecyclerView;
    private VideoAdepter mAdepter;

    private List<Video> mVideoNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        mAdepter = new VideoAdepter(this);
        mRecyclerView.setAdapter(mAdepter);
        mRecyclerView.setLayoutManager(manager);
//        mVideoNames = new ArrayList<>();
//        for (int i = 0; i < 10; i++){
//            mVideoNames.add(i + "");
//        }
//        mAdepter.setVideoName(mVideoNames);
//        mAdepter.notifyDataSetChanged();
        getVideoNames();
        requestPermission();
    }

    private void getVideoNames() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio");
        File fileWav = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/wav");
        File[] files = file.listFiles();
        File[] filesWav = fileWav.listFiles();
        if (mVideoNames != null){
            mVideoNames.clear();
        }
        if (files != null){
            for (int i = 0; i < files.length; i++){
                String path = files[i].getAbsolutePath();
                String[] paths = path.split("/");
                if (paths.length > 0){
                    if (paths[paths.length - 1].contains(".amr")){
                        Video video = new Video();
                        video.setDuration(getMediaDuration(path));
                        video.setVideoName(paths[paths.length - 1]);
                        video.setFilePath(path);
                        mVideoNames.add(video);
                    }
                }
            }
        }
        if (filesWav != null){
            for (int i = 0; i < filesWav.length; i++){
                String path = filesWav[i].getAbsolutePath();
                String[] paths = path.split("/");
                if (paths.length > 0){
                    if (paths[paths.length - 1].contains(".wav") ){
                        Video video = new Video();
                        video.setDuration(getMediaDuration(path));
                        video.setVideoName(paths[paths.length - 1]);
                        video.setFilePath(path);
                        mVideoNames.add(video);
                    }
                }
            }
        }
        mAdepter.setVideoName(mVideoNames);
        mAdepter.notifyDataSetChanged();
    }

    private long getMediaDuration(String filePath){
        long duration = 0;
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(filePath);
        String durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (durationStr != null){
            duration = Long.parseLong(durationStr);
        }
        metadataRetriever.release();
        return duration;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getVideoNames();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }
    }


    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_media_recorder:
                startActivity(new Intent(MainActivity.this, MediaRecorderActivity.class));
                break;
            case R.id.btn_audio_recorder:
                startActivity(new Intent(MainActivity.this, AudioRecordActivity.class));
                break;
        }
    }
}
