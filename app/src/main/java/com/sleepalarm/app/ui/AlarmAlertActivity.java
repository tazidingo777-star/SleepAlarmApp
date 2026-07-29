package com.sleepalarm.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sleepalarm.app.AlarmService;
import com.sleepalarm.app.R;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/**
 * 闹钟响铃全屏界面 - 滑动关闭 + 按钮关闭 + 起床语音播报
 */
public class AlarmAlertActivity extends AppCompatActivity {

    private static final String TAG = "AlarmAlertActivity";

    private TextView tvAlarmTitle;
    private TextView tvAlarmTime;
    private TextView tvAlarmSubtitle;
    private View slideContainer;
    private View slideThumb;
    private View slideTrackBg;
    private TextView tvSlideHint;
    private AlarmService alarmService;
    private boolean isBedtime;
    private boolean isBound = false;
    private boolean alarmDismissed = false;

    // 滑动关闭
    private float slideStartX;
    private float maxSlideDistance;
    private float thumbStartX;
    private boolean isSliding = false;
    private boolean slideCompleted = false;

    // TTS
    private TextToSpeech tts;
    private boolean ttsInitialized = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 励志语录库
    private static final String[] QUOTES = {
            "每一个清晨都是新的开始，把握今天，成就未来。",
            "早起的鸟儿有虫吃，勤奋的人儿有收获。",
            "生活不会辜负每一个努力的人，加油！",
            "今天的你，一定会感谢昨天努力的自己。",
            "阳光正好，微风不燥，又是美好的一天。",
            "成功不是将来才有的，而是从决定去做的那一刻开始。",
            "世界上最可贵的两个词：一个叫认真，一个叫坚持。",
            "不要等待机会，而要创造机会。",
            "每一天都是生命中最年轻的一天，珍惜当下。",
            "梦想不会逃跑，逃跑的永远是自己。"
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            if (alarmDismissed) {
                try { AlarmAlertActivity.this.unbindService(this); } catch (Exception ignored) {}
                return;
            }
            AlarmService.LocalBinder binder = (AlarmService.LocalBinder) service;
            alarmService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            isBound = false;
            alarmService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_alert);

        // 保持屏幕常亮，在锁屏上显示
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        Intent intent = getIntent();
        isBedtime = intent.getBooleanExtra(AlarmManagerHelper.EXTRA_IS_BEDTIME, false);
        int hour = isBedtime ? intent.getIntExtra(AlarmManagerHelper.EXTRA_BEDTIME_HOUR, 23)
                : intent.getIntExtra(AlarmManagerHelper.EXTRA_WAKE_HOUR, 7);
        int minute = isBedtime ? intent.getIntExtra(AlarmManagerHelper.EXTRA_BEDTIME_MINUTE, 0)
                : intent.getIntExtra(AlarmManagerHelper.EXTRA_WAKE_MINUTE, 0);

        initViews(hour, minute);
        setupListeners();
        setupSlideToDismiss();

