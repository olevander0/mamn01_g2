package com.example.mamn01_g2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Sensorhantering
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor proximitySensor;
    
    // UI-komponenter
    private ClockView clockView;
    private TimePicker timePicker;
    private Button startButton;
    
    // Rotationsvariabler (för att ställa in tid genom att snurra mobilen)
    private float smoothedAzimuth = 0;
    private float lastAzimuth = 0;
    private float totalRotation = 0;
    private int seconds = 0;
    private boolean isFirstUpdate = true;

    // Timer-status och kontroll
    private boolean isTimerRunning = false;
    private CountDownTimer countDownTimer;
    private boolean isNear = false; // Håller koll på om något täcker närhetssensorn
    private float lastZ = 0;        // Senaste värdet från accelerometerns Z-axel

    // Shake detection (för att återställa timern)
    private long lastShakeTime;
    private static final float SHAKE_THRESHOLD = 12.0f;

    // Logik för att "lyfta" mobilen för att lägga till tid
    private static final int ADDED_TIME = 30*60; // 30 minuter
    private boolean timerFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Hantera systemfält (statusfält etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initiera UI
        clockView = findViewById(R.id.clockView);
        timePicker = findViewById(R.id.timePicker);
        startButton = findViewById(R.id.startButton);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Konfigurera startknappen
        if (startButton != null) {
            startButton.setOnClickListener(v -> {
                if (!isTimerRunning && seconds > 0) {
                    startCountdown();
                } else if (seconds == 0) {
                    Toast.makeText(this, "Ställ in en tid först!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Koppla TimePicker (för manuell tidsinställning via touch)
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

        // Initiera sensorer
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrera sensorlyssnare när appen blir synlig
        if (sensorManager != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Avregistrera sensorer för att spara batteri när appen pausas
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isTimerRunning = false;
        totalRotation = seconds * 10f; // Spara nuvarande tid som rotationsbasis
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
            // isNear blir sant om något är nära sensorn (oftast < 5cm)
            boolean currentIsNear = event.values[0] < proximitySensor.getMaximumRange();

            // Om vi precis lyfte mobilen från bordet (gick från nära till inte nära)
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

        // Low-pass filter för att jämna ut darrig sensor-data
        float alpha = 0.2f; 
        float diff = rawAzimuth - smoothedAzimuth;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        smoothedAzimuth += alpha * diff;

        // Normalisera till [-180, 180]
        if (smoothedAzimuth > 180) smoothedAzimuth -= 360;
        if (smoothedAzimuth < -180) smoothedAzimuth += 360;

        // Räkna ut skillnaden sedan förra uppdateringen
        float delta = smoothedAzimuth - lastAzimuth;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;

        // Om timern inte körs, ändra tiden baserat på rotation
        if (!isTimerRunning) {
            totalRotation += delta;
            if (totalRotation < 0) totalRotation = 0;

            int newSeconds = (int) (totalRotation / 10);
            if (newSeconds != seconds) {
                seconds = newSeconds;
                updateDisplayedTime(seconds);
            }
        }

        lastAzimuth = smoothedAzimuth;

        // Uppdatera klockans visuella rotation (om den används)
        if (clockView != null) {
            clockView.setRotationDegrees(smoothedAzimuth);
        }
    }

    private void handleAccelerometer(float[] values) {
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // Shake detection: Beräkna total acceleration minus gravitation
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

    // Starta timern om mobilen ligger nedåt och något täcker skärmen
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

    // Nollställ allt
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

    // Lägg till 30 minuter om man lyfter telefonen under körning
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

    // Uppdatera tid när användaren drar med fingret (TimePicker)
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

    // Hjälpmetod för att uppdatera både klockan och touch-väljaren
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
