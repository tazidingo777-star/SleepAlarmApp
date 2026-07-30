package com.sleepalarm.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.sleepalarm.app.models.Alarm;
import com.sleepalarm.app.models.SleepSchedule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SharedPreferences 工具类 - 管理闹钟和就寝数据的存储和读取
 */
public class PreferencesHelper {

    private static final String PREF_NAME = "sleep_alarm_prefs";
    private static final String KEY_SCHEDULES = "schedules";
    private static final String KEY_ALARMS = "alarms";
    private static final String KEY_LAST_BRIEFING_DATE = "last_briefing_date";
    private static final String KEY_BRIEFING_ENABLED = "briefing_enabled";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_LOCK_SCREEN_NOTIFY = "lock_screen_notify";

    private final SharedPreferences prefs;

    public PreferencesHelper(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==================== 普通闹钟 ====================

    public List<Alarm> loadAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_ALARMS, null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    alarms.add(jsonToAlarm(arr.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alarms;
    }

    public void saveAlarms(List<Alarm> alarms) {
        try {
            JSONArray arr = new JSONArray();
            for (Alarm a : alarms) arr.put(alarmToJson(a));
            prefs.edit().putString(KEY_ALARMS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAlarm(Alarm alarm) {
        List<Alarm> alarms = loadAlarms();
        boolean found = false;
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).getId() == alarm.getId()) {
                alarms.set(i, alarm);
                found = true;
                break;
            }
        }
        if (!found) alarms.add(alarm);
        saveAlarms(alarms);
    }

    public void deleteAlarm(long alarmId) {
        List<Alarm> alarms = loadAlarms();
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).getId() == alarmId) {
                alarms.remove(i);
                break;
            }
        }
        saveAlarms(alarms);
    }

    // ==================== 睡眠闹钟 ====================

    public void saveSchedules(List<SleepSchedule> schedules) {
        try {
            JSONArray arr = new JSONArray();
            for (SleepSchedule s : schedules) arr.put(scheduleToJson(s));
            prefs.edit().putString(KEY_SCHEDULES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SleepSchedule> loadSchedules() {
        List<SleepSchedule> schedules = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_SCHEDULES, null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    schedules.add(jsonToSchedule(arr.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (schedules.isEmpty()) {
            schedules.add(new SleepSchedule());
        }
        return schedules;
    }

    public SleepSchedule getActiveSchedule() {
        for (SleepSchedule s : loadSchedules()) {
            if (s.isEnabled()) return s;
        }
        SleepSchedule s = new SleepSchedule();
        return s;
    }

    // ==================== 每日播报 ====================

    public boolean isBriefingEnabled() {
        return prefs.getBoolean(KEY_BRIEFING_ENABLED, true);
    }

    public void setBriefingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BRIEFING_ENABLED, enabled).apply();
    }

    public String getLastBriefingDate() {
        return prefs.getString(KEY_LAST_BRIEFING_DATE, "");
    }

    public void setLastBriefingDate(String date) {
        prefs.edit().putString(KEY_LAST_BRIEFING_DATE, date).apply();
    }

    // ==================== 主题与通知 ====================

    public boolean isDarkTheme() {
        return prefs.getBoolean(KEY_DARK_THEME, true);
    }

    public void setDarkTheme(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply();
    }

    public boolean isLockScreenNotifyEnabled() {
        return prefs.getBoolean(KEY_LOCK_SCREEN_NOTIFY, true);
    }

    public void setLockScreenNotifyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCK_SCREEN_NOTIFY, enabled).apply();
    }

    // ==================== JSON 转换 ====================

    private JSONObject alarmToJson(Alarm a) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", a.getId());
        o.put("hour", a.getHour());
        o.put("minute", a.getMinute());
        o.put("enabled", a.isEnabled());
        o.put("label", a.getLabel());
        o.put("name", a.getName());
        o.put("gradualMinutes", a.getGradualMinutes());
        o.put("vibrate", a.isVibrate());
        o.put("ringtoneUri", a.getRingtoneUri());
        o.put("ringtoneName", a.getRingtoneName());
        o.put("vibrateMode", a.getVibrateMode());
        o.put("snoozeInterval", a.getSnoozeInterval());
        o.put("snoozeCount", a.getSnoozeCount());
        o.put("holidaySkip", a.isHolidaySkip());
        JSONArray days = new JSONArray();
        for (boolean d : a.getRepeatDays()) days.put(d);
        o.put("repeatDays", days);
        return o;
    }

    private Alarm jsonToAlarm(JSONObject o) throws Exception {
        long id = o.getLong("id");
        int hour = o.getInt("hour");
        int minute = o.getInt("minute");
        boolean enabled = o.getBoolean("enabled");
        String label = o.optString("label", "闹钟");
        String name = o.optString("name", "");
        int gm = o.optInt("gradualMinutes", 5);
        boolean vib = o.optBoolean("vibrate", true);
        String ringtoneUri = o.optString("ringtoneUri", "");
        String ringtoneName = o.optString("ringtoneName", "默认铃声");
        String vibrateMode = o.optString("vibrateMode", "默认振动");
        int snoozeInterval = o.optInt("snoozeInterval", 5);
        int snoozeCount = o.optInt("snoozeCount", 3);
        boolean holidaySkip = o.optBoolean("holidaySkip", false);
        boolean[] days = new boolean[7];
        JSONArray arr = o.getJSONArray("repeatDays");
        for (int i = 0; i < arr.length() && i < 7; i++) days[i] = arr.getBoolean(i);
        return new Alarm(id, hour, minute, enabled, days, label, name, gm, vib,
                ringtoneUri, ringtoneName, vibrateMode, snoozeInterval, snoozeCount, holidaySkip);
    }

    private JSONObject scheduleToJson(SleepSchedule s) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", s.getId());
        o.put("bedtimeHour", s.getBedtimeHour());
        o.put("bedtimeMinute", s.getBedtimeMinute());
        o.put("wakeHour", s.getWakeHour());
        o.put("wakeMinute", s.getWakeMinute());
        o.put("enabled", s.isEnabled());
        o.put("gradualMinutes", s.getGradualMinutes());
        JSONArray days = new JSONArray();
        for (boolean d : s.getRepeatDays()) days.put(d);
        o.put("repeatDays", days);
        return o;
    }

    private SleepSchedule jsonToSchedule(JSONObject o) throws Exception {
        long id = o.getLong("id");
        int bh = o.getInt("bedtimeHour"), bm = o.getInt("bedtimeMinute");
        int wh = o.getInt("wakeHour"), wm = o.getInt("wakeMinute");
        boolean en = o.getBoolean("enabled");
        int gm = o.optInt("gradualMinutes", 10);
        boolean[] days = new boolean[7];
        JSONArray arr = o.getJSONArray("repeatDays");
        for (int i = 0; i < arr.length() && i < 7; i++) days[i] = arr.getBoolean(i);
        return new SleepSchedule(id, bh, bm, wh, wm, en, days, gm);
    }
}
