package com.example.mamn01_g2.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mamn01_g2.sensor.GestureListener;
import com.example.mamn01_g2.sensor.SensorController;
import com.example.mamn01_g2.timer.TimerController;
import com.example.mamn01_g2.timer.TimerState;

public class TimerViewModel extends AndroidViewModel implements GestureListener {
    private final SensorController sensorController;
    private final TimerController timerController;
    private final MutableLiveData<Long> currentTimeLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<TimerState> timerStateLiveData = new MutableLiveData<>(TimerState.IDLE);
    private boolean isPhoneFaceDown = false;

    public TimerViewModel(Application application) {
        super(application);
        sensorController = new SensorController(application, this);
        timerController = new TimerController(application);
    }

    public LiveData<TimerState> getTimerState() {
        return timerStateLiveData;
    }

    public LiveData<Long> getCurrentTime() {
        return currentTimeLiveData;
    }

    public boolean isTimerRunning() {
        return timerController.isRunning();
    }

    // --- BUTTON & UI ACTIONS --- 🔘

    public void toggleTimeLock() {
        TimerState currentState = timerStateLiveData.getValue();
        Long currentSetTime = currentTimeLiveData.getValue();

        if (currentSetTime != null && currentSetTime > 0) {
            if (currentState == TimerState.IDLE) {
                timerStateLiveData.setValue(TimerState.LOCKED);
                timerController.playTickFeedback();
            } else if (currentState == TimerState.LOCKED) {
                timerStateLiveData.setValue(TimerState.IDLE);
                timerController.playTickFeedback();
            }
        }
    }

    public void manuallySetTime(long timeInMillis) {
        TimerState currentState = timerStateLiveData.getValue();
        // Only allow manual set if not currently running alarm or focus
        if (currentState == TimerState.IDLE || currentState == TimerState.LOCKED) {
            currentTimeLiveData.setValue(timeInMillis);
            timerStateLiveData.setValue(TimerState.IDLE); // Auto-unlock on manual change! 🔓
            timerController.playTickFeedback();
        }
    }

    public void snoozeTimer() {
        TimerState currentState = timerStateLiveData.getValue();

        if (currentState == TimerState.RINGING || currentState == TimerState.WARNING) {
            timerController.stopTimer();

            long snoozeTimeInMillis = 5 * 60 * 1000L;

            Long currentVal = currentTimeLiveData.getValue();
            long currentTimeInMillis = (currentVal != null) ? currentVal : 0L;

            if (currentTimeInMillis >= 0L) {
                long newTotalTimeInMillis = currentTimeInMillis + snoozeTimeInMillis;

                currentTimeLiveData.setValue(newTotalTimeInMillis);

                if (isPhoneFaceDown) {
                    timerStateLiveData.setValue(TimerState.FOCUSING);
                } else {
                    timerStateLiveData.setValue(TimerState.WARNING);
                }

                timerController.startTimer(newTotalTimeInMillis, currentTimeLiveData::postValue, () -> {
                    currentTimeLiveData.postValue(0L);
                    timerStateLiveData.postValue(TimerState.RINGING);
                });

                timerController.playTickFeedback();
            }
        }
    }

    public void stopAlarm() {
        TimerState currentState = timerStateLiveData.getValue();
        if (currentState == TimerState.RINGING || currentState == TimerState.WARNING || currentState == TimerState.FOCUSING) {
            timerController.stopTimer();
            currentTimeLiveData.setValue(0L);
            timerStateLiveData.setValue(TimerState.IDLE);
        }
    }

    // --- SENSOR LIFECYCLE --- ♻️

    public void startSensors() {
        sensorController.startListening();
    }

    public void stopSensors() {
        sensorController.stopListening();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Prevent Memory Leaks during Garbage Collection! 🗑️
        sensorController.stopListening();
        timerController.stopTimer();
    }

    // --- GESTURE ROUTING --- 🪨📱

    @Override
    public void onTimeRotate(int minutesToChange) {
        if (timerStateLiveData.getValue() != TimerState.IDLE) {
            return;
        }

        long millisToChange = minutesToChange * 60000L;
        Long currentTime = currentTimeLiveData.getValue();
        if (currentTime == null) currentTime = 0L;

        long newTime = currentTime + millisToChange;
        if (newTime < 0) {
            newTime = 0;
        }

        if (newTime != currentTime) {
            currentTimeLiveData.setValue(newTime);
            timerController.playTickFeedback();
        }
    }

    @Override
    public void onPhoneFlippedDown() {
        TimerState currentState = timerStateLiveData.getValue();
        Long currentSetTime = currentTimeLiveData.getValue();

        if (currentState == TimerState.WARNING) {
            timerStateLiveData.setValue(TimerState.FOCUSING);
            return;
        }

        if (currentSetTime != null && currentSetTime > 0 && (currentState == TimerState.IDLE || currentState == TimerState.LOCKED)) {

            timerStateLiveData.setValue(TimerState.FOCUSING); // Enter Focus Mode!

            timerController.startTimer(currentSetTime, currentTimeLiveData::postValue, () -> {
                currentTimeLiveData.postValue(0L);
                timerStateLiveData.postValue(TimerState.RINGING); // Time up! 🔔
            });
        }
    }

    @Override
    public void onPhoneFlippedUp() {
        TimerState currentState = timerStateLiveData.getValue();

        if (currentState == TimerState.FOCUSING) {
            timerStateLiveData.setValue(TimerState.WARNING);
        }
    }

    @Override
    public void onPhoneLiftedFaceDown() {
        TimerState currentState = timerStateLiveData.getValue();

        if (currentState == TimerState.RINGING) {
            snoozeTimer(); // Lift while ringing = SNOOZE
        } else if (currentState == TimerState.FOCUSING) {
            timerStateLiveData.setValue(TimerState.WARNING); // Lift while focusing = WARNING
        }
    }

    @Override
    public void onPhoneShaken() {
        if (timerController.isRunning()) {
            timerController.stopTimer();
        }
        currentTimeLiveData.setValue(0L);
        timerStateLiveData.setValue(TimerState.IDLE);
    }
}