package com.example.mamn01_g2.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.mamn01_g2.R;

import java.util.Locale;

public class TimePicker extends ConstraintLayout {

    private static final int MAX_MINUTES = 99;
    private static final String EDIT_TIMER_DESCRIPTION = "Edit timer";
    private static final String EDIT_TIMER_WITH_VALUE_DESCRIPTION = "Edit timer, currently %s";

    private AppCompatImageButton openPickerButton;
    private int selectedSeconds = 0;

    @Nullable
    private PopupWindow popupWindow;

    @Nullable
    private OnTimeSelectedListener onTimeSelectedListener;

    public TimePicker(android.content.Context context) {
        super(context);
        init();
    }

    public TimePicker(android.content.Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimePicker(android.content.Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_time_picker, this, true);
        openPickerButton = findViewById(R.id.openTimerPickerButton);
        openPickerButton.setOnClickListener(v -> showPickerPopup());
        updateButtonLabel();
    }

    public void setSelectedSeconds(int seconds) {
        selectedSeconds = Math.max(0, seconds);
        updateButtonLabel();
    }

    public void setOnTimeSelectedListener(@Nullable OnTimeSelectedListener onTimeSelectedListener) {
        this.onTimeSelectedListener = onTimeSelectedListener;
    }

    private void showPickerPopup() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_timer_picker, this, false);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minutePicker);
        NumberPicker secondPicker = dialogView.findViewById(R.id.secondPicker);
        AppCompatImageButton cancelButton = dialogView.findViewById(R.id.cancelTimerPickerButton);
        AppCompatImageButton applyButton = dialogView.findViewById(R.id.applyTimerPickerButton);
        final int originalSeconds = selectedSeconds;
        final boolean[] wasApplied = {false};

        configurePicker(minutePicker, 0, MAX_MINUTES, selectedSeconds / 60);
        configurePicker(secondPicker, 0, 59, selectedSeconds % 60);

        NumberPicker.OnValueChangeListener previewListener = (picker, oldVal, newVal) -> {
            int previewSeconds = minutePicker.getValue() * 60 + secondPicker.getValue();
            setSelectedSeconds(previewSeconds);

            if (onTimeSelectedListener != null) {
                onTimeSelectedListener.onTimePreviewChanged(previewSeconds);
            }
        };
        minutePicker.setOnValueChangedListener(previewListener);
        secondPicker.setOnValueChangedListener(previewListener);

        dialogView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        popupWindow = new PopupWindow(dialogView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.transparent)));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));
        popupWindow.setOnDismissListener(() -> {
            popupWindow = null;

            if (!wasApplied[0]) {
                setSelectedSeconds(originalSeconds);

                if (onTimeSelectedListener != null) {
                    onTimeSelectedListener.onTimeSelectionCancelled(originalSeconds);
                }
            }
        });

        cancelButton.setContentDescription("Close timer picker");
        applyButton.setContentDescription("Apply timer");
        cancelButton.setOnClickListener(v -> {
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
        });

        applyButton.setOnClickListener(v -> {
            wasApplied[0] = true;
            int newSeconds = minutePicker.getValue() * 60 + secondPicker.getValue();

            // Removed the old CountDownTimer cancellation logic from here!
            setSelectedSeconds(newSeconds);

            if (onTimeSelectedListener != null) {
                onTimeSelectedListener.onTimeSelected(newSeconds);
            }

            if (popupWindow != null) {
                popupWindow.dismiss();
            }
        });

        int popupWidth = dialogView.getMeasuredWidth();
        int xOffset = -Math.max(0, (popupWidth - openPickerButton.getWidth()) / 2);
        popupWindow.showAsDropDown(openPickerButton, xOffset, dpToPx(8));
    }

    private void configurePicker(NumberPicker picker, int min, int max, int value) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.min(max, Math.max(min, value)));
        picker.setWrapSelectorWheel(true);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setFormatter(pickerValue -> String.format(Locale.getDefault(), "%02d", pickerValue));
        stylePicker(picker);
        picker.post(() -> stylePicker(picker));
    }

    private void updateButtonLabel() {
        if (openPickerButton == null) {
            return;
        }

        String description = selectedSeconds > 0 ? String.format(Locale.getDefault(), EDIT_TIMER_WITH_VALUE_DESCRIPTION, formatDuration(selectedSeconds)) : EDIT_TIMER_DESCRIPTION;
        openPickerButton.setContentDescription(description);
        ViewCompat.setTooltipText(openPickerButton, description);
    }

    private String formatDuration(int totalSeconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void stylePicker(NumberPicker picker) {
        int pickerTextColor = ContextCompat.getColor(getContext(), R.color.text_color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            picker.setTextColor(pickerTextColor);
            picker.setTextSize(spToPx(16));
        }

        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                editText.setTextColor(pickerTextColor);
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        }
    }

    private float spToPx(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    public interface OnTimeSelectedListener {
        void onTimePreviewChanged(int selectedSeconds);

        void onTimeSelected(int selectedSeconds);

        void onTimeSelectionCancelled(int restoredSeconds);
    }
}