package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.sleepalarm.app.utils.PressFeedbackHelper;
import com.sleepalarm.app.utils.ThemeColors;
import com.sleepalarm.app.utils.ViewUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.sleepalarm.app.R;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private TextView tvTimerDisplay, tvHours, tvMinutes, tvSeconds;
    private NumberPicker npHours, npMinutes, npSeconds;
    private View layoutWheels;
    private MaterialButton btnStart, btnStop;

    private long totalSeconds = 0;
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private boolean isPaused = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);

        tvTimerDisplay = v.findViewById(R.id.tv_timer_display);
        layoutWheels = v.findViewById(R.id.layout_wheels);
        tvHours = v.findViewById(R.id.tv_hours);
        tvMinutes = v.findViewById(R.id.tv_minutes);
        tvSeconds = v.findViewById(R.id.tv_seconds);
        npHours = v.findViewById(R.id.np_hours);
        npMinutes = v.findViewById(R.id.np_minutes);
        npSeconds = v.findViewById(R.id.np_seconds);
        btnStart = v.findViewById(R.id.btn_timer_start);
        btnStop = v.findViewById(R.id.btn_timer_stop);

        btnStop.setEnabled(false);

        PressFeedbackHelper.apply(tvHours);
        PressFeedbackHelper.apply(tvMinutes);
        PressFeedbackHelper.apply(tvSeconds);
        PressFeedbackHelper.apply(btnStart);
        PressFeedbackHelper.apply(btnStop);

        // 设置轮盘
        setupNumberPicker(npHours, 0, 23, 0);
        setupNumberPicker(npMinutes, 0, 59, 0);
        setupNumberPicker(npSeconds, 0, 59, 0);

        // 点击文字 → 输入数字（弹出输入框）
        tvHours.setOnClickListener(view -> {
            if (isRunning) return;
            showNumberInput("输入小时", 0, 23, getHours(), val -> {
                npHours.setValue(val);
                updateFromWheels();
            });
        });
        tvMinutes.setOnClickListener(view -> {
            if (isRunning) return;
            showNumberInput("输入分钟", 0, 59, getMinutes(), val -> {
                npMinutes.setValue(val);
                updateFromWheels();
            });
        });
        tvSeconds.setOnClickListener(view -> {
            if (isRunning) return;
            showNumberInput("输入秒", 0, 59, getSeconds(), val -> {
                npSeconds.setValue(val);
                updateFromWheels();
            });
        });

        // 轮盘变化监听
        NumberPicker.OnValueChangeListener wheelListener = (picker, oldVal, newVal) -> updateFromWheels();
        npHours.setOnValueChangedListener(wheelListener);
        npMinutes.setOnValueChangedListener(wheelListener);
        npSeconds.setOnValueChangedListener(wheelListener);

        btnStart.setOnClickListener(view -> {
            if (isRunning && !isPaused) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnStop.setOnClickListener(view -> stopTimer());

        updateFromWheels();
        return v;
    }

    private void setupNumberPicker(NumberPicker picker, int min, int max, int value) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        picker.setFormatter(i -> String.format("%02d", i));
        picker.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        ViewUtils.setNumberPickerDividerColor(picker, ThemeColors.getAccent(requireContext()));
    }

    private void updateFromWheels() {
        totalSeconds = npHours.getValue() * 3600L + npMinutes.getValue() * 60L + npSeconds.getValue();
        updateDisplay();
    }

    /**
     * 弹出数字输入框（替代轮盘）
     */
    private void showNumberInput(String title, int min, int max, int initial, ValueCallback callback) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 32, 48, 32);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setCornerRadius(dpToPx(16));
        rootBg.setColor(ThemeColors.getSurface(requireContext()));
        root.setBackground(rootBg);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(14);
        tvTitle.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        EditText etInput = new EditText(requireContext());
        etInput.setText(String.format("%02d", initial));
        etInput.setTextSize(40);
        etInput.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        etInput.setGravity(Gravity.CENTER);
        etInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dpToPx(12));
        inputBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        etInput.setBackground(inputBg);
        etInput.setPadding(32, 20, 32, 20);
        etInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(etInput);

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        btnRow.setPadding(0, 20, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        btnCancel.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        btnCancel.setPadding(48, 12, 48, 12);
        PressFeedbackHelper.apply(btnCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        btnOK.setTextColor(ThemeColors.getAccent(requireContext()));
        btnOK.setPadding(48, 12, 48, 12);
        PressFeedbackHelper.apply(btnOK);
        btnOK.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                int val = Integer.parseInt(etInput.getText().toString());
                if (val >= min && val <= max) {
                    callback.onValue(val);
                }
            } catch (NumberFormatException ignored) {}
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnOK);
        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }

    private interface ValueCallback {
        void onValue(int value);
    }

    private int getHours() { return (int) (totalSeconds / 3600); }
    private int getMinutes() { return (int) ((totalSeconds % 3600) / 60); }
    private int getSeconds() { return (int) (totalSeconds % 60); }

    private void updateDisplay() {
        int h = getHours();
        int m = getMinutes();
        int s = getSeconds();

        tvHours.setText(String.format(Locale.getDefault(), "%02d", h));
        tvMinutes.setText(String.format(Locale.getDefault(), "%02d", m));
        tvSeconds.setText(String.format(Locale.getDefault(), "%02d", s));

        if (isRunning) {
            long remain = totalSeconds;
            int rh = (int) (remain / 3600);
            int rm = (int) ((remain % 3600) / 60);
            int rs = (int) (remain % 60);
            tvTimerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", rh, rm, rs));
            tvTimerDisplay.setVisibility(View.VISIBLE);
            layoutWheels.setVisibility(View.GONE);
        } else {
            tvTimerDisplay.setVisibility(View.GONE);
            layoutWheels.setVisibility(View.VISIBLE);
        }
    }

    private void startTimer() {
        if (isPaused) {
            isPaused = false;
            isRunning = true;
            btnStart.setIconResource(R.drawable.ic_pause);
            btnStop.setEnabled(true);

            countDownTimer = new CountDownTimer(totalSeconds * 1000, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    totalSeconds = millisUntilFinished / 1000;
                    updateDisplay();
                }
                @Override
                public void onFinish() {
                    totalSeconds = 0;
                    isRunning = false;
                    isPaused = false;
                    btnStart.setIconResource(R.drawable.ic_play_triangle);
                    btnStop.setEnabled(false);
                    updateDisplay();
                    try {
                        android.media.MediaPlayer mp = android.media.MediaPlayer.create(
                                requireContext(),
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                        if (mp != null) mp.start();
                    } catch (Exception ignored) {}
                }
            }.start();
        } else {
            if (totalSeconds <= 0) return;
            isRunning = true;
            isPaused = false;
            btnStart.setIconResource(R.drawable.ic_pause);
            btnStop.setEnabled(true);

            countDownTimer = new CountDownTimer(totalSeconds * 1000, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    totalSeconds = millisUntilFinished / 1000;
                    updateDisplay();
                }
                @Override
                public void onFinish() {
                    totalSeconds = 0;
                    isRunning = false;
                    isPaused = false;
                    btnStart.setIconResource(R.drawable.ic_play_triangle);
                    btnStop.setEnabled(false);
                    updateDisplay();
                    try {
                        android.media.MediaPlayer mp = android.media.MediaPlayer.create(
                                requireContext(),
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                        if (mp != null) mp.start();
                    } catch (Exception ignored) {}
                }
            }.start();
        }
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isPaused = true;
        isRunning = true;
        btnStart.setIconResource(R.drawable.ic_play_triangle);
        btnStop.setEnabled(true);
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
        isPaused = false;
        totalSeconds = 0;
        npHours.setValue(0);
        npMinutes.setValue(0);
        npSeconds.setValue(0);
        btnStart.setIconResource(R.drawable.ic_play_triangle);
        btnStop.setEnabled(false);
        updateDisplay();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
