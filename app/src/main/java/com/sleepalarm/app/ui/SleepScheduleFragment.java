package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.sleepalarm.app.R;
import com.sleepalarm.app.models.SleepSchedule;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;
import com.sleepalarm.app.utils.PressFeedbackHelper;
import com.sleepalarm.app.utils.ThemeColors;

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
    private TextView currentRingtoneValueView;

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

        PressFeedbackHelper.apply(tvBedtime);
        PressFeedbackHelper.apply(tvWakeTime);

        loadSchedule();
        tempRingtoneUri = schedule.getRingtoneUri().isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                : Uri.parse(schedule.getRingtoneUri());

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
            PressFeedbackHelper.apply(v.findViewById(R.id.btn_options));
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
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setCornerRadius(dpToPx(16));
        rootBg.setColor(ThemeColors.getSurface(requireContext()));
        root.setBackground(rootBg);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(24);
        tvTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));
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
        etHour.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        etHour.setGravity(Gravity.CENTER);
        etHour.setInputType(InputType.TYPE_CLASS_NUMBER);
        GradientDrawable etHourBg = new GradientDrawable();
        etHourBg.setCornerRadius(dpToPx(12));
        etHourBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        etHour.setBackground(etHourBg);
        etHour.setPadding(24, 16, 24, 16);
        etHour.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(36);
        sep.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        sep.setPadding(16, 0, 16, 0);

        EditText etMinute = new EditText(requireContext());
        etMinute.setText(String.format("%02d", initMinute));
        etMinute.setTextSize(36);
        etMinute.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        etMinute.setGravity(Gravity.CENTER);
        etMinute.setInputType(InputType.TYPE_CLASS_NUMBER);
        GradientDrawable etMinuteBg = new GradientDrawable();
        etMinuteBg.setCornerRadius(dpToPx(12));
        etMinuteBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        etMinute.setBackground(etMinuteBg);
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
        btnCancel.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        btnCancel.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        PressFeedbackHelper.apply(btnCancel);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTextColor(ThemeColors.getAccent(requireContext()));
        btnOK.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        PressFeedbackHelper.apply(btnOK);
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
     * 统一的选项弹窗：可滚动，包含重复、渐响、铃音、振动
     */
    private void showOptionsDialog() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.06);
        dialog.getWindow().setAttributes(lp);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setCornerRadius(dpToPx(20));
        rootBg.setColor(ThemeColors.getSurface(requireContext()));
        root.setBackground(rootBg);

        ScrollView sv = new ScrollView(requireContext());
        sv.addView(root);

        // ===== 顶部导航 =====
        LinearLayout navBar = new LinearLayout(requireContext());
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER_VERTICAL);
        navBar.setPadding(0, 0, 0, dpToPx(16));

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(ThemeColors.getAccent(requireContext()));
        btnCancel.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("就寝设置");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView btnDone = new TextView(requireContext());
        btnDone.setText("完成");
        btnDone.setTextSize(16);
        btnDone.setTextColor(ThemeColors.getAccent(requireContext()));
        btnDone.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        navBar.addView(btnCancel);
        navBar.addView(tvTitle);
        navBar.addView(btnDone);
        root.addView(navBar);

        // ===== 重复日期卡片 =====
        boolean[] selectedDays = schedule.getRepeatDays().clone();
        LinearLayout repeatCard = createCard();
        repeatCard.setOrientation(LinearLayout.VERTICAL);
        repeatCard.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        TextView tvRepeatTitle = new TextView(requireContext());
        tvRepeatTitle.setText("响铃重复日期");
        tvRepeatTitle.setTextSize(16);
        tvRepeatTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));

        TextView tvRepeatSubtitle = new TextView(requireContext());
        tvRepeatSubtitle.setTextSize(14);
        tvRepeatSubtitle.setTextColor(ThemeColors.getAccent(requireContext()));
        tvRepeatSubtitle.setPadding(0, dpToPx(4), 0, dpToPx(12));

        Runnable updateRepeatSubtitle = () -> tvRepeatSubtitle.setText(getRepeatSummary(selectedDays));

        LinearLayout daysRow = new LinearLayout(requireContext());
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        daysRow.setWeightSum(7);
        String[] shortDays = {"日", "一", "二", "三", "四", "五", "六"};

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView day = new TextView(requireContext());
            day.setText(shortDays[i]);
            day.setTextSize(14);
            day.setGravity(Gravity.CENTER);
            day.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(40), 1));
            day.setBackgroundResource(selectedDays[i] ? R.drawable.bg_day_selected : R.drawable.bg_day_unselected);
            day.setTextColor(selectedDays[i] ? ThemeColors.getOnAccent(requireContext()) : ThemeColors.getTextSecondary(requireContext()));
            PressFeedbackHelper.apply(day);
            day.setOnClickListener(v -> {
                selectedDays[idx] = !selectedDays[idx];
                day.setBackgroundResource(selectedDays[idx] ? R.drawable.bg_day_selected : R.drawable.bg_day_unselected);
                day.setTextColor(selectedDays[idx] ? ThemeColors.getOnAccent(requireContext()) : ThemeColors.getTextSecondary(requireContext()));
                updateRepeatSubtitle.run();
            });
            daysRow.addView(day);
        }

        repeatCard.addView(tvRepeatTitle);
        repeatCard.addView(tvRepeatSubtitle);
        repeatCard.addView(daysRow);
        root.addView(repeatCard);
        updateRepeatSubtitle.run();

        // ===== 渐响时长卡片 =====
        LinearLayout gradualCard = createCard();
        gradualCard.setOrientation(LinearLayout.VERTICAL);
        gradualCard.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        TextView tvGradualTitle = new TextView(requireContext());
        tvGradualTitle.setText("渐响时长");
        tvGradualTitle.setTextSize(16);
        tvGradualTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        gradualCard.addView(tvGradualTitle);

        LinearLayout gradualChips = new LinearLayout(requireContext());
        gradualChips.setOrientation(LinearLayout.HORIZONTAL);
        gradualChips.setGravity(Gravity.CENTER);
        gradualChips.setPadding(0, dpToPx(12), 0, 0);
        int[] gradualValues = {1, 3, 5, 10, 15, 20, 30};
        String[] gradualLabels = {"1分", "3分", "5分", "10分", "15分", "20分", "30分"};
        final int[] selectedGradual = {schedule.getGradualMinutes()};

        for (int i = 0; i < gradualValues.length; i++) {
            final int val = gradualValues[i];
            TextView tv = new TextView(requireContext());
            tv.setText(gradualLabels[i]);
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            tv.setMinWidth(dpToPx(40));
            updateChipStyle(tv, val == selectedGradual[0]);
            PressFeedbackHelper.apply(tv);
            tv.setOnClickListener(v3 -> {
                selectedGradual[0] = val;
                for (int j = 0; j < gradualChips.getChildCount(); j++) {
                    View child = gradualChips.getChildAt(j);
                    if (child instanceof TextView) {
                        updateChipStyle((TextView) child, child == tv);
                    }
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) p.setMargins(dpToPx(6), 0, 0, 0);
            tv.setLayoutParams(p);
            gradualChips.addView(tv);
        }
        gradualCard.addView(gradualChips);
        root.addView(gradualCard);

        // ===== 铃声 / 振动 选项卡片 =====
        LinearLayout optionsCard = createCard();
        optionsCard.setOrientation(LinearLayout.VERTICAL);
        optionsCard.setPadding(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));

        currentRingtoneValueView = new TextView(requireContext());
        currentRingtoneValueView.setText(schedule.getRingtoneName());
        currentRingtoneValueView.setTextSize(14);
        currentRingtoneValueView.setTextColor(ThemeColors.getTextSecondary(requireContext()));

        TextView tvVibrateValue = new TextView(requireContext());
        tvVibrateValue.setText(schedule.getVibrateMode());
        tvVibrateValue.setTextSize(14);
        tvVibrateValue.setTextColor(ThemeColors.getTextSecondary(requireContext()));

        // 铃声行
        LinearLayout ringtoneRow = createOptionRow("铃声", currentRingtoneValueView);
        ringtoneRow.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择起床铃声");
            Uri existing = schedule.getRingtoneUri().isEmpty() ? null : Uri.parse(schedule.getRingtoneUri());
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
            tempRingtoneUri = existing;
            startActivityForResult(intent, RINGTONE_REQUEST);
        });
        optionsCard.addView(ringtoneRow);
        addDivider(optionsCard);

        // 振动行
        LinearLayout vibrateRow = createOptionRow("振动", tvVibrateValue);
        vibrateRow.setOnClickListener(v -> {
            String[] modes = {"默认振动", "响铃时振动", "不振动"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("振动模式")
                    .setItems(modes, (d, which) -> {
                        schedule.setVibrateMode(modes[which]);
                        schedule.setVibrate(!modes[which].equals("不振动"));
                        tvVibrateValue.setText(modes[which]);
                    })
                    .show();
        });
        optionsCard.addView(vibrateRow);

        root.addView(optionsCard);

        // ===== 事件绑定 =====
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            schedule.setRepeatDays(selectedDays);
            schedule.setGradualMinutes(selectedGradual[0]);
            if (tempRingtoneUri != null) {
                schedule.setRingtoneUri(tempRingtoneUri.toString());
                schedule.setRingtoneName(getRingtoneTitle(tempRingtoneUri));
            }
            saveSchedule();
            if (schedule.isEnabled()) {
                AlarmManagerHelper.setSleepAlarm(requireContext(), schedule);
            }
        });

        dialog.setContentView(sv);
        dialog.show();
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        card.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(p);
        return card;
    }

    private LinearLayout createOptionRow(String title, TextView valueView) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(0), dpToPx(12), dpToPx(0), dpToPx(12));
        row.setClickable(true);
        row.setFocusable(true);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));

        row.addView(tvTitle);
        row.addView(new View(requireContext()) {{
            setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        }});
        row.addView(valueView);

        TextView arrow = new TextView(requireContext());
        arrow.setText(">");
        arrow.setTextSize(16);
        arrow.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        arrow.setPadding(dpToPx(8), 0, 0, 0);
        row.addView(arrow);
        return row;
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        divider.setBackgroundColor(ThemeColors.getDivider(requireContext()));
        parent.addView(divider);
    }

    private void updateChipStyle(TextView tv, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(20));
        if (selected) {
            tv.setTextColor(ThemeColors.getOnAccent(requireContext()));
            bg.setColor(ThemeColors.getAccent(requireContext()));
        } else {
            tv.setTextColor(ThemeColors.getTextSecondary(requireContext()));
            bg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        }
        tv.setBackground(bg);
    }

    private String getRepeatSummary(boolean[] days) {
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean any = false, all = true;
        for (boolean d : days) {
            if (d) any = true;
            else all = false;
        }
        if (!any) return "仅一次";
        if (all) return "每天";

        boolean weekdays = true, weekends = true;
        for (int i = 1; i <= 5; i++) if (!days[i]) weekdays = false;
        if (!days[0] || !days[6]) weekends = false;
        if (weekdays && !weekends) return "工作日";
        if (!weekdays && weekends) return "周末";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(dayNames[i]);
            }
        }
        return sb.toString();
    }

    private String getRingtoneTitle(Uri uri) {
        try {
            android.media.Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), uri);
            if (ringtone != null) {
                String title = ringtone.getTitle(requireContext());
                if (title != null && !title.isEmpty()) return title;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "自定义铃声";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_REQUEST && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                tempRingtoneUri = uri;
                if (currentRingtoneValueView != null) {
                    currentRingtoneValueView.setText(getRingtoneTitle(uri));
                }
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
