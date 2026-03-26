package com.example.mamn01_g2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

public class ClockView extends View {
    private Paint circlePaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF rectF;
    
    private float rotationDegrees = 0;
    private int seconds = 0;
    private float progressAngle = 0;

    public ClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.LTGRAY);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(10f);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.BLUE);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(15f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(100f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        float radius = Math.min(width, height) / 3f;
        float centerX = width / 2f;
        float centerY = height / 2f;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // Rita ut cirkeln runt klockan. 
        canvas.drawCircle(centerX, centerY, radius, circlePaint);

        // Ritar ut en blå progress linje
        // Start from top 
        canvas.drawArc(rectF, -90, progressAngle, false, progressPaint);

        // Här hanteras rotationen.
        // Lägger till -90 grader för att klockan ska peka "nedåt" mot användaren
        canvas.save();
        canvas.rotate(-rotationDegrees - 90, centerX, centerY);
        
        // Rita tidstexten
        String timeText = formatTime(seconds);
        // Centrera texten vertikalt mer exakt
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
        // Map seconds to progress angle (e.g., 360 degrees = 60 seconds)
        this.progressAngle = (this.seconds % 60) * 6f;
        invalidate();
    }
}
