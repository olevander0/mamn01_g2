package com.example.mamn01_g2.timer;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class TimerController {

    private final Vibrator vibrator;
    private final Ringtone alarmSound;
    private final AudioManager audioManager;
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private long timeRemainingInMillis = 0L;

    public TimerController(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        alarmSound = RingtoneManager.getRingtone(context, alarmUri);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
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


        playShortHaptic();

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

                // Tell the ViewModel the timer finished
                if (onFinish != null) {
                    onFinish.run();
                }

                // Start making noise and vibrating!
                startAlarm();
            }
        }.start();
    }

    public void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        stopAlarm(); // Just in case it was currently ringing when the user flipped the phone up
    }

    public boolean isRunning() {
        return isRunning;
    }

    // --- Feedback Methods (Sound & Vibration) ---

    public void playShortHaptic() {
        if (vibrator != null && vibrator.hasVibrator()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 50));
            } else {
                vibrator.vibrate(15);
            }
        }
    }

    private void startAlarm() {
        // Play the sound
        if (alarmSound != null && !alarmSound.isPlaying()) {
            alarmSound.play();
        }

        // Loop a long vibration pattern (Wait 0ms, Vibrate 500ms, Wait 500ms)
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // '0' means repeat starting at index 0
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarm() {
        if (alarmSound != null && alarmSound.isPlaying()) {
            alarmSound.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public void playTickFeedback() {
        playShortHaptic();

        if (audioManager != null) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f);
        }
    }

    // A simple interface we use to pass the ticking time back to the ViewModel
    public interface OnTickListener {
        void onTick(long millisUntilFinished);
    }
}