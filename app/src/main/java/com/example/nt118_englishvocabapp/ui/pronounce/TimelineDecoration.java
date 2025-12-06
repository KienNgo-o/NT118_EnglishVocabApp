package com.example.nt118_englishvocabapp.ui.pronounce;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TimelineDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;

    public TimelineDecoration(@ColorInt int color, float lineWidthDp) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        int lineWidthPx = (int) (lineWidthDp * Resources.getSystem().getDisplayMetrics().density + 0.5f);
        paint.setStrokeWidth(lineWidthPx);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);
        int childCount = parent.getChildCount();
        if (childCount == 0) return;

        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        float centerX = (left + right) / 2f; // center of the padded area

        // draw continuous vertical line spanning the visible RecyclerView area (full straight line)
        float top = parent.getPaddingTop();
        float bottom = parent.getHeight() - parent.getPaddingBottom();

        canvas.drawLine(centerX, top, centerX, bottom, paint);
    }
}
