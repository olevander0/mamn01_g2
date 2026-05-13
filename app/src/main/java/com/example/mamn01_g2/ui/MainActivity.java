package com.example.mamn01_g2.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.mamn01_g2.R;
import com.example.mamn01_g2.timer.TimerState;
import com.example.mamn01_g2.viewmodel.TimerViewModel;

public class MainActivity extends AppCompatActivity {
    private TimePicker timePicker;
    private TimerViewModel viewModel;
    private ClockView clockView;

    private Button snoozeButton;
    private Button btnLock;
    private Button btnUnlock;
    private TextView instructionsText;
    private AppCompatImageButton infoButton;
    private ConstraintLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        snoozeButton = findViewById(R.id.btn_snooze);
        btnLock = findViewById(R.id.btn_lock);
        btnUnlock = findViewById(R.id.btn_unlock);
        instructionsText = findViewById(R.id.tv_instruction);
        clockView = findViewById(R.id.clock_view);
        timePicker = findViewById(R.id.time_picker);
        infoButton = findViewById(R.id.btn_info);
        rootLayout = findViewById(R.id.main);

        viewModel = new ViewModelProvider(this).get(TimerViewModel.class);

        // One observer for State Machine
        viewModel.getTimerState().observe(this, this::updateUIForState);

        ViewCompat.setTooltipText(infoButton, getString(R.string.info_button_content_description));
        infoButton.setOnClickListener(v -> showInfoDialog());

        timePicker.setOnTimeSelectedListener(selectedSeconds -> viewModel.manuallySetTime(selectedSeconds * 1000L));

        viewModel.getCurrentTime().observe(this, timeInMillis -> {
            if (!viewModel.isTimerRunning()) {
                clockView.setInitialTime(timeInMillis);
            } else {
                clockView.updateTime(timeInMillis);
            }
            timePicker.setSelectedSeconds((int) (timeInMillis / 1000));
        });
    }


    private void updateUIForState(TimerState state) {
        cancelPulseAnimation(clockView); // Stop shaking by default
        rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        switch (state) {
            case IDLE:
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText("Spin phone to set time\nShake to reset");
                instructionsText.setTextColor(Color.GRAY);
                timePicker.setVisibility(View.VISIBLE);
                infoButton.setVisibility(View.VISIBLE);

                snoozeButton.setVisibility(View.GONE);

                btnUnlock.setVisibility(View.GONE); // Hide dark button

                btnLock.setVisibility(View.VISIBLE); // Show primary button
                btnLock.setText(R.string.lock_time);
                btnLock.setOnClickListener(v -> viewModel.toggleTimeLock());
                break;

            case LOCKED:
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText("LOCKED! 🔒\nFlip phone face down to start");
                instructionsText.setTextColor(Color.parseColor("#4CAF50")); // Green!
                timePicker.setVisibility(View.VISIBLE);
                infoButton.setVisibility(View.VISIBLE);

                snoozeButton.setVisibility(View.GONE);

                btnLock.setVisibility(View.GONE); // Hide primary button

                btnUnlock.setVisibility(View.VISIBLE); // Show dark button! 🌑
                btnUnlock.setOnClickListener(v -> viewModel.toggleTimeLock());

                // Small bounce Animation
                instructionsText.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction(() -> instructionsText.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150));
                break;

            case FOCUSING:
                // Minimal UI
                instructionsText.setVisibility(View.GONE);
                timePicker.setVisibility(View.GONE);
                infoButton.setVisibility(View.GONE);

                snoozeButton.setVisibility(View.VISIBLE);
                snoozeButton.setOnClickListener(v -> viewModel.snoozeTimer());

                btnUnlock.setVisibility(View.GONE);

                btnLock.setVisibility(View.VISIBLE); // Reuse primary button for STOP! 🛑
                btnLock.setText("STOP");
                btnLock.setOnClickListener(v -> viewModel.stopAlarm());
                break;

            case WARNING:
                // Red alert! Timer running but phone lifted.
                instructionsText.setVisibility(View.GONE);
                timePicker.setVisibility(View.GONE);
                infoButton.setVisibility(View.GONE);

                snoozeButton.setVisibility(View.VISIBLE);
                snoozeButton.setOnClickListener(v -> viewModel.snoozeTimer());

                btnUnlock.setVisibility(View.GONE);

                btnLock.setVisibility(View.VISIBLE);
                btnLock.setText("STOP");
                btnLock.setOnClickListener(v -> viewModel.stopAlarm());

                rootLayout.setBackgroundColor(Color.parseColor("#481D24"));
                pulseAnimation(clockView);
                break;

            case RINGING:
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText("TIME UP! ⏰\nLift phone or press Snooze");
                instructionsText.setTextColor(Color.parseColor("#C5283D"));
                timePicker.setVisibility(View.GONE);
                infoButton.setVisibility(View.GONE);

                snoozeButton.setVisibility(View.VISIBLE);
                snoozeButton.setOnClickListener(v -> viewModel.snoozeTimer());

                btnUnlock.setVisibility(View.GONE);

                btnLock.setVisibility(View.VISIBLE);
                btnLock.setText("STOP ALARM");
                btnLock.setOnClickListener(v -> viewModel.stopAlarm());

                pulseAnimation(clockView);
                break;
        }
    }

    private void pulseAnimation(View view) {
        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(400).withEndAction(() -> {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).withEndAction(() -> {
                TimerState state = viewModel.getTimerState().getValue();
                // Prevent Memory Leak. Only loop when warning or ringing
                if (state == TimerState.WARNING || state == TimerState.RINGING) {
                    pulseAnimation(view);
                }
            });
        }).start();
    }

    private void cancelPulseAnimation(View view) {
        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }

    private void showInfoDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        AppCompatImageButton closeButton = dialogView.findViewById(R.id.closeInfoDialogButton);
        AppCompatButton dismissButton = dialogView.findViewById(R.id.dismissInfoDialogButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dismissButton.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnShowListener(shownDialog -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.transparent)));
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.startSensors();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.stopSensors();
        }
    }

}
