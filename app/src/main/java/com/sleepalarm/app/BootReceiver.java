package com.sleepalarm.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sleepalarm.app.utils.AlarmManagerHelper;

/**
 * 开机广播接收器 - 设备重启后自动恢复所有闹钟
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "设备启动完成，恢复闹钟设置");
            AlarmManagerHelper.setAllAlarms(context);
        }
    }
}
