package com.example.mamn01_g2.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Listens to the Rotation Vector sensor to detect when the user
 * physically turns the phone like a dial.
 */
public class RotationDetector implements SensorEventListener {

    // 6 degrees of physical rotation = 1 minute on a clock face (360 degrees / 60 mins)
    private static final float DEGREES_PER_MINUTE = 6.0f;
    private final GestureListener listener;
    // Arrays to hold sensor math calculations
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationValues = new float[3];
    private boolean isInitialized = false;
    private float lastAzimuth = 0f;
    private float accumulatedAngle = 0f;

    public RotationDetector(GestureListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationValues);

            float currentAzimuth = (float) Math.toDegrees(orientationValues[0]);

            if (!isInitialized) {
                lastAzimuth = currentAzimuth;
                isInitialized = true;
                return;
            }

            float deltaAzimuth = currentAzimuth - lastAzimuth;

            if (deltaAzimuth > 180) {
                deltaAzimuth -= 360;
            } else if (deltaAzimuth < -180) {
                deltaAzimuth += 360;
            }

            accumulatedAngle += deltaAzimuth;

            if (Math.abs(accumulatedAngle) >= DEGREES_PER_MINUTE) {

                // Calculate how many minutes we should jump based on the rotation amount
                int minutesToChange = (int) (accumulatedAngle / DEGREES_PER_MINUTE);

                // Fire the event back up to our ViewModel!
                if (listener != null) {
                    listener.onTimeRotate(minutesToChange);
                }

                // Keep the leftover remainder so continuous turning is smooth and accurate
                accumulatedAngle = accumulatedAngle % DEGREES_PER_MINUTE;
            }

            // Save the current angle for the next sensor update
            lastAzimuth = currentAzimuth;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We do not need to do anything when the sensor accuracy changes for this app.
    }
}