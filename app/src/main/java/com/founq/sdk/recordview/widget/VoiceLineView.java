package com.founq.sdk.recordview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.founq.sdk.recordview.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ring on 2019/8/7.
 */
public class VoiceLineView extends View {


    private int middleLineColor = Color.BLACK;
    private int voiceLineColor = Color.BLACK;
    private float middleLineHeight = 4;
    private Paint paint;
    private Paint paintVoicLine;

    /**
     * 灵敏度
     */
    private int sensibility = 4;

    private float maxVolume = 100;


    private float translateX = 0;

    /**
     * 振幅
     */
    private float amplitude = 1;
    /**
     * 音量
     */
    private float volume;
    private int fineness = 1;
    private long speedY = 50;
    private long lastTime = 0;
    private int lineSpeed = 90;

    List<Path> paths = null;
    /**
     * 继续或停止震动，默认继续
     */
    private boolean continueOrPause = false;

    public VoiceLineView(Context context) {
        this(context, null);
    }

    public VoiceLineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.VoiceLineView);
        voiceLineColor = array.getColor(R.styleable.VoiceLineView_voiceLine, Color.BLACK);
        maxVolume = array.getFloat(R.styleable.VoiceLineView_maxVolume, 100);
        sensibility = array.getInt(R.styleable.VoiceLineView_sensibility, 4);

        middleLineColor = array.getColor(R.styleable.VoiceLineView_middleLine, Color.BLACK);
        middleLineHeight = array.getDimension(R.styleable.VoiceLineView_middleLineHeight, 4);
        lineSpeed = array.getInt(R.styleable.VoiceLineView_lineSpeed, 90);
        fineness = array.getInt(R.styleable.VoiceLineView_fineness, 1);
        paths = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            paths.add(new Path());
        }
        volume = maxVolume / 10;
        array.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawMiddleLine(canvas);
        if (continueOrPause) {
            drawVoiceLine(canvas);
            invalidate();
        }
    }

    private void drawVoiceLine(Canvas canvas) {
        lineChange();
        if (paintVoicLine == null) {
            paintVoicLine = new Paint();
            paintVoicLine.setColor(voiceLineColor);
            paintVoicLine.setAntiAlias(true);
            paintVoicLine.setStyle(Paint.Style.STROKE);
            paintVoicLine.setStrokeWidth(2);
        }
        canvas.save();
        int moveY = getHeight() / 2;
        // 将所有路径的起始点移动到中间位置，和标尺线位置一致
        for (int i = 0; i < paths.size(); i++) {
            paths.get(i).reset();
            paths.get(i).moveTo(getWidth(), getHeight() / 2);
        }
        // 先把fineness当做1来看，相当于遍历像素点
        for (float i = getWidth() - 1; i >= 0; i -= fineness) {
            float apartWidth = getWidth() / 2;//想把横坐标分成4份，每一份是一个sin函数
            float sinOffset = i % apartWidth;//这样取余，每部分都一样了
            float percent = sinOffset / apartWidth; //百分比
            float angle = (float) (percent * 360 * Math.PI / 180);//转换成角度

            //上边的方法太规矩了，完整的sin周期，不好玩，换一个，要8分之一的sin周期吧

            float maxHeightOffset = (volume / maxVolume) * getHeight() * 2 / 3;//上下的最大偏移量就是一半再一半的高度了
            maxHeightOffset *= (float) Math.sin((i % getWidth()) / getWidth() * Math.PI);
            for (int n = 1; n <= paths.size(); n++) {
                float realHeight = maxHeightOffset * n / paths.size();//根据n不同，最大高度不一样
                //增加了一个横向的偏移量,加一个和n有关的量，能让分部的更好看一些
                paths.get(n - 1).lineTo(i, (float) (realHeight * Math.sin(angle - Math.pow(1.23, n) * Math.PI / 180 - translateX) + moveY));
            }
        }
        for (int n = 0; n < paths.size(); n++) {
            //设置线条的透明度
            if (n == paths.size() - 1) {
                paintVoicLine.setAlpha(255);
            } else {
                //这个要是设置成从255往下降，是属于渐变色，但是不好看，所以只有一条最明显，剩下的透明度可以低一点
                paintVoicLine.setAlpha(n * 160 / paths.size());
            }
            //绘制透明度不为0的线条
            if (paintVoicLine.getAlpha() > 0) {
                canvas.drawPath(paths.get(n), paintVoicLine);
            }
        }
        canvas.restore();

    }

    private void lineChange() {
        if (System.currentTimeMillis() - lastTime > lineSpeed) {
            lastTime = System.currentTimeMillis();
            translateX += 1.5;
        }
    }

    private void drawMiddleLine(Canvas canvas) {
        if (paint == null) {
            paint = new Paint();
            paint.setColor(middleLineColor);
            //抗锯齿
            paint.setAntiAlias(true);
        }
        //canvas.save();与canvas.restore();一般结合使用，.save()函数在前，.restore()函数在后，用来保证在这两个函数之间所做的操作不会对原来在canvas上所画图形产生影响
        canvas.save();
        canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2 + middleLineHeight / 2, paint);
        canvas.restore();
    }

    public void setPause() {
        continueOrPause = false;
    }

    public void setContunue() {
        continueOrPause = true;
        invalidate();
    }

    public void setVolume(int volume) {
        if (volume < maxVolume / 10){
            this.volume = maxVolume / 10;
        }else if (volume < maxVolume) {
            this.volume = volume;
        }else {
            this.volume = maxVolume;
        }
    }
}
