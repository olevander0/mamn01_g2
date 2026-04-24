package com.example.mamn01_g2.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Listens to the Accelerometer to detect when the user flips the phone
 * face down, face up, or lifts it while face down.
 */
public class MovementDetector implements SensorEventListener {

    // Thresholds for Earth's gravity (approx 9.8 m/s^2)
    private static final float FACE_DOWN_THRESHOLD = -7.5f;
    private static final float FACE_UP_THRESHOLD = 7.5f;
    // Threshold for sudden upward acceleration while face down
    private static final float LIFT_THRESHOLD = -11.5f;
    private static final long LIFT_COOLDOWN_MS = 1500; // 1.5 seconds
    private final GestureListener listener;
    // State tracker so we don't spam the listener
    private boolean isFaceDown = false;
    // Cooldown timer to prevent a single lift from triggering multiple times
    private long lastLiftTime = 0;

    public MovementDetector(GestureListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Ensure we are only reading the accelerometer
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float zValue = event.values[2]; // Index 2 is the Z-axis

            // 1. Check for Flip (Face Up vs Face Down)
            if (zValue < FACE_DOWN_THRESHOLD && !isFaceDown) {
                isFaceDown = true;
                if (listener != null) {
                    listener.onPhoneFlippedDown(); // Trigger timer start!
                }
            } else if (zValue > FACE_UP_THRESHOLD && isFaceDown) {
                isFaceDown = false;
                if (listener != null) {
                    listener.onPhoneFlippedUp(); // Trigger timer pause/stop!
                }
            }

            // 2. Check for Lift (Snooze / Add 5 minutes)
            // If it is currently face down, and Z becomes significantly more negative,
            // the user is lifting the phone off the table.
            if (isFaceDown && zValue < LIFT_THRESHOLD) {
                long currentTime = System.currentTimeMillis();

                // Only trigger if the cooldown has passed
                if (currentTime - lastLiftTime > LIFT_COOLDOWN_MS) {
                    lastLiftTime = currentTime;

                    if (listener != null) {
                        listener.onPhoneLiftedFaceDown(); // Trigger add 5 minutes!
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this specific implementation
    }
}