        // 绑定服务
        bindService(new Intent(this, AlarmService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        // 起床闹钟时初始化TTS并播报
        if (!isBedtime) {
            initTTS();
        }
    }

    private void initViews(int hour, int minute) {
        tvAlarmTitle = findViewById(R.id.tv_alarm_title);
        tvAlarmTime = findViewById(R.id.tv_alarm_time);
        tvAlarmSubtitle = findViewById(R.id.tv_alarm_subtitle);
        slideContainer = findViewById(R.id.slide_container);
        slideThumb = findViewById(R.id.slide_thumb);
        slideTrackBg = findViewById(R.id.slide_track_bg);
        tvSlideHint = findViewById(R.id.tv_slide_hint);

        String timeText = String.format("%02d:%02d", hour, minute);
        tvAlarmTime.setText(timeText);

        if (isBedtime) {
            tvAlarmTitle.setText("该睡觉了");
            tvAlarmSubtitle.setText("充足的睡眠让你精力充沛");
            slideContainer.setVisibility(View.GONE);
        } else {
            tvAlarmTitle.setText("早上好");
            tvAlarmSubtitle.setText("新的一天开始了，加油！");
        }
    }

    /**
     * 设置滑动关闭手势 - 直接在 slideContainer 上追踪整个滑动区域
     */
    private void setupSlideToDismiss() {
        if (isBedtime) return;

        // 等待布局完成后再获取正确的宽度
        slideContainer.post(() -> {
            maxSlideDistance = slideTrackBg.getWidth() - slideThumb.getWidth();
            Log.d(TAG, "maxSlideDistance = " + maxSlideDistance);
        });

        slideContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 如果 maxSlideDistance 还没计算好，用默认值
                if (maxSlideDistance <= 0) {
                    maxSlideDistance = slideTrackBg.getWidth() - slideThumb.getWidth();
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        // 检查触摸点是否在轨道区域内
                        float touchY = event.getY();
                        float trackTop = slideTrackBg.getTop();
                        float trackBottom = slideTrackBg.getBottom();
                        if (touchY >= trackTop && touchY <= trackBottom) {
                            slideStartX = event.getRawX();
                            thumbStartX = slideThumb.getTranslationX();
                            isSliding = true;
                            slideThumb.setPressed(true);
                            return true;
                        }
                        return false;
                    }

                    case MotionEvent.ACTION_MOVE:
                        if (!isSliding) return false;
                        float deltaX = event.getRawX() - slideStartX;
                        float newX = thumbStartX + deltaX;
                        newX = Math.max(0, Math.min(newX, maxSlideDistance));
                        slideThumb.setTranslationX(newX);

                        float progress = newX / maxSlideDistance;
                        tvSlideHint.setAlpha(1f - progress);

                        if (progress >= 0.8f && !slideCompleted) {
                            slideCompleted = true;
                            animateSlideComplete();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isSliding) return false;
                        isSliding = false;
                        slideThumb.setPressed(false);

                        if (!slideCompleted) {
                            animateSlideBack();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * 滑动完成动画
     */
    private void animateSlideComplete() {
        ValueAnimator animator = ValueAnimator.ofFloat(slideThumb.getTranslationX(), maxSlideDistance);
        animator.setDuration(200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            slideThumb.setTranslationX((Float) animation.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                handler.postDelayed(() -> dismissAlarm(), 300);
            }
        });
        animator.start();
    }

    /**
     * 滑动回弹动画
     */
    private void animateSlideBack() {
        ValueAnimator animator = ValueAnimator.ofFloat(slideThumb.getTranslationX(), 0f);
        animator.setDuration(300);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            slideThumb.setTranslationX((Float) animation.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tvSlideHint.setAlpha(0.8f);
            }
        });
        animator.start();
    }

