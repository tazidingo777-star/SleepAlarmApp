package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.sleepalarm.app.utils.ViewUtils;

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

        // Click bedtime time text to edit
        tvBedtime.setOnClickListener(view -> {
            showTimePicker("设置就寝时间", schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                    (h, m) -> {
                        schedule.setBedtimeHour(h);
                        schedule.setBedtimeMinute(m);
                        circleClock.setTimes(schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                                schedule.getWakeHour(), schedule.getWakeMinute());
                        updateTimeTexts();
                        saveSchedule();
                        if (schedule.isEnabled()) {
                            AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
                        }
                    });
        });

        // Click wake time text to edit
        tvWakeTime.setOnClickListener(view -> {
            showTimePicker("设置起床时间", schedule.getWakeHour(), schedule.getWakeMinute(),
                    (h, m) -> {
                        schedule.setWakeHour(h);
                        schedule.setWakeMinute(m);
                        circleClock.setTimes(schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                                schedule.getWakeHour(), schedule.getWakeMinute());
                        updateTimeTexts();
                        saveSchedule();
                        if (schedule.isEnabled()) {
                            AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
                        }
                    });
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
            showOptionsDialog();
        });

        return v;
    }

    private void showTimePicker(String title, int initHour, int initMinute, TimePickedCallback callback) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 32, 40, 32);
        root.setBackgroundColor(0xFF1C1C1E);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setPadding(0, 0, 0, 20);
        root.addView(tvTitle);

        LinearLayout pickerRow = new LinearLayout(requireContext());
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(android.view.Gravity.CENTER);

        NumberPicker hourPicker = new NumberPicker(requireContext());
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(initHour);
        hourPicker.setFormatter(i -> String.format("%02d", i));
        hourPicker.setTextColor(Color.WHITE);
        ViewUtils.setNumberPickerDividerColor(hourPicker, Color.parseColor("#FF9500"));

        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(32);
        sep.setTextColor(Color.WHITE);
        sep.setPadding(16, 0, 16, 0);

        NumberPicker minutePicker = new NumberPicker(requireContext());
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(initMinute);
        minutePicker.setFormatter(i -> String.format("%02d", i));
        minutePicker.setTextColor(Color.WHITE);
        ViewUtils.setNumberPickerDividerColor(minutePicker, Color.parseColor("#FF9500"));

        pickerRow.addView(hourPicker);
        pickerRow.addView(sep);
        pickerRow.addView(minutePicker);
        root.addView(pickerRow);

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        btnRow.setPadding(0, 24, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(Color.parseColor("#8E8E93"));
        btnCancel.setPadding(40, 12, 40, 12);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTextColor(Color.parseColor("#FF9500"));
        btnOK.setPadding(40, 12, 40, 12);
        btnOK.setOnClickListener(v2 -> {
            dialog.dismiss();
            callback.onTimePicked(hourPicker.getValue(), minutePicker.getValue());
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnOK);
        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }

    private interface TimePickedCallback {
        void onTimePicked(int hour, int minute);
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
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);
        root.setBackgroundColor(0xFF1C1C1E);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("选项");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setPadding(40, 32, 40, 8);
        root.addView(tvTitle);

        addOptionItem(root, "设置重复", () -> {
            dialog.dismiss();
            showRepeatDialog();
        });
        addOptionItem(root, "设置渐响时长", () -> {
            dialog.dismiss();
            showGradualDialog();
        });
        addOptionItem(root, "查看每日播报", () -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), DailyBriefingActivity.class));
        });

        TextView btnClose = new TextView(requireContext());
        btnClose.setText("取消");
        btnClose.setTextSize(16);
        btnClose.setTextColor(Color.parseColor("#FF9500"));
        btnClose.setGravity(android.view.Gravity.CENTER);
        btnClose.setPadding(40, 24, 40, 32);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        dialog.show();
    }

    private void addOptionItem(LinearLayout root, String text, Runnable action) {
        TextView item = new TextView(requireContext());
        item.setText(text);
        item.setTextSize(17);
        item.setTextColor(Color.WHITE);
        item.setPadding(40, 16, 40, 16);
        item.setBackgroundColor(0xFF1C1C1E);
        item.setOnClickListener(v -> action.run());
        root.addView(item);

        // Separator
        View sep = new View(requireContext());
        sep.setBackgroundColor(0xFF2C2C2E);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        root.addView(sep);
    }

    private void showRepeatDialog() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 32, 40, 32);
        root.setBackgroundColor(0xFF1C1C1E);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("选择重复日期");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setPadding(0, 0, 0, 20);
        root.addView(tvTitle);

        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean[] selected = schedule.getRepeatDays().clone();
        TextView[] dayViews = new TextView[7];

        LinearLayout daysContainer = new LinearLayout(requireContext());
        daysContainer.setOrientation(LinearLayout.HORIZONTAL);
        daysContainer.setGravity(android.view.Gravity.CENTER);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView dayView = new TextView(requireContext());
            dayView.setText(dayNames[i]);
            dayView.setTextSize(14);
            dayView.setPadding(8, 10, 8, 10);
            dayView.setGravity(android.view.Gravity.CENTER);
            if (selected[i]) {
                dayView.setTextColor(Color.BLACK);
                dayView.setBackgroundColor(Color.parseColor("#FF9500"));
            } else {
                dayView.setTextColor(Color.parseColor("#8E8E93"));
                dayView.setBackgroundColor(0xFF2C2C2E);
            }
            dayView.setOnClickListener(v2 -> {
                selected[idx] = !selected[idx];
                if (selected[idx]) {
                    dayView.setTextColor(Color.BLACK);
                    dayView.setBackgroundColor(Color.parseColor("#FF9500"));
                } else {
                    dayView.setTextColor(Color.parseColor("#8E8E93"));
                    dayView.setBackgroundColor(0xFF2C2C2E);
                }
            });
            dayViews[i] = dayView;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(3, 0, 3, 0);
            dayView.setLayoutParams(params);
            daysContainer.addView(dayView);
        }
        root.addView(daysContainer);

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        btnRow.setPadding(0, 24, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(Color.parseColor("#8E8E93"));
        btnCancel.setPadding(40, 12, 40, 12);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTextColor(Color.parseColor("#FF9500"));
        btnOK.setPadding(40, 12, 40, 12);
        btnOK.setOnClickListener(v2 -> {
            dialog.dismiss();
            schedule.setRepeatDays(selected);
            saveSchedule();
            if (schedule.isEnabled()) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            }
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnOK);
        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }

    private void showGradualDialog() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 32, 40, 32);
        root.setBackgroundColor(0xFF1C1C1E);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("渐响时长");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setPadding(0, 0, 0, 20);
        root.addView(tvTitle);

        String[] options = {"1分钟", "3分钟", "5分钟", "10分钟", "15分钟", "20分钟", "30分钟"};
        int[] values = {1, 3, 5, 10, 15, 20, 30};

        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            TextView item = new TextView(requireContext());
            item.setText(options[i]);
            item.setTextSize(17);
            item.setTextColor(Color.WHITE);
            item.setPadding(0, 14, 0, 14);
            if (values[i] == schedule.getGradualMinutes()) {
                item.setTextColor(Color.parseColor("#FF9500"));
            }
            item.setOnClickListener(v2 -> {
                dialog.dismiss();
                schedule.setGradualMinutes(values[idx]);
                saveSchedule();
            });
            root.addView(item);

            if (i < options.length - 1) {
                View sep = new View(requireContext());
                sep.setBackgroundColor(0xFF2C2C2E);
                sep.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                root.addView(sep);
            }
        }

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        btnRow.setPadding(0, 16, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(Color.parseColor("#8E8E93"));
        btnCancel.setPadding(40, 12, 40, 12);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        btnRow.addView(btnCancel);
        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }
}
