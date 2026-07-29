package com.sleepalarm.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sleepalarm.app.ui.AlarmsFragment;
import com.sleepalarm.app.ui.AlarmAlertActivity;
import com.sleepalarm.app.ui.SleepScheduleFragment;
import com.sleepalarm.app.ui.StopwatchFragment;
import com.sleepalarm.app.ui.TimerFragment;
import com.sleepalarm.app.utils.AlarmManagerHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> notificationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { /* 权限结果处理 */ });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // 默认显示闹钟页
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
        // 如果闹钟正在响铃，直接跳转到闹钟关闭界面
        if (AlarmService.isRinging) {
            Intent alertIntent = new Intent(this, AlarmAlertActivity.class);
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(alertIntent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 当从通知栏点击进入时也检查闹钟状态
        if (AlarmService.isRinging) {
            Intent alertIntent = new Intent(this, AlarmAlertActivity.class);
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(alertIntent);
        }
    }
}
