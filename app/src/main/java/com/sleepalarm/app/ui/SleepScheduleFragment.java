package com.sleepalarm.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.sleepalarm.app.R;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.util.List;

/**
 * iOS 风格"就寝"页面
 */
public class SleepScheduleFragment extends Fragment {

    private CircleClockView circleClock;
    private MaterialSwitch switchEnabled;
    private TextView tvBedtime;
    private TextView tvWakeTime;
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
        tvBedtime = v.findViewById(R.id.tv_bedtime);
        tvWakeTime = v.findViewById(R.id.tv_wake_time);

        loadSchedule();

        circleClock.setOnTimeChangedListener((bh, bm, wh, wm) -> {
            schedule.setBedtimeHour(bh);
            schedule.setBedtimeMinute(bm);
            schedule.setWakeHour(wh);
            schedule.setWakeMinute(wm);
            updateTimeTexts();
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

        v.findViewById(R.id.btn_options).setOnClickListener(view -> {
            // 选项：可扩展为设置重复/渐响等
            showOptionsDialog();
        });

        return v;
    }

    private void loadSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        schedule = schedules.isEmpty() ? new SleepSchedule() : schedules.get(0);

        circleClock.setTimes(
                schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                schedule.getWakeHour(), schedule.getWakeMinute()
        );
        switchEnabled.setChecked(schedule.isEnabled());
        updateTimeTexts();
    }

    private void updateTimeTexts() {
        tvBedtime.setText(String.format("%02d:%02d", schedule.getBedtimeHour(), schedule.getBedtimeMinute()));
        tvWakeTime.setText(String.format("%02d:%02d", schedule.getWakeHour(), schedule.getWakeMinute()));
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

    private void showOptionsDialog() {
        String[] options = {"设置重复", "设置渐响时长", "查看每日播报"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("选项")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showRepeatDialog();
                            break;
                        case 1:
                            showGradualDialog();
                            break;
                        case 2:
                            startActivity(new android.content.Intent(requireContext(), DailyBriefingActivity.class));
                            break;
                    }
                })
                .show();
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
        int current = 2;
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
                    dialog.dismiss();
                })
                .show();
    }
}
