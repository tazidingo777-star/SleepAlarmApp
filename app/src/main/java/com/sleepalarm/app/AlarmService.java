package com.sleepalarm.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

import com.sleepalarm.app.ui.AlarmAlertActivity;
import com.sleepalarm.app.utils.AlarmManagerHelper;

/**
 * 闹钟前台服务 - 实现渐响功能和闹钟界面展示
 */
public class AlarmService extends Service {

    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1001;

    /** 静态标志：闹钟是否正在响铃。MainActivity 用此判断是否需要跳转到关闭界面 */
    public static volatile boolean isRinging = false;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private CountDownTimer volumeTimer;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;

    private int maxVolume;
    private int gradualMinutes;
    private int currentVolume = 0;
    private boolean isBedtime;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        }
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        gradualMinutes = intent.getIntExtra(AlarmManagerHelper.EXTRA_GRADUAL_MINUTES, 10);
        isBedtime = intent.getBooleanExtra(AlarmManagerHelper.EXTRA_IS_BEDTIME, false);

        // 获取WakeLock防止屏幕熄灭
        acquireWakeLock();

        // 启动前台服务通知
        startForeground(NOTIFICATION_ID, buildNotification());

        // 标记闹钟正在响铃
        isRinging = true;

        // 启动渐响闹钟
        startGradualAlarm(intent);

        // 打开闹钟界面
        openAlarmScreen(intent);

        return START_NOT_STICKY;
    }

    /**
     * 获取WakeLock
     */
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "SleepAlarm::WakeLock"
            );
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L); // 最多持有10分钟
            }
        }
    }

    /**
     * 启动渐响闹钟 - 音量从0逐渐增加到最大
     */
    private void startGradualAlarm(Intent intent) {
        // 初始化MediaPlayer播放闹钟铃声
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 设置初始音量为0
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
        }

        // 开始播放
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }

        // 启动振动
        startVibration();

        // 渐响计时器: 在 gradualMinutes 分钟内从0到最大音量
        long totalDurationMs = gradualMinutes * 60L * 1000L;
        int volumeSteps = maxVolume > 0 ? maxVolume : 15;
        long intervalMs = totalDurationMs / volumeSteps;

        volumeTimer = new CountDownTimer(totalDurationMs, intervalMs) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentVolume++;
                if (currentVolume <= maxVolume) {
                    setAlarmVolume(currentVolume);
                }
            }

            @Override
            public void onFinish() {
                // 达到最大音量
                setAlarmVolume(maxVolume);
            }
        };
        volumeTimer.start();
    }

    /**
     * 设置闹钟音量
     */
    private void setAlarmVolume(int volume) {
        if (audioManager != null && volume >= 0 && volume <= maxVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
        }
    }

    /**
     * 启动振动
     */
    private void startVibration() {
        long[] pattern = {0, 1000, 500, 1000}; // 振动模式：等待0ms，振动1000ms，暂停500ms，振动1000ms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibrator = vibratorManager.getDefaultVibrator();
            }
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
            vibrator.vibrate(effect);
        }
    }

    /**
     * 停止闹钟
     */
    public void stopAlarm() {
        if (volumeTimer != null) {
            volumeTimer.cancel();
            volumeTimer = null;
        }

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }

        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        // 恢复原始音量
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume / 2, 0);
        }
    }

    /**
     * 打开闹钟响铃界面
     */
    private void openAlarmScreen(Intent intent) {
        Intent alertIntent = new Intent(this, AlarmAlertActivity.class);
        alertIntent.putExtras(intent);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(alertIntent);
    }

    /**
     * 构建前台通知 - 包含关闭动作
     */
    private Notification buildNotification() {
        String title = isBedtime ? "就寝时间到" : "起床时间到";

        // 通知栏关闭闹钟的 Intent
        Intent dismissIntent = new Intent(this, AlarmReceiver.class);
        dismissIntent.setAction(AlarmManagerHelper.ACTION_STOP_ALARM);
        PendingIntent dismissPI = PendingIntent.getBroadcast(this, 0, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 点击通知打开闹钟界面
        Intent openIntent = new Intent(this, AlarmAlertActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPI = PendingIntent.getActivity(this, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("点击打开闹钟界面")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(openPI)
                .addAction(android.R.drawable.ic_media_pause, "关闭闹钟", dismissPI)
                .build();
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "闹钟提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于闹钟响铃时的前台通知");
            channel.setSound(null, null);
            channel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * LocalBinder - 让Activity可以获取Service实例来控制闹钟
     */
    public class LocalBinder extends android.os.Binder {
        public AlarmService getService() {
            return AlarmService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        isRinging = false;
        stopAlarm();
        super.onDestroy();
    }
}
