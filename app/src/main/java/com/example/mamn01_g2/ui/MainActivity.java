package com.example.mamn01_g2.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mamn01_g2.R;
import com.example.mamn01_g2.viewmodel.TimerViewModel;

public class MainActivity extends AppCompatActivity {
    private TimePicker timePicker;
    private TimerViewModel viewModel;
    private ClockView clockView;

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