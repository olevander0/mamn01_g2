package com.example.mamn01_g2.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Manages the Android Sensor hardware, registering and unregistering
 * our custom gesture detectors to save battery.
 */
public class SensorController {

    private final SensorManager sensorManager;
    private final RotationDetector rotationDetector;
    private final MovementDetector movementDetector;
    private Sensor rotationVectorSensor;
    private Sensor accelerometerSensor;

    public SensorController(Context context, GestureListener listener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        rotationDetector = new RotationDetector(listener);
        movementDetector = new MovementDetector(listener); // We will build this next!

        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    /**
     * Call this when the app comes into the foreground (e.g., onResume)
     */
    public void startListening() {
        if (sensorManager == null) return;

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(rotationDetector, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        // Register the movement (flip/lift) detector
        if (accelerometerSensor != null) {
            sensorManager.registerListener(movementDetector, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Call this when the app goes into the background (e.g., onPause)
     */
    public void stopListening() {
        if (sensorManager == null) return;
        sensorManager.unregisterListener(rotationDetector);
        sensorManager.unregisterListener(movementDetector);
    }
}