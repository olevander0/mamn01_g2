package com.example.mamn01_g2;

import android.os.CountDownTimer;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TimerController {

    public interface Listener {
        void onTimeChanged(int remainingSeconds, boolean running);
        void onStarted(int remainingSeconds);
        void onFinished();
        void onReset();
    }

    private static final long TICK_MILLIS = 1000L;

    private final List<Listener> listeners = new ArrayList<>();

    private int remainingSeconds = 0;
    private boolean running = false;
    private boolean finished = false;

    @Nullable
    private CountDownTimer countDownTimer;

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setTime(int seconds) {
        cancelInternal();
        remainingSeconds = Math.max(0, seconds);
        finished = false;
        notifyTimeChanged();
    }

    public void addTime(int deltaSeconds) {
        boolean wasRunning = running;
        cancelInternal();
        remainingSeconds = Math.max(0, remainingSeconds + deltaSeconds);
        finished = false;
        if (wasRunning && remainingSeconds > 0) {
            start();
        } else {
            notifyTimeChanged();
        }
    }

    public void start() {
        if (running || remainingSeconds <= 0) {
            return;
        }
        cancelInternal();
        finished = false;
        running = true;

        final long durationMillis = remainingSeconds * 1000L;
        countDownTimer = new CountDownTimer(durationMillis, TICK_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = (int) Math.round(millisUntilFinished / 1000.0);
                notifyTimeChanged();
            }

            @Override
            public void onFinish() {
                remainingSeconds = 0;
                running = false;
                finished = true;
                countDownTimer = null;
                notifyTimeChanged();
                for (Listener l : new ArrayList<>(listeners)) {
                    l.onFinished();
                }
            }
        };
        countDownTimer.start();

        for (Listener l : new ArrayList<>(listeners)) {
            l.onStarted(remainingSeconds);
        }
        notifyTimeChanged();
    }

    public void cancel() {
        if (!running && countDownTimer == null) {
            return;
        }
        cancelInternal();
        notifyTimeChanged();
    }

    public void reset() {
        cancelInternal();
        remainingSeconds = 0;
        finished = false;
        for (Listener l : new ArrayList<>(listeners)) {
            l.onReset();
        }
        notifyTimeChanged();
    }

    private void cancelInternal() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        running = false;
    }

    private void notifyTimeChanged() {
        for (Listener l : new ArrayList<>(listeners)) {
            l.onTimeChanged(remainingSeconds, running);
        }
    }
}
