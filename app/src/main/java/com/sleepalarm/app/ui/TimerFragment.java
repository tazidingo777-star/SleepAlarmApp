package com.sleepalarm.app.ui;

import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.sleepalarm.app.R;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private TextView tvTimerDisplay, tvHours, tvMinutes, tvSeconds;
    private MaterialButton btnNum1, btnNum2, btnNum3, btnNum4, btnNum5,
            btnNum6, btnNum7, btnNum8, btnNum9, btnNum0;
    private MaterialButton btnClear, btnStart, btnReset;

    private StringBuilder input = new StringBuilder();
    private long totalSeconds = 0;
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);

        tvTimerDisplay = v.findViewById(R.id.tv_timer_display);
        tvHours = v.findViewById(R.id.tv_hours);
        tvMinutes = v.findViewById(R.id.tv_minutes);
        tvSeconds = v.findViewById(R.id.tv_seconds);

        btnNum0 = v.findViewById(R.id.btn_num_0);
        btnNum1 = v.findViewById(R.id.btn_num_1);
        btnNum2 = v.findViewById(R.id.btn_num_2);
        btnNum3 = v.findViewById(R.id.btn_num_3);
        btnNum4 = v.findViewById(R.id.btn_num_4);
        btnNum5 = v.findViewById(R.id.btn_num_5);
        btnNum6 = v.findViewById(R.id.btn_num_6);
        btnNum7 = v.findViewById(R.id.btn_num_7);
        btnNum8 = v.findViewById(R.id.btn_num_8);
        btnNum9 = v.findViewById(R.id.btn_num_9);
        btnClear = v.findViewById(R.id.btn_timer_clear);
        btnStart = v.findViewById(R.id.btn_timer_start);
        btnReset = v.findViewById(R.id.btn_timer_reset);

        View.OnClickListener numListener = view -> {
            if (isRunning) return;
            MaterialButton btn = (MaterialButton) view;
            String num = btn.getText().toString();
            if (input.length() < 6) { // HHMMSS max
                input.append(num);
                updateDisplay();
            }
        };

        btnNum0.setOnClickListener(numListener);
        btnNum1.setOnClickListener(numListener);
        btnNum2.setOnClickListener(numListener);
        btnNum3.setOnClickListener(numListener);
        btnNum4.setOnClickListener(numListener);
        btnNum5.setOnClickListener(numListener);
        btnNum6.setOnClickListener(numListener);
        btnNum7.setOnClickListener(numListener);
        btnNum8.setOnClickListener(numListener);
        btnNum9.setOnClickListener(numListener);

        btnClear.setOnClickListener(view -> {
            if (input.length() > 0) {
                input.deleteCharAt(input.length() - 1);
                updateDisplay();
            }
        });
        btnClear.setOnLongClickListener(v -> {
            input.setLength(0);
            updateDisplay();
            return true;
        });

        btnStart.setOnClickListener(view -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(view -> {
            stopTimer();
            input.setLength(0);
            updateDisplay();
        });

        updateDisplay();
        return v;
    }

    private void updateDisplay() {
        parseInput();
        int h = (int) (totalSeconds / 3600);
        int m = (int) ((totalSeconds % 3600) / 60);
        int s = (int) (totalSeconds % 60);

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

    private void parseInput() {
        String s = input.toString();
        while (s.length() < 6) s = "0" + s;
        int h = Integer.parseInt(s.substring(0, 2));
        int m = Integer.parseInt(s.substring(2, 4));
        int sec = Integer.parseInt(s.substring(4, 6));
        totalSeconds = h * 3600L + m * 60L + sec;
    }

    private void startTimer() {
        parseInput();
        if (totalSeconds <= 0) return;

        isRunning = true;
        btnStart.setText("暂停");
        btnReset.setEnabled(false);

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
                btnStart.setText("开始");
                btnReset.setEnabled(true);
                updateDisplay();

                // 播放提示音
                try {
                    android.media.MediaPlayer mp = android.media.MediaPlayer.create(
                            requireContext(),
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                    if (mp != null) mp.start();
                } catch (Exception ignored) {}
            }
        }.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        btnStart.setText("继续");
        btnReset.setEnabled(true);
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        totalSeconds = 0;
        btnStart.setText("开始");
        btnReset.setEnabled(false);
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
