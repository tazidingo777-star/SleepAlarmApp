package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.sleepalarm.app.R;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.util.List;

/**
 * iOS 风格"就寝"页面
 */
public class SleepScheduleFragment extends Fragment {

    private static final int RINGTONE_REQUEST = 2001;

    private CircleClockView circleClock;
    private MaterialSwitch switchEnabled;
    private TextView tvBedtime;
    private TextView tvWakeTime;
    private PreferencesHelper prefsHelper;
    private SleepSchedule schedule;
    private Uri tempRingtoneUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sleep, container, false);
        prefsHelper = new PreferencesHelper(requireContext());

        circleClock = v.findViewById(R.id.circle_clock);
        switchEnabled = v.findViewById(R.id.switch_sleep_enabled);
        tvBedtime = v.findViewById(R.id.tv_bedtime);
        tvWakeTime = v.findViewById(R.id.tv_wake_time);

        tempRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        loadSchedule();

        circleClock.setOnTimeChangedListener((bh, bm, wh, wm) -> {
            schedule.setBedtimeHour(bh);
            schedule.setBedtimeMinute(bm);
            schedule.setWakeHour(wh);
            schedule.setWakeMinute(wm);
            updateTimeTexts();
            saveSchedule();
            if (schedule.isEnabled()) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            }
        });

        // 就寝时间点击：弹出文字输入框
        tvBedtime.setOnClickListener(view -> {
            showTimeInputDialog("设置就寝时间", schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                    (h, m) -> {
                        schedule.setBedtimeHour(h);
                        schedule.setBedtimeMinute(m);
                        circleClock.setTimes(schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                                schedule.getWakeHour(), schedule.getWakeMinute());
                        updateTimeTexts();
                        saveSchedule();
                        if (schedule.isEnabled()) {
                            AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
                        }
                    });
        });

        // 起床时间点击：弹出文字输入框
        tvWakeTime.setOnClickListener(view -> {
            showTimeInputDialog("设置起床时间", schedule.getWakeHour(), schedule.getWakeMinute(),
                    (h, m) -> {
                        schedule.setWakeHour(h);
                        schedule.setWakeMinute(m);
                        circleClock.setTimes(schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                                schedule.getWakeHour(), schedule.getWakeMinute());
                        updateTimeTexts();
                        saveSchedule();
                        if (schedule.isEnabled()) {
                            AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
                        }
                    });
        });

        switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            schedule.setEnabled(checked);
            saveSchedule();
            if (checked) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            } else {
                AlarmManagerHelper.cancelSleepAlarm(requireContext(), schedule);
            }
        });

        v.findViewById(R.id.btn_options).setOnClickListener(view -> {
            showOptionsDialog();
        });

        return v;
    }

    /**
     * 文字输入时间弹窗（替代轮盘）
     */
    private void showTimeInputDialog(String title, int initHour, int initMinute, TimePickedCallback callback) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // 80% 宽度，顶部留边距
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.80);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.08);
        dialog.getWindow().setAttributes(lp);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 32, 40, 32);
        root.setBackgroundColor(0xFF1C1C1E);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 24);
        root.addView(tvTitle);

        // 时:分 输入行
        LinearLayout inputRow = new LinearLayout(requireContext());
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER);

        EditText etHour = new EditText(requireContext());
        etHour.setText(String.format("%02d", initHour));
        etHour.setTextSize(36);
        etHour.setTextColor(Color.WHITE);
        etHour.setGravity(Gravity.CENTER);
        etHour.setInputType(InputType.TYPE_CLASS_NUMBER);
        etHour.setBackgroundColor(0xFF2C2C2E);
        etHour.setPadding(24, 16, 24, 16);
        etHour.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(36);
        sep.setTextColor(Color.WHITE);
        sep.setPadding(16, 0, 16, 0);

        EditText etMinute = new EditText(requireContext());
        etMinute.setText(String.format("%02d", initMinute));
        etMinute.setTextSize(36);
        etMinute.setTextColor(Color.WHITE);
        etMinute.setGravity(Gravity.CENTER);
        etMinute.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMinute.setBackgroundColor(0xFF2C2C2E);
        etMinute.setPadding(24, 16, 24, 16);
        etMinute.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        inputRow.addView(etHour);
        inputRow.addView(sep);
        inputRow.addView(etMinute);
        root.addView(inputRow);

        // 按钮
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        btnRow.setPadding(0, 24, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(Color.parseColor("#8E8E93"));
        btnCancel.setPadding(48, 14, 48, 14);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTextColor(Color.parseColor("#FF9500"));
        btnOK.setPadding(48, 14, 48, 14);
        btnOK.setOnClickListener(v2 -> {
            dialog.dismiss();
            try {
                int h = Integer.parseInt(etHour.getText().toString());
                int m = Integer.parseInt(etMinute.getText().toString());
                if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                    callback.onTimePicked(h, m);
                }
            } catch (NumberFormatException ignored) {}
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnOK);
        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }

    private interface TimePickedCallback {
        void onTimePicked(int hour, int minute);
    }

    private void loadSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        schedule = schedules.isEmpty() ? new SleepSchedule() : schedules.get(0);

        circleClock.setTimes(
                schedule.getBedtimeHour(), schedule.getBedtimeMinute(),
                schedule.getWakeHour(), schedule.getWakeMinute()
        );
        switchEnabled.setChecked(schedule.isEnabled());
        updateTimeTexts();
    }

    private void updateTimeTexts() {
        tvBedtime.setText(String.format("%02d:%02d", schedule.getBedtimeHour(), schedule.getBedtimeMinute()));
        tvWakeTime.setText(String.format("%02d:%02d", schedule.getWakeHour(), schedule.getWakeMinute()));
    }

    private void saveSchedule() {
        List<SleepSchedule> schedules = prefsHelper.loadSchedules();
        if (schedules.isEmpty()) {
            schedules.add(schedule);
        } else {
            schedules.set(0, schedule);
        }
        prefsHelper.saveSchedules(schedules);
    }

    /**
     * 统一的选项弹窗：可滚动，包含重复、铃音、渐强、每日播报
     */
    private void showOptionsDialog() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.85);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.08);
        dialog.getWindow().setAttributes(lp);

        ScrollView sv = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 32, 0, 32);
        root.setBackgroundColor(0xFF1C1C1E);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("就寝设置");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        // 重复日
        addOptionSectionTitle(root, "重复");
        showRepeatSection(root, dialog);

        // 铃音
        addOptionSectionTitle(root, "铃音");
        TextView tvRingtone = new TextView(requireContext());
        tvRingtone.setText("点击选择铃声");
        tvRingtone.setTextSize(16);
        tvRingtone.setTextColor(Color.parseColor("#8E8E93"));
        tvRingtone.setPadding(40, 14, 40, 14);
        tvRingtone.setBackgroundColor(0xFF2C2C2E);
        tvRingtone.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tempRingtoneUri);
            startActivityForResult(intent, RINGTONE_REQUEST);
        });
        root.addView(tvRingtone);

        // 渐响
        addOptionSectionTitle(root, "渐响时长");
        showGradualSection(root, dialog);

        // 每日播报
        addOptionSectionTitle(root, "每日播报");
        LinearLayout briefingRow = new LinearLayout(requireContext());
        briefingRow.setOrientation(LinearLayout.HORIZONTAL);
        briefingRow.setGravity(Gravity.CENTER_VERTICAL);
        briefingRow.setPadding(40, 12, 40, 12);
        briefingRow.setBackgroundColor(0xFF2C2C2E);

        TextView tvBriefing = new TextView(requireContext());
        tvBriefing.setText("启用每日播报");
        tvBriefing.setTextSize(16);
        tvBriefing.setTextColor(Color.WHITE);

        MaterialSwitch swBriefing = new MaterialSwitch(requireContext());
        swBriefing.setChecked(prefsHelper.isBriefingEnabled());
        swBriefing.setOnCheckedChangeListener((btn, c) -> prefsHelper.setBriefingEnabled(c));

        briefingRow.addView(tvBriefing);
        briefingRow.addView(new View(requireContext()) {{
            setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        }});
        briefingRow.addView(swBriefing);
        root.addView(briefingRow);

        // 查看每日播报
        TextView tvViewBriefing = new TextView(requireContext());
        tvViewBriefing.setText("查看每日播报 >");
        tvViewBriefing.setTextSize(16);
        tvViewBriefing.setTextColor(Color.parseColor("#FF9500"));
        tvViewBriefing.setPadding(40, 14, 40, 14);
        tvViewBriefing.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), DailyBriefingActivity.class));
        });
        root.addView(tvViewBriefing);

        // 关闭
        TextView btnClose = new TextView(requireContext());
        btnClose.setText("关闭");
        btnClose.setTextSize(16);
        btnClose.setTextColor(Color.parseColor("#FF9500"));
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setPadding(40, 24, 40, 8);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        sv.addView(root);
        dialog.setContentView(sv);
        dialog.show();
    }

    private void showRepeatSection(LinearLayout root, Dialog parentDialog) {
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean[] selected = schedule.getRepeatDays().clone();

        LinearLayout daysContainer = new LinearLayout(requireContext());
        daysContainer.setOrientation(LinearLayout.HORIZONTAL);
        daysContainer.setGravity(Gravity.CENTER);
        daysContainer.setPadding(0, 8, 0, 8);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView dayView = new TextView(requireContext());
            dayView.setText(dayNames[i]);
            dayView.setTextSize(13);
            dayView.setPadding(6, 8, 6, 8);
            dayView.setGravity(Gravity.CENTER);
            dayView.setMinWidth(36);
            if (selected[idx]) {
                dayView.setTextColor(Color.BLACK);
                dayView.setBackgroundColor(Color.parseColor("#FF9500"));
            } else {
                dayView.setTextColor(Color.parseColor("#8E8E93"));
                dayView.setBackgroundColor(0xFF2C2C2E);
            }
            dayView.setOnClickListener(v2 -> {
                selected[idx] = !selected[idx];
                if (selected[idx]) {
                    dayView.setTextColor(Color.BLACK);
                    dayView.setBackgroundColor(Color.parseColor("#FF9500"));
                } else {
                    dayView.setTextColor(Color.parseColor("#8E8E93"));
                    dayView.setBackgroundColor(0xFF2C2C2E);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 0, 2, 0);
            dayView.setLayoutParams(params);
            daysContainer.addView(dayView);
        }
        root.addView(daysContainer);

        // OK button for repeat
        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(14);
        btnOK.setTextColor(Color.parseColor("#FF9500"));
        btnOK.setGravity(Gravity.CENTER);
        btnOK.setPadding(0, 8, 0, 0);
        btnOK.setOnClickListener(v -> {
            schedule.setRepeatDays(selected);
            saveSchedule();
            if (schedule.isEnabled()) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            }
        });
        root.addView(btnOK);
    }

    private void showGradualSection(LinearLayout root, Dialog parentDialog) {
        String[] options = {"1分钟", "3分钟", "5分钟", "10分钟", "15分钟", "20分钟", "30分钟"};
        int[] values = {1, 3, 5, 10, 15, 20, 30};

        LinearLayout gradualRow = new LinearLayout(requireContext());
        gradualRow.setOrientation(LinearLayout.HORIZONTAL);
        gradualRow.setGravity(Gravity.CENTER);
        gradualRow.setPadding(0, 8, 0, 0);

        for (int i = 0; i < options.length; i++) {
            final int val = values[i];
            TextView tv = new TextView(requireContext());
            tv.setText(options[i]);
            tv.setTextSize(12);
            tv.setPadding(6, 8, 6, 8);
            tv.setGravity(Gravity.CENTER);
            tv.setMinWidth(42);
            if (val == schedule.getGradualMinutes()) {
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundColor(Color.parseColor("#FF9500"));
            } else {
                tv.setTextColor(Color.parseColor("#8E8E93"));
                tv.setBackgroundColor(0xFF2C2C2E);
            }
            tv.setOnClickListener(v2 -> {
                schedule.setGradualMinutes(val);
                saveSchedule();
                // update all styles
                for (int j = 0; j < gradualRow.getChildCount(); j++) {
                    View child = gradualRow.getChildAt(j);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(Color.parseColor("#8E8E93"));
                        child.setBackgroundColor(0xFF2C2C2E);
                    }
                }
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundColor(Color.parseColor("#FF9500"));
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 0, 2, 0);
            tv.setLayoutParams(params);
            gradualRow.addView(tv);
        }
        root.addView(gradualRow);
    }

    private void addOptionSectionTitle(LinearLayout root, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#8E8E93"));
        tv.setPadding(40, 20, 40, 8);
        root.addView(tv);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_REQUEST && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                tempRingtoneUri = uri;
            }
        }
    }
}
