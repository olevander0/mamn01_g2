package com.example.mamn01_g2.ui;

import android.graphics.Paint;
import android.graphics.Typeface;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import com.example.mamn01_g2.R;

import java.util.Locale;

public class TimePicker extends ConstraintLayout {

    private static final int MAX_MINUTES = 99;
    private static final float CLOCK_TEXT_SIZE_PX = 100f;
    private static final String TIME_TEXT_SAMPLE = "00:00";
    private static final String TIME_TEXT_PART_SAMPLE = "00";
    private static final String TIME_TEXT_SEPARATOR = ":";
    private static final int TARGET_HORIZONTAL_PADDING_DP = 16;
    private static final int TARGET_VERTICAL_PADDING_DP = 16;
    private static final int MIN_TARGET_WIDTH_DP = 56;
    private static final int MIN_TARGET_HEIGHT_DP = 48;

    private enum TimeField {
        MINUTES,
        SECONDS
    }

    private View minutePickerTarget;
    private View secondPickerTarget;
    private Paint timeTextPaint;
    private int selectedSeconds = 0;

    @Nullable
    private PopupWindow popupWindow;

    @Nullable
    private TimeField activeField;

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
        minutePickerTarget = findViewById(R.id.minutePickerTarget);
        secondPickerTarget = findViewById(R.id.secondPickerTarget);
        minutePickerTarget.setOnClickListener(v -> showPickerPopup(TimeField.MINUTES));
        secondPickerTarget.setOnClickListener(v -> showPickerPopup(TimeField.SECONDS));
        configureTimeTextPaint();
        updateTargetLabels();
        post(this::positionPickerTargets);
    }

    public void setSelectedSeconds(int seconds) {
        selectedSeconds = Math.max(0, seconds);
        updateTargetLabels();
    }

    public void setOnTimeSelectedListener(@Nullable OnTimeSelectedListener onTimeSelectedListener) {
        this.onTimeSelectedListener = onTimeSelectedListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        positionPickerTargets();
    }

    private void showPickerPopup(TimeField field) {
        if (popupWindow != null && popupWindow.isShowing()) {
            boolean isSameField = field == activeField;
            popupWindow.dismiss();

            if (isSameField) {
                return;
            }
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_timer_picker, this, false);
        NumberPicker valuePicker = dialogView.findViewById(R.id.timerValuePicker);
        int value = field == TimeField.MINUTES ? selectedSeconds / 60 : selectedSeconds % 60;
        int max = field == TimeField.MINUTES ? MAX_MINUTES : 59;

        activeField = field;
        configurePicker(valuePicker, 0, max, value);
        valuePicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSelectedTime(field, newVal));

        dialogView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        popupWindow = new PopupWindow(dialogView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.transparent)));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));
        popupWindow.setOnDismissListener(() -> {
            popupWindow = null;
            activeField = null;
        });

        View anchor = field == TimeField.MINUTES ? minutePickerTarget : secondPickerTarget;
        int popupWidth = dialogView.getMeasuredWidth();
        int xOffset = (anchor.getWidth() - popupWidth) / 2;
        popupWindow.showAsDropDown(anchor, xOffset, dpToPx(8));
    }

    private void configureTimeTextPaint() {
        timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeTextPaint.setTextSize(CLOCK_TEXT_SIZE_PX);
        Typeface clockTypeface = ResourcesCompat.getFont(getContext(), R.font.rubik_mono);
        timeTextPaint.setTypeface(clockTypeface);
    }

    private void positionPickerTargets() {
        if (getWidth() == 0 || getHeight() == 0 || timeTextPaint == null) {
            return;
        }

        float textWidth = timeTextPaint.measureText(TIME_TEXT_SAMPLE);
        float partWidth = timeTextPaint.measureText(TIME_TEXT_PART_SAMPLE);
        float separatorWidth = timeTextPaint.measureText(TIME_TEXT_SEPARATOR);
        float textStart = (getWidth() - textWidth) / 2f;
        float minuteCenterX = textStart + partWidth / 2f;
        float secondCenterX = textStart + partWidth + separatorWidth + partWidth / 2f;
        Paint.FontMetrics metrics = timeTextPaint.getFontMetrics();
        int targetWidth = Math.max(dpToPx(MIN_TARGET_WIDTH_DP), Math.round(partWidth + dpToPx(TARGET_HORIZONTAL_PADDING_DP)));
        int targetHeight = Math.max(dpToPx(MIN_TARGET_HEIGHT_DP), Math.round((metrics.descent - metrics.ascent) + dpToPx(TARGET_VERTICAL_PADDING_DP)));
        int top = Math.round((getHeight() - targetHeight) / 2f);

        positionTarget(minutePickerTarget, minuteCenterX, top, targetWidth, targetHeight);
        positionTarget(secondPickerTarget, secondCenterX, top, targetWidth, targetHeight);
    }

    private void positionTarget(View target, float centerX, int top, int width, int height) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) target.getLayoutParams();
        params.width = width;
        params.height = height;
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.rightToRight = ConstraintLayout.LayoutParams.UNSET;
        params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
        params.leftMargin = Math.max(0, Math.min(getWidth() - width, Math.round(centerX - width / 2f)));
        params.topMargin = Math.max(0, Math.min(getHeight() - height, top));
        target.setLayoutParams(params);
    }

    private void updateSelectedTime(TimeField field, int newValue) {
        int minutes = selectedSeconds / 60;
        int seconds = selectedSeconds % 60;

        if (field == TimeField.MINUTES) {
            minutes = newValue;
        } else {
            seconds = newValue;
        }

        int newSeconds = minutes * 60 + seconds;
        setSelectedSeconds(newSeconds);

        if (onTimeSelectedListener != null) {
            onTimeSelectedListener.onTimeSelected(newSeconds);
        }
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

    private void updateTargetLabels() {
        if (minutePickerTarget == null || secondPickerTarget == null) {
            return;
        }

        String minuteDescription = getResources().getString(R.string.edit_minutes_with_value, selectedSeconds / 60);
        String secondDescription = getResources().getString(R.string.edit_seconds_with_value, selectedSeconds % 60);
        minutePickerTarget.setContentDescription(minuteDescription);
        secondPickerTarget.setContentDescription(secondDescription);
        ViewCompat.setTooltipText(minutePickerTarget, minuteDescription);
        ViewCompat.setTooltipText(secondPickerTarget, secondDescription);
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
        void onTimeSelected(int selectedSeconds);
    }
}