    /**
     * 初始化TTS并播报每日内容
     */
    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized = true;
                int result = tts.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInitialized = false;
                    return;
                }
                handler.postDelayed(this::speakBriefing, 1500);
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {}

            @Override
            public void onError(String utteranceId) {}
        });
    }

    /**
     * 语音播报每日内容
     */
    private void speakBriefing() {
        if (!ttsInitialized || tts == null) return;

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        StringBuilder briefing = new StringBuilder();

        String greeting;
        if (hour < 6) greeting = "凌晨好";
        else if (hour < 9) greeting = "早上好";
        else if (hour < 12) greeting = "上午好";
        else if (hour < 14) greeting = "中午好";
        else if (hour < 18) greeting = "下午好";
        else greeting = "晚上好";
        briefing.append(greeting).append("。");

        SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日", Locale.CHINESE);
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        briefing.append("今天是").append(dateFormat.format(calendar.getTime())).append("，");
        briefing.append(weekDays[dayOfWeek]).append("。");

        PreferencesHelper prefsHelper = new PreferencesHelper(this);
        SleepSchedule schedule = prefsHelper.getActiveSchedule();
        if (schedule != null) {
            briefing.append("昨晚就寝时间").append(schedule.getBedtimeText()).append("，");
            briefing.append("计划睡眠时长").append(schedule.getSleepDurationText()).append("。");
        }

        Random random = new Random();
        int temp = 22 + random.nextInt(15);
        String[] conditions = {"晴", "多云", "阴", "晴间多云", "微风"};
        String condition = conditions[random.nextInt(conditions.length)];
        briefing.append("今日天气").append(condition).append("，");
        briefing.append("气温").append(temp).append("度。");

        String quote = QUOTES[random.nextInt(QUOTES.length)];
        briefing.append(quote);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(briefing.toString(), TextToSpeech.QUEUE_FLUSH, null, "briefing_utterance");
        } else {
            tts.speak(briefing.toString(), TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void setupListeners() {
        // 关闭闹钟按钮 - 始终可见
        View btnDismiss = findViewById(R.id.btn_dismiss);
        btnDismiss.setOnClickListener(v -> {
            Log.d(TAG, "btn_dismiss clicked");
            dismissAlarm();
        });
        // 确保可点击
        btnDismiss.setClickable(true);
        btnDismiss.setFocusable(true);

        // 稍后提醒按钮（仅起床闹钟有）
        View btnSnooze = findViewById(R.id.btn_snooze);
        if (!isBedtime && btnSnooze != null) {
            btnSnooze.setVisibility(View.VISIBLE);
            btnSnooze.setOnClickListener(v -> snoozeAlarm());
        } else if (btnSnooze != null) {
            btnSnooze.setVisibility(View.GONE);
        }

        // 查看每日播报（仅起床闹钟显示）
        View btnBriefing = findViewById(R.id.btn_briefing_from_alarm);
        if (!isBedtime && btnBriefing != null) {
            btnBriefing.setVisibility(View.VISIBLE);
            btnBriefing.setOnClickListener(v -> {
                dismissAlarm();
                startActivity(new Intent(this, DailyBriefingActivity.class));
            });
        } else if (btnBriefing != null) {
            btnBriefing.setVisibility(View.GONE);
        }

        // 就寝闹钟也显示关闭按钮（在滑动区域位置）
        if (isBedtime && slideContainer != null) {
            // 就寝时没有滑动条，但关闭按钮在按钮区已经可见
        }
    }

    /**
     * 关闭闹钟 - 核心方法：解绑服务 → 停止服务 → 关闭页面
     */
    private void dismissAlarm() {
        if (alarmDismissed) return;
        alarmDismissed = true;
        Log.d(TAG, "dismissAlarm");

        // 停止TTS
        stopTTS();

        // 停止闹钟声音
        if (isBound && alarmService != null) {
            alarmService.stopAlarm();
        }

        // 解绑服务（无论如何都要尝试）
        try {
            unbindService(serviceConnection);
        } catch (Exception e) {
            Log.w(TAG, "unbindService failed: " + e.getMessage());
        }
        isBound = false;

        // 直接停止前台服务
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
        AlarmService.isRinging = false;

        finish();
    }

    /**
     * 稍后提醒 - 10分钟后再次响铃
     */
    private void snoozeAlarm() {
        if (alarmDismissed) return;
        alarmDismissed = true;
        stopTTS();

        // 解绑并停止服务
        try {
            unbindService(serviceConnection);
        } catch (Exception ignored) {}
        isBound = false;

        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
        AlarmService.isRinging = false;

        // 设置10分钟后的临时闹钟
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);
        int snoozeHour = calendar.get(Calendar.HOUR_OF_DAY);
        int snoozeMinute = calendar.get(Calendar.MINUTE);

        AlarmManagerHelper.setSnoozeAlarm(this, snoozeHour, snoozeMinute);
        finish();
    }

    /**
     * 停止TTS
     */
    private void stopTTS() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        try {
            unbindService(serviceConnection);
        } catch (Exception ignored) {}
        isBound = false;
        stopTTS();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        // 禁止返回键关闭闹钟
    }
}
