package com.sleepalarm.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.sleepalarm.app.AlarmReceiver;
import com.sleepalarm.app.models.Alarm;
import com.sleepalarm.app.models.SleepSchedule;

import java.util.Calendar;
import java.util.List;

public class AlarmManagerHelper {

    public static final String EXTRA_ALARM_ID = "alarm_id";
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_IS_BEDTIME = "is_bedtime";
    public static final String EXTRA_IS_REGULAR = "is_regular";
    public static final String EXTRA_WAKE_HOUR = "wake_hour";
    public static final String EXTRA_WAKE_MINUTE = "wake_minute";
    public static final String EXTRA_BEDTIME_HOUR = "bedtime_hour";
    public static final String EXTRA_BEDTIME_MINUTE = "bedtime_minute";
    public static final String EXTRA_GRADUAL_MINUTES = "gradual_minutes";

    // requestCode 范围: 0-99999 = 普通闹钟, 100000-199999 = 睡眠起床, 200000-299999 = 睡眠就寝
    private static final int REGULAR_BASE = 0;
    private static final int SLEEP_WAKE_BASE = 100000;
    private static final int SLEEP_BED_BASE = 200000;

    // ==================== 普通闹钟 ====================

    public static void setAlarm(Context context, Alarm alarm) {
        cancelAlarm(context, alarm);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        boolean[] days = alarm.getRepeatDays();
        boolean anyDay = false;
        for (boolean d : days) if (d) anyDay = true;

        if (!anyDay) {
            // 单次闹钟
            setOneTimeAlarm(context, am, alarm, -1);
        } else {
            for (int i = 0; i < 7; i++) {
                if (days[i]) setOneTimeAlarm(context, am, alarm, i);
            }
        }
    }

    private static void setOneTimeAlarm(Context context, AlarmManager am, Alarm alarm, int dayIndex) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        cal.set(Calendar.MINUTE, alarm.getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (dayIndex >= 0) {
            int targetDay = dayIndex + 1; // Calendar.SUNDAY=1
            int currentDay = cal.get(Calendar.DAY_OF_WEEK);
            int daysUntil = (targetDay - currentDay + 7) % 7;
            if (daysUntil == 0) {
                int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                        + Calendar.getInstance().get(Calendar.MINUTE);
                int target = alarm.getHour() * 60 + alarm.getMinute();
                if (now >= target) daysUntil = 7;
            }
            cal.add(Calendar.DAY_OF_YEAR, daysUntil);
        } else {
            // 单次闹钟，如果时间已过就设到明天
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_IS_REGULAR, true);
        intent.putExtra(EXTRA_ALARM_ID, alarm.getId());
        intent.putExtra(EXTRA_WAKE_HOUR, alarm.getHour());
        intent.putExtra(EXTRA_WAKE_MINUTE, alarm.getMinute());
        intent.putExtra(EXTRA_GRADUAL_MINUTES, alarm.getGradualMinutes());
        intent.putExtra(EXTRA_IS_BEDTIME, false);

        int reqCode = REGULAR_BASE + (int) (alarm.getId() % 50000) + (dayIndex >= 0 ? dayIndex : 7);
        PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    public static void cancelAlarm(Context context, Alarm alarm) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        for (int i = 0; i <= 7; i++) { // 0-6 for days, 7 for one-time
            Intent intent = new Intent(context, AlarmReceiver.class);
            int reqCode = REGULAR_BASE + (int) (alarm.getId() % 50000) + i;
            PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    // ==================== 睡眠闹钟 ====================

    public static void setSleepAlarm(Context context, SleepSchedule schedule) {
        cancelSleepAlarm(context, schedule);
        // 起床闹钟
        setSleepSingle(context, schedule, SLEEP_WAKE_BASE, schedule.getWakeHour(), schedule.getWakeMinute());
        // 就寝提醒
        setSleepSingle(context, schedule, SLEEP_BED_BASE, schedule.getBedtimeHour(), schedule.getBedtimeMinute());
    }

    private static void setSleepSingle(Context context, SleepSchedule schedule, int base,
                                       int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        boolean isBedtime = (base == SLEEP_BED_BASE);
        boolean[] days = schedule.getRepeatDays();

        for (int i = 0; i < 7; i++) {
            if (!days[i]) continue;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            int targetDay = i + 1;
            int currentDay = cal.get(Calendar.DAY_OF_WEEK);
            int daysUntil = (targetDay - currentDay + 7) % 7;
            if (daysUntil == 0) {
                int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                        + Calendar.getInstance().get(Calendar.MINUTE);
                if (now >= hour * 60 + minute) daysUntil = 7;
            }
            cal.add(Calendar.DAY_OF_YEAR, daysUntil);

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(EXTRA_IS_REGULAR, false);
            intent.putExtra(EXTRA_SCHEDULE_ID, schedule.getId());
            intent.putExtra(EXTRA_IS_BEDTIME, isBedtime);
            intent.putExtra(EXTRA_WAKE_HOUR, schedule.getWakeHour());
            intent.putExtra(EXTRA_WAKE_MINUTE, schedule.getWakeMinute());
            intent.putExtra(EXTRA_BEDTIME_HOUR, schedule.getBedtimeHour());
            intent.putExtra(EXTRA_BEDTIME_MINUTE, schedule.getBedtimeMinute());
            intent.putExtra(EXTRA_GRADUAL_MINUTES, schedule.getGradualMinutes());

            int reqCode = base + (int) (schedule.getId() % 50000) + i;
            PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }

    public static void cancelSleepAlarm(Context context, SleepSchedule schedule) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        for (int base : new int[]{SLEEP_WAKE_BASE, SLEEP_BED_BASE}) {
            for (int i = 0; i < 7; i++) {
                Intent intent = new Intent(context, AlarmReceiver.class);
                int reqCode = base + (int) (schedule.getId() % 50000) + i;
                PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) {
                    am.cancel(pi);
                    pi.cancel();
                }
            }
        }
    }

    // ==================== 全部 ====================

    public static void setAllAlarms(Context context) {
        PreferencesHelper ph = new PreferencesHelper(context);

        // 普通闹钟
        for (Alarm a : ph.loadAlarms()) {
            if (a.isEnabled()) setAlarm(context, a);
        }

        // 睡眠闹钟
        for (SleepSchedule s : ph.loadSchedules()) {
            if (s.isEnabled()) setSleepAlarm(context, s);
        }
    }
}
