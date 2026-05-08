package com.example.mamn01_g2.timer;

import android.content.Context;
import android.os.CountDownTimer;
import com.example.mamn01_g2.feedback.TimerFeedbackManager;

/**
 * Hanterar logiken för nedräkningen.
 * Delegerar all feedback (ljud/vibration) till TimerFeedbackManager.
 */
public class TimerController {

    private final TimerFeedbackManager feedbackManager;
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private long timeRemainingInMillis = 0L;

    public TimerController(Context context) {
        this.feedbackManager = new TimerFeedbackManager(context);
    }

    /**
     * Starts the countdown timer.
     *
     * @param durationInMillis How long the timer should run.
     * @param onTick           Callback triggered every second with the remaining time.
     * @param onFinish         Callback triggered when the timer reaches zero.
     */
    public void startTimer(long durationInMillis, OnTickListener onTick, Runnable onFinish) {
        if (isRunning) {
            stopTimer();
        }

        isRunning = true;

        // Använd den nya feedback-modulen för start-effekter
        feedbackManager.playStartFeedback();

        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingInMillis = millisUntilFinished;
                if (onTick != null) {
                    onTick.onTick(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timeRemainingInMillis = 0;

                if (onFinish != null) {
                    onFinish.run();
                }

                // Aktivera alarm via feedback-modulen
                feedbackManager.startAlarm();
            }
        }.start();
    }

    public void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        // Stoppar all feedback (både start-ljud och alarm)
        feedbackManager.stopAll();
    }

    public boolean isRunning() {
        return isRunning;
    }

    // --- Delegerade Feedback-metoder ---

    public void playShortHaptic() {
        feedbackManager.playTick();
    }

    public void playTickFeedback() {
        feedbackManager.playTick();
    }

    public interface OnTickListener {
        void onTick(long millisUntilFinished);
    }
}
