package com.sleepalarm.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.ui.DailyBriefingActivity;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION = 100;
    private static final int REQUEST_EXACT_ALARM = 101;

    private PreferencesHelper prefsHelper;
    private SleepSchedule currentSchedule;

    // UI组件
    private TextView tvBedtime, tvWakeTime, tvSleepDuration;
    private TextView tvRepeatDays;
    private MaterialSwitch switchEnabled;
    private Slider sliderGradual;
    private TextView tvGradualLabel;
    private LinearLayout layoutDaysSelector;
    private MaterialButton[] dayButtons = new MaterialButton[7];

    private ActivityResultLauncher<String> notificationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsHelper = new PreferencesHelper(this);

        // 注册权限请求
        notificationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        setupAlarms();
                    }
                });

        initViews();
        loadSchedule();
        setupListeners();
        checkAndRequestPermissions();
    }

    private void initViews() {
        tvBedtime = findViewById(R.id.tv_bedtime);
        tvWakeTime = findViewById(R.id.tv_wake_time);
        tvSleepDuration = findViewById(R.id.tv_sleep_duration);
        tvRepeatDays = findViewById(R.id.tv_repeat_days);
        switchEnabled = findViewById(R.id.switch_enabled);
        sliderGradual = findViewById(R.id.slider_gradual);
        tvGradualLabel = findViewById(R.id.tv_gradual_label);
        layoutDaysSelector = findViewById(R.id.layout_days_selector);

        // 星期选择按钮
        String[] dayNames = {"日", "一", "二", "三", "四", "五", "六"};
        int[] dayButtonIds = {
                R.id.btn_sun, R.id.btn_mon, R.id.btn_tue,
                R.id.btn_wed, R.id.btn_thu, R.id.btn_fri, R.id.btn_sat
        };
        for (int i = 0; i < 7; i++) {
            dayButtons[i] = findViewById(dayButtonIds[i]);
            final int dayIndex = i;
            dayButtons[i].setText(dayNames[i]);
            dayButtons[i].setOnClickListener(v -> toggleDay(dayIndex));
        }

        // 每日播报按钮
        findViewById(R.id.btn_briefing).setOnClickListener(v -> {
            startActivity(new Intent(this, DailyBriefingActivity.class));
        });

        // 就寝时间设置
        findViewById(R.id.layout_bedtime).setOnClickListener(v -> showTimePicker(true));

        // 起床时间设置
        findViewById(R.id.layout_wake_time).setOnClickListener(v -> showTimePicker(false));
    }

    private void loadSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        if (!schedules.isEmpty()) {
            currentSchedule = schedules.get(0);
        } else {
            currentSchedule = new SleepSchedule();
        }
        updateUI();
    }

    private void updateUI() {
        tvBedtime.setText(currentSchedule.getBedtimeText());
        tvWakeTime.setText(currentSchedule.getWakeTimeText());
        tvSleepDuration.setText("预计睡眠 " + currentSchedule.getSleepDurationText());
        tvRepeatDays.setText(currentSchedule.getRepeatDaysText());
        switchEnabled.setChecked(currentSchedule.isEnabled());
        sliderGradual.setValue(currentSchedule.getGradualMinutes());
        tvGradualLabel.setText("渐响时长: " + currentSchedule.getGradualMinutes() + "分钟");

        // 更新星期按钮状态
        for (int i = 0; i < 7; i++) {
            updateDayButtonStyle(i, currentSchedule.getRepeatDays()[i]);
        }

        // 渐响滑块
        sliderGradual.addOnChangeListener((slider, value, fromUser) -> {
            int minutes = (int) value;
            currentSchedule.setGradualMinutes(minutes);
            tvGradualLabel.setText("渐响时长: " + minutes + "分钟");
            saveSchedule();
        });
    }

    private void setupListeners() {
        // 启用/禁用开关
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentSchedule.setEnabled(isChecked);
            saveSchedule();
            if (isChecked) {
                setAlarmForSchedule();
            } else {
                AlarmManagerHelper.cancelAlarmsForSchedule(this, currentSchedule);
            }
        });
    }

    /**
     * 显示时间选择器
     */
    private void showTimePicker(boolean isBedtime) {
        int hour = isBedtime ? currentSchedule.getBedtimeHour() : currentSchedule.getWakeHour();
        int minute = isBedtime ? currentSchedule.getBedtimeMinute() : currentSchedule.getWakeMinute();

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(isBedtime ? "设置就寝时间" : "设置起床时间")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int newHour = picker.getHour();
            int newMinute = picker.getMinute();
            if (isBedtime) {
                currentSchedule.setBedtimeHour(newHour);
                currentSchedule.setBedtimeMinute(newMinute);
            } else {
                currentSchedule.setWakeHour(newHour);
                currentSchedule.setWakeMinute(newMinute);
            }
            updateUI();
            saveSchedule();
            if (currentSchedule.isEnabled()) {
                setAlarmForSchedule();
            }
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    /**
     * 切换某一天的选中状态
     */
    private void toggleDay(int dayIndex) {
        boolean[] days = currentSchedule.getRepeatDays();
        days[dayIndex] = !days[dayIndex];
        updateDayButtonStyle(dayIndex, days[dayIndex]);
        tvRepeatDays.setText(currentSchedule.getRepeatDaysText());
        saveSchedule();
        if (currentSchedule.isEnabled()) {
            setAlarmForSchedule();
        }
    }

    /**
     * 更新星期按钮样式
     */
    private void updateDayButtonStyle(int dayIndex, boolean selected) {
        MaterialButton btn = dayButtons[dayIndex];
        if (selected) {
            btn.setStrokeColorResource(R.color.primary);
            btn.setTextColor(getColor(R.color.primary));
        } else {
            btn.setStrokeColorResource(R.color.gray_light);
            btn.setTextColor(getColor(R.color.gray_light));
        }
    }

    /**
     * 保存当前安排
     */
    private void saveSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        if (schedules.isEmpty()) {
            schedules.add(currentSchedule);
        } else {
            for (int i = 0; i < schedules.size(); i++) {
                if (schedules.get(i).getId() == currentSchedule.getId()) {
                    schedules.set(i, currentSchedule);
                    break;
                }
            }
        }
        prefsHelper.saveSchedules(schedules);
    }

    /**
     * 设置闹钟
     */
    private void setAlarmForSchedule() {
        AlarmManagerHelper.cancelAlarmsForSchedule(this, currentSchedule);
        AlarmManagerHelper.setAlarmForSchedule(this, currentSchedule);
    }

    /**
     * 检查并请求必要权限
     */
    private void checkAndRequestPermissions() {
        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Android 12+ 精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, REQUEST_EXACT_ALARM);
            } else {
                setupAlarms();
            }
        } else {
            setupAlarms();
        }
    }

    private void setupAlarms() {
        if (currentSchedule.isEnabled()) {
            setAlarmForSchedule();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXACT_ALARM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                    setupAlarms();
                } else {
                    Toast.makeText(this, "需要精确闹钟权限才能正常使用", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
