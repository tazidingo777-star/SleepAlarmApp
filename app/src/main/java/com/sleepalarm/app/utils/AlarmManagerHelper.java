package com.sleepalarm.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.sleepalarm.app.AlarmReceiver;
import com.sleepalarm.app.models.SleepSchedule;

import java.util.Calendar;
import java.util.List;

/**
 * AlarmManager 工具类 - 管理系统闹钟的设定和取消
 */
public class AlarmManagerHelper {

    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_BEDTIME_HOUR = "bedtime_hour";
    public static final String EXTRA_BEDTIME_MINUTE = "bedtime_minute";
    public static final String EXTRA_WAKE_HOUR = "wake_hour";
    public static final String EXTRA_WAKE_MINUTE = "wake_minute";
    public static final String EXTRA_GRADUAL_MINUTES = "gradual_minutes";
    public static final String EXTRA_IS_BEDTIME = "is_bedtime";

    /**
     * 为所有启用的就寝/起床安排设置闹钟
     */
    public static void setAllAlarms(Context context) {
        PreferencesHelper prefsHelper = new PreferencesHelper(context);
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        for (SleepSchedule schedule : schedules) {
            if (schedule.isEnabled()) {
                setAlarmForSchedule(context, schedule);
            }
        }
    }

    /**
     * 为单个就寝/起床安排设置闹钟（起床闹钟和就寝提醒）
     */
    public static void setAlarmForSchedule(Context context, SleepSchedule schedule) {
        // 设置起床闹钟
        setSingleAlarm(context, schedule, false);
        // 设置就寝提醒
        setSingleAlarm(context, schedule, true);
    }

    /**
     * 设置单个闹钟
     * @param isBedtime true=就寝提醒, false=起床闹钟
     */
    private static void setSingleAlarm(Context context, SleepSchedule schedule, boolean isBedtime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        boolean[] repeatDays = schedule.getRepeatDays();
        int hour = isBedtime ? schedule.getBedtimeHour() : schedule.getWakeHour();
        int minute = isBedtime ? schedule.getBedtimeMinute() : schedule.getWakeMinute();

        // 为每个重复日设置独立的闹钟
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            if (!repeatDays[dayIndex]) continue;

            Calendar calendar = Calendar.getInstance();
            // Calendar.SUNDAY=1, 我们的索引0=周日
            int calDay = dayIndex + 1; // 转换为Calendar的星期几
            int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // 计算距离目标日还有多少天
            int daysUntilTarget = (calDay - currentDay + 7) % 7;
            if (daysUntilTarget == 0) {
                // 同一天，检查时间是否已过
                int targetTotal = hour * 60 + minute;
                int currentTotal = currentHour * 60 + currentMinute;
                if (currentTotal >= targetTotal) {
                    daysUntilTarget = 7; // 今天已过，设为下周
                }
            }

            calendar.add(Calendar.DAY_OF_YEAR, daysUntilTarget);

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(EXTRA_SCHEDULE_ID, schedule.getId());
            intent.putExtra(EXTRA_IS_BEDTIME, isBedtime);
            intent.putExtra(EXTRA_WAKE_HOUR, schedule.getWakeHour());
            intent.putExtra(EXTRA_WAKE_MINUTE, schedule.getWakeMinute());
            intent.putExtra(EXTRA_BEDTIME_HOUR, schedule.getBedtimeHour());
            intent.putExtra(EXTRA_BEDTIME_MINUTE, schedule.getBedtimeMinute());
            intent.putExtra(EXTRA_GRADUAL_MINUTES, schedule.getGradualMinutes());

            // 使用唯一的 requestCode
            int requestCode = generateRequestCode(schedule.getId(), isBedtime, dayIndex);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    /**
     * 取消指定就寝/起床安排的所有闹钟
     */
    public static void cancelAlarmsForSchedule(Context context, SleepSchedule schedule) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (boolean isBedtime : new boolean[]{false, true}) {
            for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
                Intent intent = new Intent(context, AlarmReceiver.class);
                int requestCode = generateRequestCode(schedule.getId(), isBedtime, dayIndex);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, requestCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
    }

    /**
     * 取消所有闹钟
     */
    public static void cancelAllAlarms(Context context) {
        PreferencesHelper prefsHelper = new PreferencesHelper(context);
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        for (SleepSchedule schedule : schedules) {
            cancelAlarmsForSchedule(context, schedule);
        }
    }

    /**
     * 生成唯一的 requestCode
     * 使用 schedule ID 的 hashCode 加上偏移量
     */
    private static int generateRequestCode(long scheduleId, boolean isBedtime, int dayIndex) {
        int base = (int) (scheduleId % 100000);
        int offset = isBedtime ? 100000 : 0;
        return base + offset + dayIndex;
    }
}
