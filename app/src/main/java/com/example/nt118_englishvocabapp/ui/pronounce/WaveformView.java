package com.example.nt118_englishvocabapp.ui.pronounce;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

public class WaveformView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int barCount = 20;
    private float[] levels;
    private int barColor = 0xFFFFFFFF; // white by default

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(barColor);
        paint.setStyle(Paint.Style.FILL);
        levels = new float[barCount];
        Arrays.fill(levels, 0.02f);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float space = width / (float) barCount;
        float barWidth = Math.max(2f, space * 0.6f);

        for (int i = 0; i < barCount; i++) {
            float cx = space * i + space / 2f;
            float level = Math.max(0.02f, Math.min(1f, levels[i]));
            float barHeight = level * height;
            float top = (height - barHeight) / 2f;
            float left = cx - barWidth / 2f;
            float right = cx + barWidth / 2f;
            float bottom = top + barHeight;
            canvas.drawRoundRect(left, top, right, bottom, barWidth/2f, barWidth/2f, paint);
        }
    }

    /**
     * Feed a new level (0..1). The view shifts older values and draws the new one.
     * Call this from your audio capture (normalized amplitude) for a real waveform.
     */
    public void setLevel(float level) {
        if (levels == null || levels.length == 0) return;
        // shift left
        System.arraycopy(levels, 1, levels, 0, levels.length - 1);
        // insert smoothed value at end
        levels[levels.length - 1] = Math.max(0f, Math.min(1f, level));
        invalidate();
    }

    public void setBarColor(int color) {
        barColor = color;
        paint.setColor(barColor);
        invalidate();
    }

    public void setBarCount(int count) {
        if (count <= 0) return;
        barCount = count;
        levels = new float[barCount];
        Arrays.fill(levels, 0.02f);
        invalidate();
    }
}
