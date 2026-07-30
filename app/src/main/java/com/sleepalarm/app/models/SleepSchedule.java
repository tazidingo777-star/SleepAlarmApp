package com.sleepalarm.app.models;

/**
 * 就寝/起床时间安排数据模型
 */
public class SleepSchedule {
    private long id;
    private int bedtimeHour;      // 就寝时间 - 小时 (0-23)
    private int bedtimeMinute;    // 就寝时间 - 分钟 (0-59)
    private int wakeHour;         // 起床时间 - 小时 (0-23)
    private int wakeMinute;       // 起床时间 - 分钟 (0-59)
    private boolean enabled;      // 是否启用
    private boolean[] repeatDays; // 重复日, 索引0=周日, 1=周一...6=周六
    private int gradualMinutes;   // 渐响时长（分钟）, 默认10
    private String ringtoneUri;   // 起床铃声 URI
    private String ringtoneName;  // 起床铃声显示名称
    private boolean vibrate;      // 是否振动
    private String vibrateMode;   // 振动模式

    public SleepSchedule() {
        this.id = System.currentTimeMillis();
        this.bedtimeHour = 23;
        this.bedtimeMinute = 0;
        this.wakeHour = 7;
        this.wakeMinute = 0;
        this.enabled = true;
        this.repeatDays = new boolean[]{true, true, true, true, true, true, true};
        this.gradualMinutes = 10;
        this.ringtoneUri = "";
        this.ringtoneName = "默认铃声";
        this.vibrate = true;
        this.vibrateMode = "默认振动";
    }

    public SleepSchedule(long id, int bedtimeHour, int bedtimeMinute, int wakeHour, int wakeMinute,
                         boolean enabled, boolean[] repeatDays, int gradualMinutes,
                         String ringtoneUri, String ringtoneName, boolean vibrate, String vibrateMode) {
        this.id = id;
        this.bedtimeHour = bedtimeHour;
        this.bedtimeMinute = bedtimeMinute;
        this.wakeHour = wakeHour;
        this.wakeMinute = wakeMinute;
        this.enabled = enabled;
        this.repeatDays = repeatDays;
        this.gradualMinutes = gradualMinutes;
        this.ringtoneUri = ringtoneUri;
        this.ringtoneName = ringtoneName;
        this.vibrate = vibrate;
        this.vibrateMode = vibrateMode;
    }

    // ===== Getters and Setters =====

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getBedtimeHour() { return bedtimeHour; }
    public void setBedtimeHour(int bedtimeHour) { this.bedtimeHour = bedtimeHour; }

    public int getBedtimeMinute() { return bedtimeMinute; }
    public void setBedtimeMinute(int bedtimeMinute) { this.bedtimeMinute = bedtimeMinute; }

    public int getWakeHour() { return wakeHour; }
    public void setWakeHour(int wakeHour) { this.wakeHour = wakeHour; }

    public int getWakeMinute() { return wakeMinute; }
    public void setWakeMinute(int wakeMinute) { this.wakeMinute = wakeMinute; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean[] getRepeatDays() { return repeatDays; }
    public void setRepeatDays(boolean[] repeatDays) { this.repeatDays = repeatDays; }

    public int getGradualMinutes() { return gradualMinutes; }
    public void setGradualMinutes(int gradualMinutes) { this.gradualMinutes = gradualMinutes; }

    public String getRingtoneUri() { return ringtoneUri; }
    public void setRingtoneUri(String ringtoneUri) { this.ringtoneUri = ringtoneUri; }

    public String getRingtoneName() { return ringtoneName; }
    public void setRingtoneName(String ringtoneName) { this.ringtoneName = ringtoneName; }

    public boolean isVibrate() { return vibrate; }
    public void setVibrate(boolean vibrate) { this.vibrate = vibrate; }

    public String getVibrateMode() { return vibrateMode; }
    public void setVibrateMode(String vibrateMode) { this.vibrateMode = vibrateMode; }

    // ===== Utility Methods =====

    /**
     * 获取睡眠时长（分钟）
     */
    public int getSleepDurationMinutes() {
        int bedTotal = bedtimeHour * 60 + bedtimeMinute;
        int wakeTotal = wakeHour * 60 + wakeMinute;
        if (wakeTotal >= bedTotal) {
            return wakeTotal - bedTotal;
        } else {
            // 跨天情况（例如：23:00就寝，7:00起床）
            return (24 * 60 - bedTotal) + wakeTotal;
        }
    }

    /**
     * 格式化睡眠时长
     */
    public String getSleepDurationText() {
        int minutes = getSleepDurationMinutes();
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0 && mins > 0) {
            return hours + "小时" + mins + "分钟";
        } else if (hours > 0) {
            return hours + "小时";
        } else {
            return mins + "分钟";
        }
    }

    /**
     * 格式化时间字符串（例如 "23:00"）
     */
    public static String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getBedtimeText() {
        return formatTime(bedtimeHour, bedtimeMinute);
    }

    public String getWakeTimeText() {
        return formatTime(wakeHour, wakeMinute);
    }

    /**
     * 获取星期几的文本
     */
    public String getRepeatDaysText() {
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        StringBuilder sb = new StringBuilder();
        boolean everyDay = true;
        boolean weekdays = true;
        boolean weekends = true;

        for (int i = 0; i < 7; i++) {
            if (!repeatDays[i]) everyDay = false;
            if (i >= 1 && i <= 5 && !repeatDays[i]) weekdays = false;
            if ((i == 0 || i == 6) && !repeatDays[i]) weekends = false;
        }

        if (everyDay) return "每天";
        if (weekdays && !weekends) return "工作日";
        if (!weekdays && weekends) return "周末";

        for (int i = 0; i < 7; i++) {
            if (repeatDays[i]) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(dayNames[i]);
            }
        }
        return sb.toString();
    }
}
