package com.example.mamn01_g2.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.mamn01_g2.R;

import java.util.Locale;

public class ClockView extends View {
    private Paint circlePaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF rectF;

    private float rotationDegrees = 0;
    private int seconds = 0;
    private float progressAngle = 0;
    private long totalInitialTimeInMillis = 0;

    public ClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int bgColor = ContextCompat.getColor(context, R.color.orange);
        int progColor = ContextCompat.getColor(context, R.color.progress_color);
        int txtColor = ContextCompat.getColor(context, R.color.text_color);
        float strokeW = 15f;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ClockView, 0, 0);

            try {
                bgColor = a.getColor(R.styleable.ClockView_clockBackgroundColor, bgColor);
                progColor = a.getColor(R.styleable.ClockView_clockProgressColor, progColor);
                txtColor = a.getColor(R.styleable.ClockView_clockTextColor, txtColor);
                strokeW = a.getDimension(R.styleable.ClockView_clockStrokeWidth, strokeW);
            } finally {
                a.recycle();
            }
        }

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(bgColor);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(10f);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeW);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(txtColor);
        textPaint.setTextSize(100f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();

        if (!isInEditMode()) {
            android.graphics.Typeface customFont = ResourcesCompat.getFont(context, R.font.rubik_mono);
            textPaint.setTypeface(customFont);
        }
    }

    /**
     * Call this when setting a brand new time (e.g., when dialing the phone).
     * This establishes the "100% full" mark for the progress arc.
     */
    public void setInitialTime(long millis) {
        this.totalInitialTimeInMillis = millis;
        updateTimeDisplay(millis);
    }

    /**
     * Call this every second while the timer is running down.
     */
    public void updateTime(long millisRemaining) {
        updateTimeDisplay(millisRemaining);
    }

    // A private helper method to handle the math and redrawing
    private void updateTimeDisplay(long millis) {
        // 1. Convert millis back to total seconds for the text display
        this.seconds = (int) (millis / 1000);

        // 2. Calculate the progress arc angle (360 degrees = full)
        if (totalInitialTimeInMillis > 0) {
            // Calculate what percentage of time is left
            float percentageRemaining = (float) millis / totalInitialTimeInMillis;

            // Convert that percentage into degrees (e.g., 50% = 180 degrees)
            this.progressAngle = percentageRemaining * 360f;
        } else {
            this.progressAngle = 0f;
        }

        // Force the view to redraw itself with the new values!
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float radius = Math.min(width, height) / 3f;
        float centerX = width / 2f;
        float centerY = height / 2f;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint);

        // Draw progress arc
        canvas.drawArc(rectF, -90, progressAngle, false, progressPaint);

        canvas.save();
        canvas.rotate(-rotationDegrees, centerX, centerY);

        // Draw time text
        String timeText = formatTime(seconds);
        float textOffset = (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText(timeText, centerX, centerY - textOffset, textPaint);

        canvas.restore();
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    public void setRotationDegrees(float degrees) {
        this.rotationDegrees = degrees;
        invalidate();
    }

    public void setTime(int seconds) {
        this.seconds = Math.max(0, seconds);
        this.progressAngle = (this.seconds % 60) * 6f;
        invalidate();
    }


}
