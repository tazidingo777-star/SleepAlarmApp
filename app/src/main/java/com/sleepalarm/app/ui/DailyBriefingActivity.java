package com.sleepalarm.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sleepalarm.app.R;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * 每日播报界面 - 类似小米的每日播报功能
 * 显示日期、天气信息、睡眠统计和励志语录
 */
public class DailyBriefingActivity extends AppCompatActivity {

    private TextView tvDate, tvDayOfWeek, tvLunarInfo;
    private TextView tvGreeting;
    private TextView tvSleepSummary;
    private TextView tvWeather, tvTemperature, tvWeatherDesc;
    private TextView tvQuote;
    private TextView tvRefreshTime;
    private Handler handler;
    private PreferencesHelper prefsHelper;

    // 励志语录库
    private static final String[] QUOTES = {
            "每一个清晨都是新的开始，把握今天，成就未来。",
            "早起的鸟儿有虫吃，勤奋的人儿有收获。",
            "生活不会辜负每一个努力的人，加油！",
            "今天的你，一定会感谢昨天努力的自己。",
            "阳光正好，微风不燥，又是美好的一天。",
            "成功不是将来才有的，而是从决定去做的那一刻开始。",
            "世界上最可贵的两个词：一个叫认真，一个叫坚持。",
            "你起不来的早晨，有人能起来；你吃不了的苦，有人能吃。",
            "不要等待机会，而要创造机会。",
            "每一天都是生命中最年轻的一天，珍惜当下。",
            "人生没有彩排，每天都是现场直播。",
            "梦想不会逃跑，逃跑的永远是自己。",
            "你今天受的苦，吃的亏，担的责，扛的罪，忍的痛，到最后都会变成光，照亮你的路。",
            "没有太晚的开始，不如就从今天行动。",
            "你若盛开，蝴蝶自来；你若精彩，天自安排。"
    };

    // 天气状况描述
    private static final String[] WEATHER_CONDITIONS = {
            "晴", "多云", "阴", "小雨", "晴间多云", "微风"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_briefing);

        prefsHelper = new PreferencesHelper(this);
        handler = new Handler(Looper.getMainLooper());

        initViews();
        loadBriefingData();

        // 每分钟刷新一次时间
        startTimeRefresh();
    }

    private void initViews() {
        tvDate = findViewById(R.id.tv_date);
        tvDayOfWeek = findViewById(R.id.tv_day_of_week);
        tvLunarInfo = findViewById(R.id.tv_lunar_info);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvSleepSummary = findViewById(R.id.tv_sleep_summary);
        tvWeather = findViewById(R.id.tv_weather);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvWeatherDesc = findViewById(R.id.tv_weather_desc);
        tvQuote = findViewById(R.id.tv_quote);
        tvRefreshTime = findViewById(R.id.tv_refresh_time);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadBriefingData() {
        Calendar calendar = Calendar.getInstance();

        // 日期信息
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE);
        tvDate.setText(dateFormat.format(calendar.getTime()));

        // 星期
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0=周日
        tvDayOfWeek.setText(weekDays[dayOfWeek]);

        // 农历信息（简化版：显示干支纪年近似值）
        tvLunarInfo.setText(getSimpleLunarInfo(calendar));

        // 根据时间显示问候语
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 6) greeting = "凌晨好";
        else if (hour < 9) greeting = "早上好";
        else if (hour < 12) greeting = "上午好";
        else if (hour < 14) greeting = "中午好";
        else if (hour < 18) greeting = "下午好";
        else if (hour < 22) greeting = "晚上好";
        else greeting = "夜深了";

        tvGreeting.setText(greeting + "！");

        // 睡眠总结
        SleepSchedule schedule = prefsHelper.getActiveSchedule();
        if (schedule != null) {
            String sleepInfo = "昨晚就寝时间: " + schedule.getBedtimeText() + "\n"
                    + "计划睡眠时长: " + schedule.getSleepDurationText() + "\n"
                    + "今日起床时间: " + schedule.getWakeTimeText();
            tvSleepSummary.setText(sleepInfo);
        }

        // 模拟天气信息（实际应用可接入天气API）
        Random random = new Random();
        int temp = 22 + random.nextInt(15); // 22-36度
        String condition = WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
        tvTemperature.setText(temp + "°");
        tvWeather.setText(condition);
        tvWeatherDesc.setText(getWeatherAdvice(condition, temp));

        // 随机语录
        tvQuote.setText("「" + QUOTES[random.nextInt(QUOTES.length)] + "」");

        // 刷新时间
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        tvRefreshTime.setText("更新时间: " + timeFormat.format(new Date()));

        // 记录今日已查看播报
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        prefsHelper.setLastBriefingDate(dayFormat.format(new Date()));
    }

    /**
     * 简单农历信息（农历年份近似计算）
     */
    private String getSimpleLunarInfo(Calendar calendar) {
        // 简化的天干地支计算
        String[] tianGan = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
        String[] diZhi = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
        String[] shengXiao = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};

        int year = calendar.get(Calendar.YEAR);
        int tgIndex = (year - 4) % 10;
        int dzIndex = (year - 4) % 12;

        return tianGan[tgIndex] + diZhi[dzIndex] + "年 · 属" + shengXiao[dzIndex];
    }

    /**
     * 根据天气给出建议
     */
    private String getWeatherAdvice(String condition, int temp) {
        if (temp > 33) return "天气炎热，注意防暑降温";
        if (temp < 10) return "天气寒冷，注意保暖";
        if (condition.contains("雨")) return "今天有雨，出门记得带伞";
        if (condition.equals("晴")) return "阳光明媚，适合户外活动";
        return "今天天气不错，祝你心情愉快";
    }

    /**
     * 定时刷新时间显示
     */
    private void startTimeRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                tvRefreshTime.setText("更新时间: " + timeFormat.format(new Date()));
                handler.postDelayed(this, 60000); // 每分钟刷新
            }
        }, 60000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
