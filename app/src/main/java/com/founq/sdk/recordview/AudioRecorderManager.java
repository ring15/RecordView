package com.founq.sdk.recordview;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ring on 2019/8/8.
 */
public class AudioRecorderManager {

    //录音状态
    public final static int ACTION_INVALID = 0;//未开始
    public final static int ACTION_START_RECORD = 1;//录音
    public final static int ACTION_STOP_RECORD = 2;//停止
    public final static int ACTION_RESUME_RECORD = 3;//预备
    public final static int ACTION_PAUSE_RECORD = 4;//暂停


    //音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    /**
     * 采样率即采样频率，采样频率越高，能表现的频率范围就越大
     * 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
     */
    private final static int AUDIO_SAMPLE_RATE = 16000;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 位深度也叫采样位深，音频的位深度决定动态范围
     * 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
     */
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;

    //录音对象
    private AudioRecord audioRecord;

    /**
     * 播放声音
     * 一些必要的参数，需要和AudioRecord一一对应，否则声音会出错
     */
    private AudioTrack audioTrack;

    //录音状态,默认未开始
    private int status = ACTION_INVALID;

    //文件名
    private String fileName;

    //录音文件集合
    private List<String> filesName = new ArrayList<>();

    /**
     * 重置，删除所有的pcm文件
     */
    private boolean isReset = false;

    /**
     * 创建带有缓存的线程池
     * 当执行第二个任务时第一个任务已经完成，会复用执行第一个任务的线程，而不用每次新建线程。
     * 如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程。
     * 一开始选择错误，选用newSingleThreadExecutor，导致停止后在录制，出现一堆问题
     */
    private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    private int toatleTime = 0;

    private static AudioRecorderManager mRecorderManager;

    private Handler mHandler;

    private AudioRecorderManager() {

    }

    public static AudioRecorderManager getInstance() {
        if (mRecorderManager == null) {
            synchronized (AudioRecorderManager.class) {
                if (mRecorderManager == null) {
                    mRecorderManager = new AudioRecorderManager();
                }
            }
        }
        return mRecorderManager;
    }

    public void setReset() {
        isReset = true;
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    public void createAudio(String fileName) {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
        this.fileName = fileName;
        status = ACTION_RESUME_RECORD;

        //api21及以上
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(AUDIO_SAMPLE_RATE)
                .setEncoding(AUDIO_ENCODING).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();

        audioTrack = new AudioTrack(audioAttributes, audioFormat, bufferSizeInBytes,
                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    /**
     * 开始录制
     */
    public void startRecording(Handler handler) {
        mHandler = handler;
        if (status == ACTION_INVALID || TextUtils.isEmpty(fileName)) {
            throw new IllegalStateException("请检查录音权限");
        }
        if (status == ACTION_START_RECORD) {
            throw new IllegalStateException("正在录音");
        }
        audioRecord.startRecording();
        if (mObtainDecibelThread != null) {
            mObtainDecibelThread.begin();
        }
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                writeDataToFile();
            }
        });
    }

    public void pauseRecording() {
        if (status != ACTION_START_RECORD) {
            throw new IllegalStateException("没有在录音");
        } else {
            audioRecord.stop();
            status = ACTION_PAUSE_RECORD;
            if (mObtainDecibelThread != null) {
                mObtainDecibelThread.pause();
            }
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        if (status == ACTION_INVALID || status == ACTION_RESUME_RECORD) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            audioRecord.stop();
            status = ACTION_STOP_RECORD;
            toatleTime = 0;
            realese();
        }
    }

    /**
     * 释放资源
     */
    public void realese() {
        if (filesName.size() > 0) {
            final List<String> filePaths = new ArrayList<>();
            for (String fileName : filesName) {
                filePaths.add(FileUtils.getPcmFileAbsolutePath(fileName));
            }
            filesName.clear();
            if (isReset) {
                isReset = false;
                FileUtils.clearFiles(filePaths);
            } else {
                //将多个pcm文件转化为wav文件
                cachedThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        pcmFilesToWavFile(filePaths);
                    }
                });
            }
        }
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }

        if (mObtainDecibelThread != null) {
            mObtainDecibelThread.exit();
            mObtainDecibelThread = null;
        }
        status = ACTION_INVALID;
    }

    public void releaseAudioTrack() {
        if (audioTrack == null) {
            return;
        }
        if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack.stop();
        }
        audioTrack.release();
        audioTrack = null;
    }

    /**
     * 将pcm合并成wav
     *
     * @param filePaths
     */
    private void pcmFilesToWavFile(List<String> filePaths) {
        String filePath = FileUtils.getWavFileAbsolutePath(fileName);
        PcmToWav.mergePCMFilesToWAVFile(filePaths, filePath);
//        convertPcm2Wav(filePaths, filePath, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        fileName = null;
    }

    /**
     * 将音频信息写入文件，io操作，放在其他线程中
     */
    private void writeDataToFile() {
        final byte[] audioData = new byte[bufferSizeInBytes];
        FileOutputStream fileOutputStream = null;
        String currentFileName = fileName;
        if (status == ACTION_PAUSE_RECORD) {
            //假如是暂停录音，将文件名后面加个数字，防止重名文件内容被覆盖
            currentFileName += filesName.size();
        }
        filesName.add(currentFileName);
        File file = new File(FileUtils.getPcmFileAbsolutePath(currentFileName));
        if (file.exists()) {
            file.delete();
        }
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //将录音状态设置为正在录音状态
        status = ACTION_START_RECORD;
        while (status == ACTION_START_RECORD) {
            final int readSize = audioRecord.read(audioData, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize && fileOutputStream != null) {
                mAudioData = audioData;
                mReadSize = readSize;
                if (mObtainDecibelThread == null) {
                    mObtainDecibelThread = new ObtainDecibelThread();
                    mObtainDecibelThread.start();
                }
                try {
                    fileOutputStream.write(audioData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] mAudioData = new byte[bufferSizeInBytes];
    private int mReadSize;
    private ObtainDecibelThread mObtainDecibelThread;

    /**
     * 监听录音声音频率大小
     */
    private class ObtainDecibelThread extends Thread {

        private volatile boolean running = true;
        private volatile boolean pause = false;

        public void exit() {
            running = false;
        }

        public void begin() {
            pause = false;
        }

        public void pause() {
            pause = true;
        }

        @Override
        public void run() {
            while (running) {
                if (!pause) {
                    try {
                        Thread.sleep(100);
                        toatleTime += 1;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long v = 0;
                    for (int i = 0; i < mAudioData.length; i++) {
                        v += mAudioData[i] * mAudioData[i];
                    }
                    if (mReadSize != 0) {
                        //平方和除以数据总长度，得到音量大小
                        double mean = v / mReadSize;
                        double volume = 10 * Math.log10(mean);
                        if (mHandler != null) {
                            Message message = new Message();
                            message.arg1 = (int) volume;
                            message.arg2 = toatleTime;
                            mHandler.sendMessage(message);
                        }
                    }
                }
            }
        }

    }


}
