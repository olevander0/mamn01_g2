package com.example.mamn01_g2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int ADDED_TIME_SECONDS = 30 * 60;
    private static final float DEGREES_PER_SECOND = 10f;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor proximitySensor;

    private ClockView clockView;
    private TimePicker timePicker;

    private final TimerController timerController = new TimerController();

    private float smoothedAzimuth = 0;
    private float lastAzimuth = 0;
    private float totalRotation = 0;
    private boolean isFirstUpdate = true;

    private boolean isNear = false;
    private float lastZ = 0;
    private long lastShakeTime;

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

        setupTimerBindings();
        setupTimePicker();
        setupSensors();
    }

    private void setupTimerBindings() {
        timerController.addListener(new TimerController.Listener() {
            @Override
            public void onTimeChanged(int remainingSeconds, boolean running) {
                if (clockView != null) {
                    clockView.setTime(remainingSeconds);
                }
                if (timePicker != null) {
                    timePicker.setSelectedSeconds(remainingSeconds);
                }
            }

            @Override
            public void onStarted(int remainingSeconds) {
                Toast.makeText(MainActivity.this, "Timer startad!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinished() {
                totalRotation = 0;
                Toast.makeText(MainActivity.this, "Tiden är ute!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReset() {
                totalRotation = 0;
                Toast.makeText(MainActivity.this, "Timer återställd", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTimePicker() {
        if (timePicker == null) {
            return;
        }
        timePicker.setSelectedSeconds(timerController.getRemainingSeconds());
        timePicker.setOnTimeSelectedListener(new TimePicker.OnTimeSelectedListener() {
            @Override
            public void onTimePreviewChanged(int selectedSeconds) {
                applyManualTime(selectedSeconds);
            }

            @Override
            public void onTimeSelected(int selectedSeconds) {
                applyManualTime(selectedSeconds);
            }

            @Override
            public void onTimeSelectionCancelled(int restoredSeconds) {
                applyManualTime(restoredSeconds);
            }
        });
    }

    private void setupSensors() {
        if (sensorManager == null) {
            return;
        }
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
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
        timerController.cancel();
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
            if (isNear && !currentIsNear && lastZ < -5.0f) {
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

        if (!timerController.isRunning()) {
            totalRotation += delta;
            if (totalRotation < 0) totalRotation = 0;

            int newSeconds = (int) (totalRotation / DEGREES_PER_SECOND);
            if (newSeconds != timerController.getRemainingSeconds()) {
                timerController.setTime(newSeconds);
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
                timerController.reset();
                lastShakeTime = currentTime;
            }
            return;
        }

        checkAndStartTimer(z);
    }

    private void checkAndStartTimer(float z) {
        if (isNear && z < -8.0f
                && !timerController.isRunning()
                && timerController.getRemainingSeconds() > 0) {
            timerController.start();
        }
    }

    private void handleLiftGesture() {
        if (timerController.isRunning()) {
            timerController.addTime(ADDED_TIME_SECONDS);
        } else if (timerController.isFinished()) {
            timerController.setTime(ADDED_TIME_SECONDS);
            timerController.start();
        } else {
            return;
        }
        totalRotation = timerController.getRemainingSeconds() * DEGREES_PER_SECOND;
        Toast.makeText(this, "Tiden ökat med " + ADDED_TIME_SECONDS + " sekunder",
                Toast.LENGTH_SHORT).show();
    }

    private void applyManualTime(int selectedSeconds) {
        timerController.setTime(selectedSeconds);
        totalRotation = timerController.getRemainingSeconds() * DEGREES_PER_SECOND;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
