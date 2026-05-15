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
    private Paint progressPaint;
    private Paint textPaint;
    private RectF rectF;

    private float rotationDegrees = 0;
    private int seconds = 0;
    private float progressAngle = 0;
    private long totalInitialTimeInMillis = 0;

    private float progressRadius;

    public ClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int progressColor = ContextCompat.getColor(context, R.color.progress);
        int textColor = ContextCompat.getColor(context, R.color.text);

        float strokeWidth = getResources().getDimension(R.dimen.clock_progress_stroke);
        float clockTextSize = getResources().getDimension(R.dimen.clock_text_size);

        float targetDiameter = getResources().getDimension(R.dimen.clock_progress_diameter);
        progressRadius = targetDiameter / 2f;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ClockView, 0, 0);

            try {
                progressColor = a.getColor(R.styleable.ClockView_clockProgressColor, progressColor);
                textColor = a.getColor(R.styleable.ClockView_clockTextColor, textColor);
                strokeWidth = a.getDimension(R.styleable.ClockView_clockStrokeWidth, strokeWidth);
                clockTextSize = a.getDimension(R.styleable.ClockView_clockTextSize, clockTextSize);
            } finally {
                a.recycle();
            }
        }

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(clockTextSize);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();

        if (!isInEditMode()) {
            android.graphics.Typeface customFont = ResourcesCompat.getFont(context, R.font.rubik_mono);
            textPaint.setTypeface(customFont);
        }
    }

    /**
     * Call this when setting a brand new time (e.g., when dialing the phone).
     * This establishes the "100% full" mark for the countdown, BUT draws the
     * progress relative to a 60-minute dial so you can see it grow!
     */
    public void setInitialTime(long millis) {
        this.totalInitialTimeInMillis = millis;
        this.seconds = (int) (millis / 1000);

        // 60 minutes = 360 degrees.
        float minutes = millis / 60000f;
        this.progressAngle = (minutes / 60f) * 360f;

        if (this.progressAngle > 360f) {
            this.progressAngle = 360f;
        }

        invalidate(); // Tell Android to redraw!
    }

    /**
     * Call this every second while the timer is running down.
     */
    public void updateTime(long millisRemaining) {
        this.seconds = (int) (millisRemaining / 1000);

        // When running, we shrink the circle from 100% down to 0% based on the initial time!
        if (totalInitialTimeInMillis > 0) {
            float percentageRemaining = (float) millisRemaining / totalInitialTimeInMillis;
            this.progressAngle = percentageRemaining * 360f;
        } else {
            this.progressAngle = 0f;
        }

        invalidate(); // Tell Android to redraw!
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float paddingLeft = getPaddingLeft();
        float paddingTop = getPaddingTop();
        float paddingRight = getPaddingRight();
        float paddingBottom = getPaddingBottom();

        float contentWidth = width - paddingLeft - paddingRight;
        float contentHeight = height - paddingTop - paddingBottom;

        float centerX = paddingLeft + (contentWidth / 2f);
        float centerY = paddingTop + (contentHeight / 2f);

        rectF.set(centerX - progressRadius, centerY - progressRadius, centerX + progressRadius, centerY + progressRadius);

        canvas.drawArc(rectF, -90, progressAngle, false, progressPaint);

        canvas.save();
        canvas.rotate(-rotationDegrees, centerX, centerY);
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
