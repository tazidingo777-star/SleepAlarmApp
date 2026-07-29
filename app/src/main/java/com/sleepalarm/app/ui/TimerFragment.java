package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.sleepalarm.app.R;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private TextView tvTimerDisplay, tvHours, tvMinutes, tvSeconds;
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
        tvHours = v.findViewById(R.id.tv_hours);
        tvMinutes = v.findViewById(R.id.tv_minutes);
        tvSeconds = v.findViewById(R.id.tv_seconds);
        btnStart = v.findViewById(R.id.btn_timer_start);
        btnStop = v.findViewById(R.id.btn_timer_stop);

        btnStop.setEnabled(false);

        // Click time texts to edit via wheel picker
        tvHours.setOnClickListener(view -> {
            if (isRunning) return;
            showNumberPicker("设置小时", 0, 23, getHours(), val -> {
                setHours(val);
                updateDisplay();
            });
        });
        tvMinutes.setOnClickListener(view -> {
            if (isRunning) return;
            showNumberPicker("设置分钟", 0, 59, getMinutes(), val -> {
                setMinutes(val);
                updateDisplay();
            });
        });
        tvSeconds.setOnClickListener(view -> {
            if (isRunning) return;
            showNumberPicker("设置秒", 0, 59, getSeconds(), val -> {
                setSeconds(val);
                updateDisplay();
            });
        });

        btnStart.setOnClickListener(view -> {
            if (isRunning && !isPaused) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnStop.setOnClickListener(view -> {
            stopTimer();
        });

        updateDisplay();
        return v;
    }

    private int getHours() {
        return (int) (totalSeconds / 3600);
    }

    private int getMinutes() {
        return (int) ((totalSeconds % 3600) / 60);
    }

    private int getSeconds() {
        return (int) (totalSeconds % 60);
    }

    private void setHours(int h) {
        int m = getMinutes();
        int s = getSeconds();
        totalSeconds = h * 3600L + m * 60L + s;
        updateDisplay();
    }

    private void setMinutes(int m) {
        int h = getHours();
        int s = getSeconds();
        totalSeconds = h * 3600L + m * 60L + s;
        updateDisplay();
    }

    private void setSeconds(int s) {
        int h = getHours();
        int m = getMinutes();
        totalSeconds = h * 3600L + m * 60L + s;
        updateDisplay();
    }

    private void showNumberPicker(String title, int min, int max, int initial, ValueCallback callback) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 28, 40, 28);
        root.setBackgroundColor(0xFF1C1C1E);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(Color.parseColor("#8E8E93"));
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(initial);
        picker.setFormatter(i -> String.format("%02d", i));
        picker.setTextColor(Color.WHITE);
        picker.setDividerColor(Color.parseColor("#FF9500"));
        root.addView(picker);

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        btnRow.setPadding(0, 20, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(Color.parseColor("#8E8E93"));
        btnCancel.setPadding(40, 12, 40, 12);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTextColor(Color.parseColor("#FF9500"));
        btnOK.setPadding(40, 12, 40, 12);
        btnOK.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onValue(picker.getValue());
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
        } else {
            tvTimerDisplay.setVisibility(View.GONE);
        }
    }

    private void startTimer() {
        if (isPaused) {
            // Resume
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
            // Fresh start
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
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isPaused = true;
        isRunning = true;
        btnStart.setIconResource(R.drawable.ic_play_triangle);
        btnStop.setEnabled(true);
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        isPaused = false;
        totalSeconds = 0;
        btnStart.setIconResource(R.drawable.ic_play_triangle);
        btnStop.setEnabled(false);
        updateDisplay();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
