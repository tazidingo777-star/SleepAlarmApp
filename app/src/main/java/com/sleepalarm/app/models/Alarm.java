package com.sleepalarm.app.models;

/**
 * 普通闹钟数据模型
 */
public class Alarm {
    private long id;
    private int hour;
    private int minute;
    private boolean enabled;
    private boolean[] repeatDays; // 0=周日...6=周六
    private String label;         // 顶部显示的时间标签/旧字段
    private String name;          // 闹钟名称
    private int gradualMinutes;   // 渐响时长(分钟)，默认5
    private boolean vibrate;
    private String ringtoneUri;   // 铃声 URI
    private String ringtoneName;  // 铃声显示名称
    private String vibrateMode;   // 振动模式显示名称
    private int snoozeInterval;   // 稍后提醒间隔（分钟）
    private int snoozeCount;      // 稍后提醒次数
    private boolean holidaySkip;  // 法定节假日不响铃

    public Alarm() {
        this.id = System.currentTimeMillis();
        this.hour = 7;
        this.minute = 0;
        this.enabled = true;
        this.repeatDays = new boolean[]{false, false, false, false, false, false, false};
        this.label = "闹钟";
        this.name = "";
        this.gradualMinutes = 5;
        this.vibrate = true;
        this.ringtoneUri = "";
        this.ringtoneName = "默认铃声";
        this.vibrateMode = "默认振动";
        this.snoozeInterval = 5;
        this.snoozeCount = 3;
        this.holidaySkip = false;
    }

    public Alarm(long id, int hour, int minute, boolean enabled, boolean[] repeatDays,
                 String label, String name, int gradualMinutes, boolean vibrate,
                 String ringtoneUri, String ringtoneName, String vibrateMode,
                 int snoozeInterval, int snoozeCount, boolean holidaySkip) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.repeatDays = repeatDays;
        this.label = label;
        this.name = name;
        this.gradualMinutes = gradualMinutes;
        this.vibrate = vibrate;
        this.ringtoneUri = ringtoneUri;
        this.ringtoneName = ringtoneName;
        this.vibrateMode = vibrateMode;
        this.snoozeInterval = snoozeInterval;
        this.snoozeCount = snoozeCount;
        this.holidaySkip = holidaySkip;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean[] getRepeatDays() { return repeatDays; }
    public void setRepeatDays(boolean[] repeatDays) { this.repeatDays = repeatDays; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getGradualMinutes() { return gradualMinutes; }
    public void setGradualMinutes(int gradualMinutes) { this.gradualMinutes = gradualMinutes; }

    public boolean isVibrate() { return vibrate; }
    public void setVibrate(boolean vibrate) { this.vibrate = vibrate; }

    public String getRingtoneUri() { return ringtoneUri; }
    public void setRingtoneUri(String ringtoneUri) { this.ringtoneUri = ringtoneUri; }

    public String getRingtoneName() { return ringtoneName; }
    public void setRingtoneName(String ringtoneName) { this.ringtoneName = ringtoneName; }

    public String getVibrateMode() { return vibrateMode; }
    public void setVibrateMode(String vibrateMode) { this.vibrateMode = vibrateMode; }

    public int getSnoozeInterval() { return snoozeInterval; }
    public void setSnoozeInterval(int snoozeInterval) { this.snoozeInterval = snoozeInterval; }

    public int getSnoozeCount() { return snoozeCount; }
    public void setSnoozeCount(int snoozeCount) { this.snoozeCount = snoozeCount; }

    public boolean isHolidaySkip() { return holidaySkip; }
    public void setHolidaySkip(boolean holidaySkip) { this.holidaySkip = holidaySkip; }

    public String getTimeText() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getRepeatDaysText() {
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean any = false;
        boolean all = true;
        for (int i = 0; i < 7; i++) {
            if (repeatDays[i]) any = true;
            else all = false;
        }
        if (!any) return "仅一次";
        if (all) return "每天";

        boolean weekdays = true, weekends = true;
        for (int i = 1; i <= 5; i++) if (!repeatDays[i]) weekdays = false;
        if (!repeatDays[0] || !repeatDays[6]) weekends = false;
        if (weekdays && !weekends) return "工作日";
        if (!weekdays && weekends) return "周末";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (repeatDays[i]) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(dayNames[i]);
            }
        }
        return sb.toString();
    }

    /**
     * 是否在未来7天内会响（有重复日设置或还未过期的单次闹钟）
     */
    public boolean isOneTime() {
        for (boolean d : repeatDays) if (d) return false;
        return true;
    }
}
