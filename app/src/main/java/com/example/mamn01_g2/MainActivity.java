package com.example.mamn01_g2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor proximitySensor;
    private ClockView clockView;
    
    private float smoothedAzimuth = 0;
    private float lastAzimuth = 0;
    private float totalRotation = 0;
    private int seconds = 0;
    private boolean isFirstUpdate = true;

    private boolean isTimerRunning = false;
    private CountDownTimer countDownTimer;
    private boolean isNear = false;
    private float lastZ = 0; 

    private long lastShakeTime;
    private static final float SHAKE_THRESHOLD = 12.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        clockView = findViewById(R.id.clockView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            handleRotation(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lastZ = event.values[2];
            handleAccelerometer(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            isNear = event.values[0] < proximitySensor.getMaximumRange();
            checkAndStartTimer(lastZ);
        }
    }

    private void handleRotation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        float rawAzimuth = (float) Math.toDegrees(orientation[0]);

        if (isFirstUpdate) {
            smoothedAzimuth = rawAzimuth;
            lastAzimuth = rawAzimuth;
            isFirstUpdate = false;
        }

        float alpha = 0.2f; 
        float diff = rawAzimuth - smoothedAzimuth;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        smoothedAzimuth += alpha * diff;

        if (smoothedAzimuth > 180) smoothedAzimuth -= 360;
        if (smoothedAzimuth < -180) smoothedAzimuth += 360;

        float delta = smoothedAzimuth - lastAzimuth;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;

        if (!isTimerRunning) {
            // Nu lägger vi till delta direkt (utan Math.abs)
            // Medurs rotation ger positivt delta, moturs ger negativt
            totalRotation += delta;
            
            // Förhindra att totalRotation blir negativ så man inte behöver 
            // skruva tillbaka massor för att öka tiden igen.
            if (totalRotation < 0) totalRotation = 0;

            int newSeconds = (int) (totalRotation / 10);
            if (newSeconds != seconds) {
                seconds = newSeconds;
                if (clockView != null) clockView.setTime(seconds);
            }
        }

        lastAzimuth = smoothedAzimuth;

        if (clockView != null) {
            clockView.setRotationDegrees(smoothedAzimuth);
        }
    }

    private void handleAccelerometer(float[] values) {
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
        if (acceleration > SHAKE_THRESHOLD) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShakeTime > 500) {
                resetTimer();
                lastShakeTime = currentTime;
            }
            return;
        }

        checkAndStartTimer(z);
    }

    private void checkAndStartTimer(float z) {
        if (isNear && z < -8.0f && !isTimerRunning && seconds > 0) {
            startCountdown();
        }
    }

    private void startCountdown() {
        isTimerRunning = true;
        Toast.makeText(this, "Timer startad!", Toast.LENGTH_SHORT).show();

        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                seconds = (int) (millisUntilFinished / 1000);
                if (clockView != null) {
                    clockView.setTime(seconds);
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                seconds = 0;
                totalRotation = 0; 
                if (clockView != null) {
                    clockView.setTime(0);
                }
                Toast.makeText(MainActivity.this, "Tiden är ute!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        seconds = 0;
        totalRotation = 0;
        if (clockView != null) {
            clockView.setTime(0);
        }
        Toast.makeText(this, "Timer återställd", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
