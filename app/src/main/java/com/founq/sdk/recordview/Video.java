package com.founq.sdk.recordview;

/**
 * Created by ring on 2019/8/9.
 */
public class Video {

    private String videoName;
    private String filePath;
    private long duration;

    public String getVideoName() {
        return videoName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getDuration() {
        return duration;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
