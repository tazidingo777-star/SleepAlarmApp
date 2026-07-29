package com.sleepalarm.app.ui;

import android.os.Bundle;
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
import com.sleepalarm.app.utils.PressFeedbackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StopwatchFragment extends Fragment {

    private TextView tvTime, tvLaps;
    private MaterialButton btnStart, btnLap, btnReset;

    private Handler handler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean running = false;
    private List<Long> laps = new ArrayList<>();

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateDisplay();
                handler.postDelayed(this, 30);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stopwatch, container, false);

        tvTime = v.findViewById(R.id.tv_stopwatch_time);
        tvLaps = v.findViewById(R.id.tv_laps);
        btnStart = v.findViewById(R.id.btn_stopwatch_start);
        btnLap = v.findViewById(R.id.btn_stopwatch_lap);
        btnReset = v.findViewById(R.id.btn_stopwatch_reset);

        PressFeedbackHelper.apply(btnReset);
        PressFeedbackHelper.apply(btnStart);
        PressFeedbackHelper.apply(btnLap);

        updateDisplay();
        btnLap.setEnabled(false);

        btnStart.setOnClickListener(view -> {
            if (!running) {
                running = true;
                startTime = System.currentTimeMillis() - elapsedTime;
                handler.post(updateRunnable);
                btnStart.setIconResource(R.drawable.ic_pause);
                btnLap.setEnabled(true);
                btnReset.setEnabled(false);
            } else {
                running = false;
                handler.removeCallbacks(updateRunnable);
                elapsedTime = System.currentTimeMillis() - startTime;
                btnStart.setIconResource(R.drawable.ic_play_triangle);
                btnLap.setEnabled(false);
                btnReset.setEnabled(true);
            }
        });

        btnLap.setOnClickListener(view -> {
            long lapTime = System.currentTimeMillis() - startTime;
            laps.add(0, lapTime);
            updateLapsDisplay();
        });

        btnReset.setOnClickListener(view -> {
            running = false;
            handler.removeCallbacks(updateRunnable);
            elapsedTime = 0;
            startTime = 0;
            laps.clear();
            btnStart.setIconResource(R.drawable.ic_play_triangle);
            btnLap.setEnabled(false);
            btnReset.setEnabled(false);
            updateDisplay();
            tvLaps.setText("");
        });

        return v;
    }

    private void updateDisplay() {
        tvTime.setText(formatTime(elapsedTime));
    }

    private void updateLapsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(laps.size(), 20); i++) {
            sb.append("计次 ").append(laps.size() - i).append("  ")
                    .append(formatTime(laps.get(i))).append("\n");
        }
        tvLaps.setText(sb.toString());
    }

    private String formatTime(long millis) {
        int mins = (int) (millis / 60000);
        int secs = (int) ((millis % 60000) / 1000);
        int ms = (int) ((millis % 1000) / 10);
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", mins, secs, ms);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateRunnable);
    }
}
