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
 
    private final Paint circlePaint;
    private final Paint progressPaint;
    private final Paint textPaint;
    private final Paint tickPaint;
    private final RectF  rectF;
 
    private float rotationDegrees = 0f;   
    private int   seconds         = 0;
    private float progressAngle   = 0f;
 
    public ClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
 
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
 
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.DKGRAY);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(4f);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
 
        rectF = new RectF();
    }
 
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
 
        float w = getWidth();
        float h = getHeight();
        float radius  = Math.min(w, h) / 3f;
        float cx = w / 2f;
        float cy = h / 2f;
 
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius);
 
        // -------------------------------------------------------
        // Rotation är inaktiverad enligt önskemål.
        // Vi behåller logiken för ringen och framstegsbågen statisk.
        // -------------------------------------------------------
 
        // Ring
        canvas.drawCircle(cx, cy, radius, circlePaint);
 
        // Progress-båge (börjar kl. 12, dvs. -90°)
        // Denna fylls på när man snurrar telefonen (eftersom 'seconds' ökar)
        canvas.drawArc(rectF, -90, progressAngle, false, progressPaint);
 
        // Fast bock-markör i toppen
        float tickInner = radius - 18f;
        float tickOuter = radius + 5f;
        canvas.drawLine(cx, cy - tickOuter, cx, cy - tickInner, tickPaint);
 
        // Texten ritas i mitten
        String timeText = formatTime(seconds);
        float textOffset = (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(timeText, cx, cy - textOffset, textPaint);
    }
 
    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int secs    = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }
 
    /** Uppdaterar rotationen (används ej visuellt just nu). */
    public void setRotationDegrees(float degrees) {
        this.rotationDegrees = degrees;
        invalidate();
    }
 
    /** Sätter visad tid och räknar om progress-bågen. */
    public void setTime(int seconds) {
        this.seconds      = Math.max(0, seconds);
        this.progressAngle = (this.seconds % 60) * 6f;   // 360° / 60 s = 6° per s
        invalidate();
    }
}
