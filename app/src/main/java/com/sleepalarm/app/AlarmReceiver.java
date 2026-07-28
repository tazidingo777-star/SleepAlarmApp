package com.sleepalarm.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.sleepalarm.app.utils.AlarmManagerHelper;

/**
 * 闹钟广播接收器 - 在设定的时间触发闹钟
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "闹钟触发!");

        long scheduleId = intent.getLongExtra(AlarmManagerHelper.EXTRA_SCHEDULE_ID, -1);
        boolean isBedtime = intent.getBooleanExtra(AlarmManagerHelper.EXTRA_IS_BEDTIME, false);
        int wakeHour = intent.getIntExtra(AlarmManagerHelper.EXTRA_WAKE_HOUR, 7);
        int wakeMinute = intent.getIntExtra(AlarmManagerHelper.EXTRA_WAKE_MINUTE, 0);
        int bedtimeHour = intent.getIntExtra(AlarmManagerHelper.EXTRA_BEDTIME_HOUR, 23);
        int bedtimeMinute = intent.getIntExtra(AlarmManagerHelper.EXTRA_BEDTIME_MINUTE, 0);
        int gradualMinutes = intent.getIntExtra(AlarmManagerHelper.EXTRA_GRADUAL_MINUTES, 10);

        // 启动闹钟前台服务
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_SCHEDULE_ID, scheduleId);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_IS_BEDTIME, isBedtime);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_WAKE_HOUR, wakeHour);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_WAKE_MINUTE, wakeMinute);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_BEDTIME_HOUR, bedtimeHour);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_BEDTIME_MINUTE, bedtimeMinute);
        serviceIntent.putExtra(AlarmManagerHelper.EXTRA_GRADUAL_MINUTES, gradualMinutes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
