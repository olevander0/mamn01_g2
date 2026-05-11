package com.example.mamn01_g2.feedback;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.example.mamn01_g2.R;

/**
 * Modul som hanterar all feedback (ljud och vibration).
 * Följer Separation of Concerns genom att isolera hårdvaruinteraktioner.
 */
public class TimerFeedbackManager {
    private final Context context;
    private final Vibrator vibrator;
    private final AudioManager audioManager;
    private Ringtone alarmSound;
    private MediaPlayer startMediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public TimerFeedbackManager(Context context) {
        this.context = context.getApplicationContext();
        
        // Initiera Vibrator för olika Android-versioner
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        // Standardljud för alarm
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        alarmSound = RingtoneManager.getRingtone(context, alarmUri);
    }

    /**
     * Feedback när klockan startas: 
     * - Vibrationstikande i 3 sekunder.
     * - Ljudfil i 4 sekunder.
     */
    public void playStartFeedback() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 10, 490, 10, 490, 10, 490, 10, 490, 10, 490, 10, 490};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }

        try {
            startMediaPlayer = MediaPlayer.create(context, R.raw.timer_start);
            if (startMediaPlayer != null) {
                startMediaPlayer.start();
                
                handler.postDelayed(() -> {
                    if (startMediaPlayer != null) {
                        stopStartAudio();
                    }
                }, 4000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Spelar upp ett mekaniskt klick för rotation (inställning av tid).
     * Använder EFFECT_CLICK för en tydligare fysisk känsla än EFFECT_TICK.
     */
    public void playRotationTick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // EFFECT_CLICK ger en mer distinkt mekanisk känsla
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(20);
            }
        }

        // Diskret klickljud för att förstärka känslan
        if (audioManager != null) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f);
        }
    }

    /**
     * Standard-tick (används av TimerController).
     */
    public void playTick() {
        playRotationTick();
    }

    public void startAlarm() {
        if (alarmSound != null && !alarmSound.isPlaying()) {
            alarmSound.play();
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    public void stopAll() {
        if (alarmSound != null && alarmSound.isPlaying()) alarmSound.stop();
        if (vibrator != null) vibrator.cancel();
        stopStartAudio();
        handler.removeCallbacksAndMessages(null);
    }

    private void stopStartAudio() {
        if (startMediaPlayer != null) {
            try {
                startMediaPlayer.stop();
                startMediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            startMediaPlayer = null;
        }
    }
}
