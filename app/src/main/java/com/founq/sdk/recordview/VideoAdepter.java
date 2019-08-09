package com.founq.sdk.recordview;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

/**
 * Created by ring on 2019/8/9.
 */
public class VideoAdepter extends RecyclerView.Adapter<VideoAdepter.MyHolder> {

    private Context mContext;

    private List<Video> videoName;

    private AnimationDrawable mAnimationDrawable;

    private int select = -1;

    private MediaPlayer mPlayer;

    private long toatleDeration;

    private Handler handler;

    private MyThread thread = null;

    public void setVideoName(List<Video> videoName) {
        this.videoName = videoName;
    }

    public VideoAdepter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_voice_list, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {
        if (videoName != null) {
            holder.mVideoName.setText(videoName.get(position).getVideoName());
            holder.mLengthText.setText(videoName.get(position).getDuration() / 1000 + "s");
            holder.mItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handler = new Handler(){
                        @Override
                        public void handleMessage(@NonNull Message msg) {
                            super.handleMessage(msg);
                            holder.mLengthText.setText(msg.arg1 + "s");
                            boolean running = (boolean) msg.obj;
                            if (!running){
                                if (mAnimationDrawable != null) {
                                    mAnimationDrawable.stop();
                                    mAnimationDrawable.selectDrawable(0);
                                }
                                //停止播放
                                if (mPlayer != null){
                                    mPlayer.release();
                                    mPlayer = null;
                                }
                                select = -1;
                                toatleDeration = videoName.get(position).getDuration();
                                holder.mVideoName.setText(videoName.get(position).getVideoName());
                            }
                        }
                    };
                    toatleDeration = videoName.get(position).getDuration();
                    if (mAnimationDrawable != null) {
                        mAnimationDrawable.stop();
                        mAnimationDrawable.selectDrawable(0);
                    }
                    if (position == select) {
                        //停止播放
                        if (mPlayer != null){
                            mPlayer.release();
                            mPlayer = null;
                        }
                        if (thread != null){
                            thread.exit();
                            thread = null;
                        }
                        select = -1;
                        toatleDeration = videoName.get(position).getDuration();
                        holder.mVideoName.setText(videoName.get(position).getVideoName());
                    } else {
                        mAnimationDrawable = (AnimationDrawable) holder.mVoiceImg.getBackground();
                        mAnimationDrawable.start();
                        mPlayer = new MediaPlayer();
                        try {
                            mPlayer.setDataSource(videoName.get(position).getFilePath());
                            mPlayer.prepare();
                            mPlayer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        select = position;
                        thread = new MyThread();
                        thread.start();
                    }
                }
            });
        }
    }

    private class MyThread extends Thread{
        private long temp = toatleDeration;
        private volatile boolean running = true;

        public void exit(){
            running = false;
        }
        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(1000);
                    toatleDeration -= 1000;
                    if (toatleDeration < 0){
                        Message message = new Message();
                        message.arg1 = (int) (temp / 1000);
                        message.obj = false;
                        handler.sendMessage(message);
                        running = false;
                    }else {
                        Message message = new Message();
                        message.arg1 = (int) (toatleDeration / 1000);
                        message.obj = true;
                        handler.sendMessage(message);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return videoName == null ? 0 : videoName.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {

        private ImageView mVoiceImg;
        private TextView mVideoName;
        private TextView mLengthText;
        private LinearLayout mItem;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            mLengthText = itemView.findViewById(R.id.tv_length);
            mVideoName = itemView.findViewById(R.id.tv_video_name);
            mVoiceImg = itemView.findViewById(R.id.iv_voice);
            mItem = (LinearLayout) itemView;
        }
    }
}
