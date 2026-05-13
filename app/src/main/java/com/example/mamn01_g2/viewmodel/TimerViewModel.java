package com.example.mamn01_g2.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mamn01_g2.sensor.GestureListener;
import com.example.mamn01_g2.sensor.SensorController;
import com.example.mamn01_g2.timer.TimerController;

public class TimerViewModel extends AndroidViewModel implements GestureListener {
    private final SensorController sensorController;
    private final TimerController timerController;
    private final MutableLiveData<Long> currentTimeLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isFaceUp = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isTimerRunningLiveData = new MutableLiveData<>(false);
    private final MediatorLiveData<Boolean> lockInState = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> isLockedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isRingingLiveData = new MutableLiveData<>(false);
    private boolean isTimeLocked = false;

    public TimerViewModel(Application application) {
        super(application);
        sensorController = new SensorController(application, this);
        timerController = new TimerController(application);
        lockInState.addSource(isFaceUp, value -> updateLockInState());
        lockInState.addSource(isTimerRunningLiveData, value -> updateLockInState());
    }

    public LiveData<Boolean> getIsRinging() {
        return isRingingLiveData;
    }

    public void snoozeTimer() {
        if (Boolean.TRUE.equals(isRingingLiveData.getValue())) {
            timerController.stopTimer();
            isRingingLiveData.setValue(false);

            long snoozeTimeInMillis = 5 * 60 * 1000L;
            currentTimeLiveData.setValue(snoozeTimeInMillis);

            timerController.startTimer(snoozeTimeInMillis, currentTimeLiveData::postValue, () -> {
                currentTimeLiveData.postValue(0L);
                isRingingLiveData.postValue(true); // Ring again when done!
            });

            timerController.playTickFeedback();
        }
    }

    public void stopAlarm() {
        if (Boolean.TRUE.equals(isRingingLiveData.getValue())) {
            timerController.stopTimer();
            isRingingLiveData.setValue(false);
            currentTimeLiveData.setValue(0L);
            isTimeLocked = false;
        }
    }

    public void toggleTimeLock() {
        Long currentSetTime = currentTimeLiveData.getValue();
        if (!timerController.isRunning() && currentSetTime != null && currentSetTime > 0) {

            isTimeLocked = !isTimeLocked;
            isLockedLiveData.setValue(isTimeLocked);
            timerController.playTickFeedback();
        }
    }

    public LiveData<Boolean> getIsLocked() {
        return isLockedLiveData;
    }

    public LiveData<Boolean> getIsFaceUp() {
        return isFaceUp;
    }

    public LiveData<Boolean> getIsTimerRunning() {
        return isTimerRunningLiveData;
    }

    public LiveData<Boolean> getLockInState() {
        return lockInState;
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
            timerController.playTickFeedback();
            isLockedLiveData.setValue(false);
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

        if (newTime != currentTime) {
            currentTimeLiveData.setValue(newTime);
            timerController.playTickFeedback();
        }
    }

    @Override
    public void onPhoneFlippedDown() {
        Long currentSetTime = currentTimeLiveData.getValue();
        if (currentSetTime != null && currentSetTime > 0 && !timerController.isRunning()) {
            isTimerRunningLiveData.setValue(true);
            timerController.startTimer(currentSetTime, currentTimeLiveData::postValue, () -> {
                currentTimeLiveData.postValue(0L);
                isTimeLocked = false;
                isTimerRunningLiveData.postValue(false);
                isRingingLiveData.postValue(true); // ALARM TRIGGERED! 🔔
            });
        }
    }

    @Override
    public void onPhoneFlippedUp() {
        isFaceUp.setValue(true);
    }

    @Override
    public void onPhoneLiftedFaceDown() {
        if (Boolean.TRUE.equals(isRingingLiveData.getValue())) {
            snoozeTimer();
        }
    }

    @Override
    public void onPhoneShaken() {
        if (timerController.isRunning()) {
            timerController.stopTimer();
        }
        currentTimeLiveData.setValue(0L);
        isTimeLocked = false;
        isLockedLiveData.setValue(false);
    }

    private void updateLockInState() {
        Boolean faceUp = isFaceUp.getValue();
        Boolean running = isTimerRunningLiveData.getValue();
        if (faceUp == null) faceUp = false;
        if (running == null) running = false;
        lockInState.setValue(faceUp && running);
    }

}