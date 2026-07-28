package com.sleepalarm.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.sleepalarm.app.models.SleepSchedule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SharedPreferences 工具类 - 管理就寝/起床数据的存储和读取
 */
public class PreferencesHelper {

    private static final String PREF_NAME = "sleep_alarm_prefs";
    private static final String KEY_SCHEDULES = "schedules";
    private static final String KEY_LAST_BRIEFING_DATE = "last_briefing_date";
    private static final String KEY_BRIEFING_ENABLED = "briefing_enabled";

    private final SharedPreferences prefs;

    public PreferencesHelper(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存就寝/起床时间安排列表
     */
    public void saveSchedules(List<SleepSchedule> schedules) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SleepSchedule schedule : schedules) {
                jsonArray.put(scheduleToJson(schedule));
            }
            prefs.edit().putString(KEY_SCHEDULES, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取所有就寝/起床时间安排
     */
    public List<SleepSchedule> loadSchedules() {
        List<SleepSchedule> schedules = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_SCHEDULES, null);
            if (json != null) {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    schedules.add(jsonToSchedule(jsonArray.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如果没有保存的数据，返回默认安排
        if (schedules.isEmpty()) {
            schedules.add(new SleepSchedule());
        }
        return schedules;
    }

    /**
     * 获取当前激活的就寝/起床安排（取第一个启用的）
     */
    public SleepSchedule getActiveSchedule() {
        List<SleepSchedule> schedules = loadSchedules();
        for (SleepSchedule schedule : schedules) {
            if (schedule.isEnabled()) {
                return schedule;
            }
        }
        return schedules.isEmpty() ? new SleepSchedule() : schedules.get(0);
    }

    // ===== 每日播报相关 =====

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

    // ===== JSON 转换 =====

    private JSONObject scheduleToJson(SleepSchedule schedule) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("id", schedule.getId());
        obj.put("bedtimeHour", schedule.getBedtimeHour());
        obj.put("bedtimeMinute", schedule.getBedtimeMinute());
        obj.put("wakeHour", schedule.getWakeHour());
        obj.put("wakeMinute", schedule.getWakeMinute());
        obj.put("enabled", schedule.isEnabled());
        obj.put("gradualMinutes", schedule.getGradualMinutes());

        JSONArray days = new JSONArray();
        for (boolean day : schedule.getRepeatDays()) {
            days.put(day);
        }
        obj.put("repeatDays", days);
        return obj;
    }

    private SleepSchedule jsonToSchedule(JSONObject obj) throws Exception {
        long id = obj.getLong("id");
        int bedtimeHour = obj.getInt("bedtimeHour");
        int bedtimeMinute = obj.getInt("bedtimeMinute");
        int wakeHour = obj.getInt("wakeHour");
        int wakeMinute = obj.getInt("wakeMinute");
        boolean enabled = obj.getBoolean("enabled");
        int gradualMinutes = obj.optInt("gradualMinutes", 10);

        boolean[] repeatDays = new boolean[7];
        JSONArray days = obj.getJSONArray("repeatDays");
        for (int i = 0; i < days.length() && i < 7; i++) {
            repeatDays[i] = days.getBoolean(i);
        }

        return new SleepSchedule(id, bedtimeHour, bedtimeMinute, wakeHour, wakeMinute,
                enabled, repeatDays, gradualMinutes);
    }
}
