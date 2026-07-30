package com.sleepalarm.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sleepalarm.app.ui.AlarmsFragment;
import com.sleepalarm.app.ui.AlarmAlertActivity;
import com.sleepalarm.app.ui.SleepScheduleFragment;
import com.sleepalarm.app.ui.StopwatchFragment;
import com.sleepalarm.app.ui.TimerFragment;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;
import com.sleepalarm.app.utils.ThemeColors;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> notificationLauncher;
    private PreferencesHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在 setContentView 前应用主题
        prefsHelper = new PreferencesHelper(this);
        if (prefsHelper.isDarkTheme()) {
            setTheme(R.style.Theme_SleepAlarm);
        } else {
            setTheme(R.style.Theme_SleepAlarm_Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { /* 权限结果处理 */ });

        // 深色/浅色主题切换（点击月亮/太阳图标）
        TextView btnTheme = findViewById(R.id.btn_theme_toggle);
        btnTheme.setText(prefsHelper.isDarkTheme() ? "🌙" : "☀");
        btnTheme.setOnClickListener(v -> {
            boolean isDark = prefsHelper.isDarkTheme();
            prefsHelper.setDarkTheme(!isDark);
            recreate();
        });

        // 设置按钮（锁屏通知等）
        findViewById(R.id.btn_settings).setOnClickListener(v -> showSettingsDialog());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AlarmsFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_alarms) {
                fragment = new AlarmsFragment();
            } else if (id == R.id.nav_sleep) {
                fragment = new SleepScheduleFragment();
            } else if (id == R.id.nav_stopwatch) {
                fragment = new StopwatchFragment();
            } else if (id == R.id.nav_timer) {
                fragment = new TimerFragment();
            } else {
                return false;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        });

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            } else {
                AlarmManagerHelper.setAllAlarms(this);
            }
        } else {
            AlarmManagerHelper.setAllAlarms(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AlarmService.isRinging) {
            Intent alertIntent = new Intent(this, AlarmAlertActivity.class);
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(alertIntent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (AlarmService.isRinging) {
            Intent alertIntent = new Intent(this, AlarmAlertActivity.class);
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(alertIntent);
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("设置");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(ThemeColors.getTextPrimary(this));
        tvTitle.setPadding(0, 0, 0, dpToPx(12));
        root.addView(tvTitle);

        // 锁屏通知开关行
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setCornerRadius(dpToPx(12));
        rowBg.setColor(ThemeColors.getSurfaceLight(this));
        row.setBackground(rowBg);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("锁屏通知");
        tvLabel.setTextSize(16);
        tvLabel.setTextColor(ThemeColors.getTextPrimary(this));
        textCol.addView(tvLabel);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("闹钟响铃时在锁屏显示");
        tvDesc.setTextSize(13);
        tvDesc.setTextColor(ThemeColors.getTextSecondary(this));
        textCol.addView(tvDesc);

        MaterialSwitch sw = new MaterialSwitch(this);
        sw.setChecked(prefsHelper.isLockScreenNotifyEnabled());
        sw.setOnCheckedChangeListener((btn, checked) -> prefsHelper.setLockScreenNotifyEnabled(checked));

        row.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(sw);
        root.addView(row);

        builder.setView(root);
        builder.setPositiveButton("关闭", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // 对话框内文字颜色跟随主题
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ThemeColors.getAccent(this));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
