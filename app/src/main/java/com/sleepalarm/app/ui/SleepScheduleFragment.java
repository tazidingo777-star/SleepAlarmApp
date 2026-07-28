package com.sleepalarm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sleepalarm.app.R;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.util.List;

public class SleepScheduleFragment extends Fragment {

    private CircleClockView circleClock;
    private MaterialSwitch switchEnabled;
    private MaterialButton btnRepeat, btnGradual, btnBriefing;
    private PreferencesHelper prefsHelper;
    private SleepSchedule schedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sleep, container, false);
        prefsHelper = new PreferencesHelper(requireContext());

        circleClock = v.findViewById(R.id.circle_clock);
        switchEnabled = v.findViewById(R.id.switch_sleep_enabled);
        btnRepeat = v.findViewById(R.id.btn_sleep_repeat);
        btnGradual = v.findViewById(R.id.btn_sleep_gradual);
        btnBriefing = v.findViewById(R.id.btn_sleep_briefing);

        loadSchedule();

        circleClock.setOnTimeChangedListener((bh, bm, wh, wm) -> {
            schedule.setBedtimeHour(bh);
            schedule.setBedtimeMinute(bm);
            schedule.setWakeHour(wh);
            schedule.setWakeMinute(wm);
            saveSchedule();
            if (schedule.isEnabled()) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            }
        });

        switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            schedule.setEnabled(checked);
            saveSchedule();
            if (checked) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            } else {
                AlarmManagerHelper.cancelSleepAlarm(requireContext(), schedule);
            }
        });

        btnRepeat.setOnClickListener(view -> showRepeatDialog());
        btnGradual.setOnClickListener(view -> showGradualDialog());
        btnBriefing.setOnClickListener(view -> {
            startActivity(new Intent(requireContext(), DailyBriefingActivity.class));
        });

        return v;
    }

    private void loadSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        schedule = schedules.isEmpty() ? new SleepSchedule() : schedules.get(0);

        circleClock.setBedtime(schedule.getBedtimeHour(), schedule.getBedtimeMinute());
        circleClock.setWakeTime(schedule.getWakeHour(), schedule.getWakeMinute());
        switchEnabled.setChecked(schedule.isEnabled());
        btnRepeat.setText("重复: " + schedule.getRepeatDaysText());
        btnGradual.setText("渐响: " + schedule.getGradualMinutes() + "分钟");
    }

    private void saveSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        if (schedules.isEmpty()) {
            schedules.add(schedule);
        } else {
            schedules.set(0, schedule);
        }
        prefsHelper.saveSchedules(schedules);
    }

    private void showRepeatDialog() {
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean[] selected = schedule.getRepeatDays().clone();

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("选择重复日期")
                .setMultiChoiceItems(dayNames, selected, (dialog, which, isChecked) -> {
                    selected[which] = isChecked;
                })
                .setPositiveButton("确定", (dialog, which) -> {
                    schedule.setRepeatDays(selected);
                    saveSchedule();
                    btnRepeat.setText("重复: " + schedule.getRepeatDaysText());
                    if (schedule.isEnabled()) {
                        AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showGradualDialog() {
        String[] options = {"1分钟", "3分钟", "5分钟", "10分钟", "15分钟", "20分钟", "30分钟"};
        int[] values = {1, 3, 5, 10, 15, 20, 30};
        int current = 2; // default index for 5 min
        for (int i = 0; i < values.length; i++) {
            if (values[i] == schedule.getGradualMinutes()) {
                current = i;
                break;
            }
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("渐响时长")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    schedule.setGradualMinutes(values[which]);
                    saveSchedule();
                    btnGradual.setText("渐响: " + values[which] + "分钟");
                    dialog.dismiss();
                })
                .show();
    }
}
