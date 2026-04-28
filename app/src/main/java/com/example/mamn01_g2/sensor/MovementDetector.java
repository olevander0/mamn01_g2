package com.example.mamn01_g2.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Listens to the Accelerometer to detect when the user flips the phone
 * face down, face up, or lifts it while face down.
 */
public class MovementDetector implements SensorEventListener {

    private static final float FACE_DOWN_THRESHOLD = -7.5f;
    private static final float FACE_UP_THRESHOLD = 7.5f;
    private static final float LIFT_THRESHOLD = -11.5f;
    private static final long LIFT_COOLDOWN_MS = 1500;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private final GestureListener listener;
    private boolean isFaceDown = false;
    private long lastLiftTime = 0;
    private long lastShakeTime = 0;

    public MovementDetector(GestureListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float xValue = event.values[0];
            float yValue = event.values[1];
            float zValue = event.values[2];

            // SHAKE DETECTION
            float acceleration = (float) Math.sqrt(xValue * xValue + yValue * yValue + zValue * zValue) - SensorManager.GRAVITY_EARTH;

            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                // Cooldown to prevent retriggering
                if (currentTime - lastShakeTime > 500) {
                    lastShakeTime = currentTime;
                    if (listener != null) {
                        listener.onPhoneShaken();
                    }
                }
            }

            // FLIP DETECTION
            if (zValue < FACE_DOWN_THRESHOLD && !isFaceDown) {
                isFaceDown = true;
                if (listener != null) listener.onPhoneFlippedDown();
            } else if (zValue > FACE_UP_THRESHOLD && isFaceDown) {
                isFaceDown = false;
                if (listener != null) listener.onPhoneFlippedUp();
            }

            // LIFT DETECTION
            if (isFaceDown && zValue < LIFT_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLiftTime > LIFT_COOLDOWN_MS) {
                    lastLiftTime = currentTime;
                    if (listener != null) listener.onPhoneLiftedFaceDown();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}