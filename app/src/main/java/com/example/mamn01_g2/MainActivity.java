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
    private TimePicker timePicker;
    
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

    private static final int ADDED_TIME = 30*60;
    private boolean timerFinished = false;

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
        timePicker = findViewById(R.id.timePicker);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (timePicker != null) {
            timePicker.setSelectedSeconds(seconds);
            timePicker.setOnTimeSelectedListener(new TimePicker.OnTimeSelectedListener() {
                @Override
                public void onTimePreviewChanged(int selectedSeconds) {
                    setManualTime(selectedSeconds);
                }

                @Override
                public void onTimeSelected(int selectedSeconds) {
                    setManualTime(selectedSeconds);
                }

                @Override
                public void onTimeSelectionCancelled(int restoredSeconds) {
                    setManualTime(restoredSeconds);
                }
            });
        }

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
            countDownTimer = null;
        }
        isTimerRunning = false;
        totalRotation = seconds * 10f;
        if (timePicker != null) {
            timePicker.setCountDownTimer(null);
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
            boolean currentIsNear = event.values[0] < proximitySensor.getMaximumRange();

            //checks if it was near but not near anymore while still upside down
            if(isNear && !currentIsNear && lastZ < -5.0f){
                handleLiftGesture();

            }
            isNear = currentIsNear;
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
                updateDisplayedTime(seconds);
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
        timerFinished = false;
        updateDisplayedTime(seconds);
        Toast.makeText(this, "Timer startad!", Toast.LENGTH_SHORT).show();

        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                seconds = (int) (millisUntilFinished / 1000);
                updateDisplayedTime(seconds);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timerFinished = true;
                seconds = 0;
                totalRotation = 0;
                countDownTimer = null;
                updateDisplayedTime(0);
                if (timePicker != null) {
                    timePicker.setCountDownTimer(null);
                }
                Toast.makeText(MainActivity.this, "Tiden är ute!", Toast.LENGTH_LONG).show();
            }
        }.start();

        if (timePicker != null) {
            timePicker.setCountDownTimer(countDownTimer);
        }
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isTimerRunning = false;
        timerFinished = false;
        seconds = 0;
        totalRotation = 0;
        updateDisplayedTime(0);
        if (timePicker != null) {
            timePicker.setCountDownTimer(null);
        }
        Toast.makeText(this, "Timer återställd", Toast.LENGTH_SHORT).show();
    }

    private void handleLiftGesture() {
        if (isTimerRunning) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            seconds += ADDED_TIME;
        } else if (timerFinished) {
            seconds = ADDED_TIME;
        } else {
            return;
        }

        totalRotation = seconds * 10f;
        updateDisplayedTime(seconds);
        startCountdown();
        Toast.makeText(this, "Tiden ökat med " + ADDED_TIME + " sekunder", Toast.LENGTH_SHORT).show();
    }

    private void setManualTime(int selectedSeconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        isTimerRunning = false;
        timerFinished = false;
        seconds = Math.max(0, selectedSeconds);
        totalRotation = seconds * 10f;
        updateDisplayedTime(seconds);

        if (timePicker != null) {
            timePicker.setCountDownTimer(null);
        }
    }

    private void updateDisplayedTime(int secondsToDisplay) {
        if (clockView != null) {
            clockView.setTime(secondsToDisplay);
        }
        if (timePicker != null) {
            timePicker.setSelectedSeconds(secondsToDisplay);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
