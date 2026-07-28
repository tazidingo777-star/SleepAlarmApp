package com.sleepalarm.app.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sleepalarm.app.AlarmService;
import com.sleepalarm.app.R;
import com.sleepalarm.app.utils.AlarmManagerHelper;

/**
 * 闹钟响铃全屏界面 - 显示闹钟信息，提供关闭按钮
 */
public class AlarmAlertActivity extends AppCompatActivity {

    private TextView tvAlarmTitle;
    private TextView tvAlarmTime;
    private TextView tvAlarmSubtitle;
    private AlarmService alarmService;
    private boolean isBedtime;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AlarmService.LocalBinder binder = (AlarmService.LocalBinder) service;
            alarmService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            alarmService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_alert);

        // 保持屏幕常亮，在锁屏上显示
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        Intent intent = getIntent();
        isBedtime = intent.getBooleanExtra(AlarmManagerHelper.EXTRA_IS_BEDTIME, false);
        int hour = isBedtime ? intent.getIntExtra(AlarmManagerHelper.EXTRA_BEDTIME_HOUR, 23)
                : intent.getIntExtra(AlarmManagerHelper.EXTRA_WAKE_HOUR, 7);
        int minute = isBedtime ? intent.getIntExtra(AlarmManagerHelper.EXTRA_BEDTIME_MINUTE, 0)
                : intent.getIntExtra(AlarmManagerHelper.EXTRA_WAKE_MINUTE, 0);

        initViews(hour, minute);
        setupListeners();

        // 绑定服务
        bindService(new Intent(this, AlarmService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews(int hour, int minute) {
        tvAlarmTitle = findViewById(R.id.tv_alarm_title);
        tvAlarmTime = findViewById(R.id.tv_alarm_time);
        tvAlarmSubtitle = findViewById(R.id.tv_alarm_subtitle);

        String timeText = String.format("%02d:%02d", hour, minute);
        tvAlarmTime.setText(timeText);

        if (isBedtime) {
            tvAlarmTitle.setText("该睡觉了");
            tvAlarmSubtitle.setText("充足的睡眠让你精力充沛");
        } else {
            tvAlarmTitle.setText("早上好");
            tvAlarmSubtitle.setText("新的一天开始了，加油！");
        }
    }

    private void setupListeners() {
        // 关闭闹钟按钮
        findViewById(R.id.btn_dismiss).setOnClickListener(v -> dismissAlarm());

        // 稍后提醒按钮（仅起床闹钟有）
        View btnSnooze = findViewById(R.id.btn_snooze);
        if (!isBedtime && btnSnooze != null) {
            btnSnooze.setVisibility(View.VISIBLE);
            btnSnooze.setOnClickListener(v -> snoozeAlarm());
        } else if (btnSnooze != null) {
            btnSnooze.setVisibility(View.GONE);
        }

        // 查看每日播报（仅起床闹钟显示）
        View btnBriefing = findViewById(R.id.btn_briefing_from_alarm);
        if (!isBedtime && btnBriefing != null) {
            btnBriefing.setVisibility(View.VISIBLE);
            btnBriefing.setOnClickListener(v -> {
                dismissAlarm();
                startActivity(new Intent(this, DailyBriefingActivity.class));
            });
        } else if (btnBriefing != null) {
            btnBriefing.setVisibility(View.GONE);
        }
    }

    /**
     * 关闭闹钟
     */
    private void dismissAlarm() {
        stopAlarmService();
        finish();
    }

    /**
     * 稍后提醒 - 10分钟后再次响铃
     */
    private void snoozeAlarm() {
        // TODO: 实现稍后提醒（可通过AlarmManager设置10分钟后的闹钟）
        stopAlarmService();
        finish();
        // 这里可以设置一个10分钟后的临时闹钟
    }

    /**
     * 停止闹钟服务
     */
    private void stopAlarmService() {
        if (isBound && alarmService != null) {
            alarmService.stopAlarm();
            unbindService(serviceConnection);
            isBound = false;
        }
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        // 禁止返回键关闭闹钟
    }
}
