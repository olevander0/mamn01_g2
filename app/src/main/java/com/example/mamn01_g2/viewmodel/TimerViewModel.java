package com.example.mamn01_g2.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mamn01_g2.sensor.GestureListener;
import com.example.mamn01_g2.sensor.SensorController;
import com.example.mamn01_g2.timer.TimerController;

public class TimerViewModel extends AndroidViewModel implements GestureListener {
    private final SensorController sensorController;
    private final TimerController timerController;
    private final MutableLiveData<Long> currentTimeLiveData = new MutableLiveData<>(0L);
    private boolean isTimeLocked = false;

    public TimerViewModel(Application application) {
        super(application);
        sensorController = new SensorController(application, this);
        timerController = new TimerController(application);
    }

    public LiveData<Long> getCurrentTime() {
        return currentTimeLiveData;
    }

    public boolean isTimerRunning() {
        return timerController.isRunning();
    }


    /**
     * Called when the user manually sets the time via the TimePicker UI.
     */
    public void manuallySetTime(long timeInMillis) {
        if (!timerController.isRunning()) {
            currentTimeLiveData.setValue(timeInMillis);
            isTimeLocked = false;
        }
    }

    public void startSensors() {
        sensorController.startListening();
    }

    public void stopSensors() {
        sensorController.stopListening();
    }

    public void lockTime() {
        isTimeLocked = true;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up when the app is completely closed
        sensorController.stopListening();
        timerController.stopTimer();
    }

    @Override
    public void onTimeRotate(int minutesToChange) {
        if (isTimeLocked || isTimerRunning()) {
            return;
        }

        long millisToChange = minutesToChange * 60000L;
        Long currentTime = currentTimeLiveData.getValue();
        if (currentTime == null) currentTime = 0L;

        long newTime = currentTime + millisToChange;

        if (newTime < 0) {
            newTime = 0;
        }

        currentTimeLiveData.setValue(newTime);
    }

    @Override
    public void onPhoneFlippedDown() {
        Long currentSetTime = currentTimeLiveData.getValue();

        if (currentSetTime != null && currentSetTime > 0 && !timerController.isRunning()) {
            timerController.startTimer(currentSetTime, currentTimeLiveData::postValue, () -> {
                currentTimeLiveData.postValue(0L);
                isTimeLocked = false;
            });
        }
    }

    @Override
    public void onPhoneFlippedUp() {
        timerController.stopTimer();
        isTimeLocked = false;
    }

    @Override
    public void onPhoneLiftedFaceDown() {
        long fiveMinutesInMillis = 5 * 60 * 1000L;

        Long currentTime = currentTimeLiveData.getValue();
        if (currentTime == null) currentTime = 0L;

        long newTime = currentTime + fiveMinutesInMillis;

        if (timerController.isRunning()) {
            timerController.stopTimer();
            currentTimeLiveData.postValue(newTime);

            timerController.startTimer(newTime, currentTimeLiveData::postValue, () -> currentTimeLiveData.postValue(0L));
        } else {
            currentTimeLiveData.setValue(newTime);
        }

        timerController.playShortHaptic();
    }

    @Override
    public void onPhoneShaken() {
        if (timerController.isRunning()) {
            timerController.stopTimer();
        }

        currentTimeLiveData.setValue(0L);

        isTimeLocked = false;
    }

}