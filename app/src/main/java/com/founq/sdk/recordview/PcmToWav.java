package com.founq.sdk.recordview;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by ZhouMeng on 2018/8/31.
 * 将pcm文件转化为wav文件
 * pcm是无损wav文件中音频数据的一种编码方式，pcm加上wav文件头就可以转为wav格式，但wav还可以用其它方式编码。
 * 此类就是通过给pcm加上wav的文件头，来转为wav格式
 */
public class PcmToWav {
    /**
     * 合并多个pcm文件为一个wav文件
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    public static boolean mergePCMFilesToWAVFile(List<String> filePathList, String destinationPath) {
        File[] file = new File[filePathList.size()];
        byte buffer[] = null;

        int TOTAL_SIZE = 0;
        int fileNum = filePathList.size();

        for (int i = 0; i < fileNum; i++) {
            file[i] = new File(filePathList.get(i));
            TOTAL_SIZE += file[i].length();
        }

        // 填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        // 长度字段 = 内容的大小（TOTAL_SIZE) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = TOTAL_SIZE + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 2;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 8000;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = TOTAL_SIZE;

        byte[] h = null;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.e("PcmToWav", e1.getMessage());
            return false;
        }

        // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
        if (h.length != 44) {
            return false;
        }

        //先删除目标文件
        File destFile = new File(destinationPath);
        if (destFile.exists()) {
            destFile.delete();
        }

        //合成所有的pcm文件的数据，写到目标文件
        try {
            buffer = new byte[1024 * 4]; // Length of All Files, Total Size
            InputStream inStream = null;
            OutputStream ouStream = null;

            ouStream = new BufferedOutputStream(new FileOutputStream(
                    destinationPath));
            ouStream.write(h, 0, h.length);
            for (int j = 0; j < fileNum; j++) {
                inStream = new BufferedInputStream(new FileInputStream(file[j]));
                int size = inStream.read(buffer);
                while (size != -1) {
                    ouStream.write(buffer);
                    size = inStream.read(buffer);
                }
                inStream.close();
            }
            ouStream.close();
        } catch (IOException ioe) {
            ioe.getMessage();
            return false;
        }
        FileUtils.clearFiles(filePathList);
//        File wavFile = new File(new File(destinationPath).getParent());
//        if (wavFile.exists()) {
//            FileUtils.deleteFile(wavFile);
//        }

        return true;
    }


    /**
     * PCM文件转WAV文件
     *
     * @param filePathList   pcm文件路径集合
     * @param outWavFilePath 输出WAV文件路径
     * @param sampleRate     采样率，例如44100
     * @param channels       声道数 单声道：1或双声道：2
     * @param audioFormat    位深度
     */
    public void convertPcm2Wav(List<String> filePathList, String outWavFilePath, int sampleRate,
                               int channels, int audioFormat) {
        File[] file = new File[filePathList.size()];
        int totalAudioLen = 0;
        for (int i = 0; i < file.length; i++) {
            file[i] = new File(filePathList.get(i));
            totalAudioLen += file[i].length();
        }
        //总大小，由于不包括RIFF和WAV，所以是44 - 8 = 36，在加上PCM文件大小
        long totalDataLen = totalAudioLen + 36;

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        byte[] data = new byte[1024 * 4];
        try {
            //采样字节byte率
            long byteRate = sampleRate * channels * audioFormat;

            // 先删除目标文件
            File destFile = new File(outWavFilePath);
            if (destFile.exists()) {
                destFile.delete();
            }

            outputStream = new FileOutputStream(outWavFilePath);


            //把wav的头填进去了
            writeWaveFileHeader(outputStream, totalAudioLen, totalDataLen, sampleRate, channels, byteRate);

            for (int i = 0; i < filePathList.size(); i++) {
                inputStream = new FileInputStream(file[i]);
                //将其他部分写进去
                int length = 0;
                while ((length = inputStream.read(data)) > 0) {
                    outputStream.write(data, 0, length);
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileUtils.clearFiles(filePathList);
        }
    }


    /**
     * 输出WAV文件
     *
     * @param outputStream  WAV输出文件流
     * @param totalAudioLen 整个音频PCM数据大小
     * @param totalDataLen  整个数据大小
     * @param sampleRate    采样率
     * @param channels      声道数
     * @param byteRate      采样字节byte率
     */
    private void writeWaveFileHeader(FileOutputStream outputStream, long totalAudioLen,
                                     long totalDataLen, int sampleRate,
                                     int channels, long byteRate) throws IOException {
        //wav头
        byte[] header = new byte[44];
        //下边的真的看不懂，我只知道wav头信息由44个字节组成，含义如下
        /* 4字节数据，内容为“RIFF”，表示资源交换文件标识
        4字节数据，内容为一个整数，表示从下个地址开始到文件尾的总字节数
        4字节数据，内容为“WAVE”，表示WAV文件标识
        4字节数据，内容为“fmt ”，表示波形格式标识（fmt ），最后一位空格。
        4字节数据，内容为一个整数，表示PCMWAVEFORMAT的长度
        2字节数据，内容为一个短整数，表示格式种类（值为1时，表示数据为线性PCM编码）
        2字节数据，内容为一个短整数，表示通道数，单声道为1，双声道为2
        4字节数据，内容为一个整数，表示采样率，比如44100
        4字节数据，内容为一个整数，表示波形数据传输速率（每秒平均字节数），大小为 采样率 * 通道数 * 采样位数
        2字节数据，内容为一个短整数，表示DATA数据块长度，大小为 通道数 * 采样位数
        2字节数据，内容为一个短整数，表示采样位数，即PCM位宽，通常为8位或16位
        4字节数据，内容为“data”，表示数据标记符
        4字节数据，内容为一个整数，表示接下来声音数据的总大小
         */
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        outputStream.write(header, 0, 44);
    }
}