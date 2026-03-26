package com.example.mamn01_g2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private ClockView clockView;

    private float lastAzimuth = 0;
    private float totalRotation = 0;
    private int seconds = 0;
    private boolean isFirstUpdate = true;

    // Shake detection variables
    private long lastShakeTime;
    private static final float SHAKE_THRESHOLD = 12.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clockView = findViewById(R.id.clockView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            handleRotation(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleShake(event.values);
        }
    }

    private void handleRotation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        
        // Azimuth is orientation[0], range [-PI, PI]
        float azimuthDegrees = (float) Math.toDegrees(orientation[0]);
        
        if (isFirstUpdate) {
            lastAzimuth = azimuthDegrees;
            isFirstUpdate = false;
            return;
        }

        float delta = azimuthDegrees - lastAzimuth;
        
        // Handle wrap-around (e.g. from 179 to -179)
        if (delta > 180) delta -= 360;
        else if (delta < -180) delta += 360;

        totalRotation += delta;
        lastAzimuth = azimuthDegrees;

        // One full rotation (360 degrees) equals 60 seconds
        int newSeconds = (int) (totalRotation / 6); 
        if (newSeconds != seconds) {
            seconds = Math.max(0, newSeconds);
            clockView.setTime(seconds);
        }

        // Förösk till att hålla klockan stadig när mobilen roterar men funkar inte riktigt än.
        clockView.setRotationDegrees(azimuthDegrees);
    }

    private void handleShake(float[] values) {
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
        
        if (acceleration > SHAKE_THRESHOLD) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShakeTime > 500) { // debounce shake
                resetTimer();
                lastShakeTime = currentTime;
            }
        }
    }

    private void resetTimer() {
        seconds = 0;
        totalRotation = 0;
        clockView.setTime(0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
