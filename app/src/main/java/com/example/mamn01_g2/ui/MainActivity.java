package com.example.mamn01_g2.ui;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.mamn01_g2.R;
import com.example.mamn01_g2.viewmodel.TimerViewModel;

public class MainActivity extends AppCompatActivity {
    private TimePicker timePicker;
    private TimerViewModel viewModel;
    private ClockView clockView;

    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clockView = findViewById(R.id.clock_view);
        viewModel = new ViewModelProvider(this).get(TimerViewModel.class);

        timePicker = findViewById(R.id.time_picker);

        timePicker.setOnTimeSelectedListener(new TimePicker.OnTimeSelectedListener() {
            @Override
            public void onTimePreviewChanged(int selectedSeconds) {
                viewModel.manuallySetTime(selectedSeconds * 1000L);
            }

            @Override
            public void onTimeSelected(int selectedSeconds) {
                viewModel.manuallySetTime(selectedSeconds * 1000L);
            }

            @Override
            public void onTimeSelectionCancelled(int restoredSeconds) {
                viewModel.manuallySetTime(restoredSeconds * 1000L);
            }
        });

        viewModel.getCurrentTime().observe(this, timeInMillis -> {
            if (!viewModel.isTimerRunning()) {
                clockView.setInitialTime(timeInMillis);
            } else {
                clockView.updateTime(timeInMillis);
            }

            timePicker.setSelectedSeconds((int) (timeInMillis / 1000));
        });
        viewModel.getLockInState().observe(this, isLockedIn -> {
            updateUILockIn(isLockedIn);
        });
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

    private void updateUILockIn(boolean isLockedIn) {
        ConstraintLayout rootLayout = findViewById(R.id.main);

        if (isLockedIn) {
            showWarningDialog();
            rootLayout.setBackgroundColor(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)
            );
        } else {
            hideWarningDialog();
            rootLayout.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.background_color)
            );
        }
    }

    private void showWarningDialog() {
        if (alertDialog == null || !alertDialog.isShowing()) {
            alertDialog = new AlertDialog.Builder(this)
                    .setTitle("LOCK IN!!!")
                    .setMessage("Turn phone facedown")
                    .setCancelable(false)
                    .create();
            alertDialog.show();
            if (alertDialog.getWindow() != null) {
                WindowManager.LayoutParams params =
                        alertDialog.getWindow().getAttributes();

                params.y = -300; // negative = move upward

                alertDialog.getWindow().setAttributes(params);
            }
        }
    }

    private void hideWarningDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }
}