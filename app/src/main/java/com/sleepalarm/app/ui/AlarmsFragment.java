package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sleepalarm.app.utils.ThemeColors;
import com.sleepalarm.app.utils.PressFeedbackHelper;
import com.sleepalarm.app.utils.ViewUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.sleepalarm.app.R;
import com.sleepalarm.app.models.Alarm;
import com.sleepalarm.app.utils.AlarmManagerHelper;
import com.sleepalarm.app.utils.PreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class AlarmsFragment extends Fragment {

    private static final int RINGTONE_REQUEST = 1001;

    private RecyclerView recyclerView;
    private ImageButton btnAddEmpty, btnAddNormal;
    private View layoutEmpty;
    private PreferencesHelper prefsHelper;
    private AlarmAdapter adapter;
    private List<Alarm> alarms;

    // 临时保存新建闹钟的铃声 URI
    private Uri tempRingtoneUri;
    // 当前弹窗中的铃声值显示控件
    private TextView currentRingtoneValueView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarms, container, false);
        prefsHelper = new PreferencesHelper(requireContext());

        layoutEmpty = v.findViewById(R.id.layout_empty);
        recyclerView = v.findViewById(R.id.recycler_alarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnAddEmpty = v.findViewById(R.id.btn_add_alarm_empty);
        btnAddNormal = v.findViewById(R.id.btn_add_alarm);
        PressFeedbackHelper.apply(btnAddEmpty);
        PressFeedbackHelper.apply(btnAddNormal);
        btnAddEmpty.setOnClickListener(view -> showAddAlarmDialog());
        btnAddNormal.setOnClickListener(view -> showAddAlarmDialog());

        loadAlarms();
        return v;
    }

    private void loadAlarms() {
        alarms = prefsHelper.loadAlarms();
        adapter = new AlarmAdapter(alarms);
        recyclerView.setAdapter(adapter);

        if (alarms.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnAddNormal.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnAddNormal.setVisibility(View.VISIBLE);
        }
    }

    private void showAddAlarmDialog() {
        showAlarmDialog(null);
    }

    private void showAlarmDialog(@Nullable Alarm existingAlarm) {
        final Alarm alarm = existingAlarm != null ? existingAlarm : new Alarm();
        tempRingtoneUri = alarm.getRingtoneUri().isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                : Uri.parse(alarm.getRingtoneUri());
        showUnifiedAlarmDialog(alarm, existingAlarm != null);
    }

    /**
     * 像素级复刻参考图风格的闹钟设置弹窗
     */
    private void showUnifiedAlarmDialog(Alarm alarm, boolean isEditing) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.06);
        dialog.getWindow().setAttributes(lp);

        // 主容器：深色背景 + 圆角
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setCornerRadius(dpToPx(20));
        rootBg.setColor(ThemeColors.getSurface(requireContext()));
        root.setBackground(rootBg);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(root);

        // ===== 顶部导航栏：取消 | 标题 | 完成 =====
        LinearLayout navBar = new LinearLayout(requireContext());
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER_VERTICAL);
        navBar.setPadding(0, 0, 0, dpToPx(16));

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(ThemeColors.getAccent(requireContext()));
        btnCancel.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        TextView tvNavTitle = new TextView(requireContext());
        tvNavTitle.setText(isEditing ? "编辑闹钟" : "新建闹钟");
        tvNavTitle.setTextSize(18);
        tvNavTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        tvNavTitle.setGravity(Gravity.CENTER);
        tvNavTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView btnDone = new TextView(requireContext());
        btnDone.setText("完成");
        btnDone.setTextSize(16);
        btnDone.setTextColor(ThemeColors.getAccent(requireContext()));
        btnDone.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        navBar.addView(btnCancel);
        navBar.addView(tvNavTitle);
        navBar.addView(btnDone);
        root.addView(navBar);

        // ===== 时间选择卡片 =====
        LinearLayout timeCard = createCard();
        timeCard.setGravity(Gravity.CENTER);
        timeCard.setPadding(0, dpToPx(8), 0, dpToPx(8));

        NumberPicker hourPicker = new NumberPicker(requireContext());
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(alarm.getHour());
        hourPicker.setFormatter(i -> String.format("%02d", i));
        ViewUtils.setNumberPickerTextColor(hourPicker, ThemeColors.getTextPrimary(requireContext()));
        ViewUtils.setNumberPickerDividerColor(hourPicker, ThemeColors.getAccent(requireContext()));
        hourPicker.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(80), dpToPx(140)));

        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(28);
        sep.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        sep.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        NumberPicker minutePicker = new NumberPicker(requireContext());
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(alarm.getMinute());
        minutePicker.setFormatter(i -> String.format("%02d", i));
        ViewUtils.setNumberPickerTextColor(minutePicker, ThemeColors.getTextPrimary(requireContext()));
        ViewUtils.setNumberPickerDividerColor(minutePicker, ThemeColors.getAccent(requireContext()));
        minutePicker.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(80), dpToPx(140)));

        timeCard.addView(hourPicker);
        timeCard.addView(sep);
        timeCard.addView(minutePicker);
        root.addView(timeCard);

        // ===== 响铃模式 chips =====
        LinearLayout modeRow = new LinearLayout(requireContext());
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setPadding(0, dpToPx(16), 0, dpToPx(8));
        String[] modeLabels = {"响一次", "工作日响铃", "自定义"};
        TextView[] modeViews = new TextView[3];
        boolean[] selectedDays = alarm.getRepeatDays().clone();

        Runnable updateModeChips = () -> {
            boolean weekdays = true, weekends = true, any = false, all = true;
            for (int i = 1; i <= 5; i++) if (!selectedDays[i]) weekdays = false;
            if (!selectedDays[0] || !selectedDays[6]) weekends = false;
            for (boolean d : selectedDays) {
                if (d) any = true;
                else all = false;
            }
            int mode = 2; // 自定义
            if (!any) mode = 0;
            else if (weekdays && !weekends) mode = 1;
            for (int i = 0; i < 3; i++) {
                updateChipStyle(modeViews[i], i == mode);
            }
        };

        for (int i = 0; i < 3; i++) {
            final int modeIdx = i;
            TextView tv = new TextView(requireContext());
            tv.setText(modeLabels[i]);
            tv.setTextSize(14);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) p.setMargins(dpToPx(8), 0, 0, 0);
            tv.setLayoutParams(p);
            PressFeedbackHelper.apply(tv);
            tv.setOnClickListener(v -> {
                if (modeIdx == 0) {
                    for (int j = 0; j < 7; j++) selectedDays[j] = false;
                } else if (modeIdx == 1) {
                    selectedDays[0] = false;
                    selectedDays[6] = false;
                    for (int j = 1; j <= 5; j++) selectedDays[j] = true;
                }
                updateModeChips.run();
                // 自定义模式不自动改日期，由用户点星期
            });
            modeViews[i] = tv;
            modeRow.addView(tv);
        }
        root.addView(modeRow);

        // ===== 重复日期卡片 =====
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

        Runnable updateRepeatSubtitle = () -> {
            tvRepeatSubtitle.setText(getRepeatSummary(selectedDays));
        };

        LinearLayout daysRow = new LinearLayout(requireContext());
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        daysRow.setWeightSum(7);
        String[] shortDays = {"日", "一", "二", "三", "四", "五", "六"};
        TextView[] dayViews = new TextView[7];

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
                updateModeChips.run();
                updateRepeatSubtitle.run();
            });
            dayViews[i] = day;
            daysRow.addView(day);
        }

        repeatCard.addView(tvRepeatTitle);
        repeatCard.addView(tvRepeatSubtitle);
        repeatCard.addView(daysRow);
        root.addView(repeatCard);
        updateRepeatSubtitle.run();
        updateModeChips.run();

        // ===== 法定节假日不响铃 =====
        LinearLayout holidayRow = createSettingRow("法定节假日不响铃", "已勾选日期中识别法定节假日不响铃");
        MaterialSwitch swHoliday = new MaterialSwitch(requireContext());
        swHoliday.setChecked(alarm.isHolidaySkip());
        holidayRow.addView(new View(requireContext()) {{
            setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        }});
        holidayRow.addView(swHoliday);
        root.addView(holidayRow);

        // ===== 闹钟名称卡片 =====
        LinearLayout nameCard = createCard();
        nameCard.setOrientation(LinearLayout.VERTICAL);
        nameCard.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        TextView tvNameHint = new TextView(requireContext());
        tvNameHint.setText("闹钟名称");
        tvNameHint.setTextSize(12);
        tvNameHint.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        EditText etName = new EditText(requireContext());
        etName.setText(alarm.getName());
        etName.setHint("");
        etName.setTextSize(16);
        etName.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        etName.setBackground(null);
        etName.setPadding(0, dpToPx(4), 0, 0);
        etName.setSingleLine(true);
        nameCard.addView(tvNameHint);
        nameCard.addView(etName);
        root.addView(nameCard);

        // ===== 可选项列表卡片 =====
        LinearLayout optionsCard = createCard();
        optionsCard.setOrientation(LinearLayout.VERTICAL);
        optionsCard.setPadding(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));

        TextView tvRingtoneValue = new TextView(requireContext());
        tvRingtoneValue.setText(alarm.getRingtoneName());
        tvRingtoneValue.setTextSize(14);
        tvRingtoneValue.setTextColor(ThemeColors.getTextSecondary(requireContext()));

        TextView tvVibrateValue = new TextView(requireContext());
        tvVibrateValue.setText(alarm.getVibrateMode());
        tvVibrateValue.setTextSize(14);
        tvVibrateValue.setTextColor(ThemeColors.getTextSecondary(requireContext()));

        TextView tvSnoozeValue = new TextView(requireContext());
        tvSnoozeValue.setText(String.format("响铃间隔 %d 分钟，响铃次数 %d 次", alarm.getSnoozeInterval(), alarm.getSnoozeCount()));
        tvSnoozeValue.setTextSize(14);
        tvSnoozeValue.setTextColor(ThemeColors.getTextSecondary(requireContext()));

        // 铃声行
        currentRingtoneValueView = tvRingtoneValue;
        LinearLayout ringtoneRow = createOptionRow("铃声", tvRingtoneValue);
        ringtoneRow.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声");
            Uri existing = alarm.getRingtoneUri().isEmpty() ? null : Uri.parse(alarm.getRingtoneUri());
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
                        alarm.setVibrateMode(modes[which]);
                        alarm.setVibrate(!modes[which].equals("不振动"));
                        tvVibrateValue.setText(modes[which]);
                    })
                    .show();
        });
        optionsCard.addView(vibrateRow);
        addDivider(optionsCard);

        // 稍后提醒行
        LinearLayout snoozeRow = createOptionRow("稍后提醒", tvSnoozeValue);
        snoozeRow.setOnClickListener(v -> {
            String[] intervals = {"1 分钟", "5 分钟", "10 分钟", "15 分钟", "30 分钟"};
            int[] intervalValues = {1, 5, 10, 15, 30};
            new AlertDialog.Builder(requireContext())
                    .setTitle("稍后提醒间隔")
                    .setItems(intervals, (d, which) -> {
                        alarm.setSnoozeInterval(intervalValues[which]);
                        tvSnoozeValue.setText(String.format("响铃间隔 %d 分钟，响铃次数 %d 次", alarm.getSnoozeInterval(), alarm.getSnoozeCount()));
                    })
                    .show();
        });
        optionsCard.addView(snoozeRow);

        root.addView(optionsCard);

        // ===== 渐响音量（保留旧功能） =====
        addSectionTitle(root, "渐响音量");
        LinearLayout gradualRow = createSettingRow("渐响", "");
        MaterialSwitch swGradual = new MaterialSwitch(requireContext());
        swGradual.setChecked(alarm.getGradualMinutes() > 0);
        gradualRow.addView(new View(requireContext()) {{
            setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        }});
        gradualRow.addView(swGradual);
        root.addView(gradualRow);

        LinearLayout gradualChips = new LinearLayout(requireContext());
        gradualChips.setOrientation(LinearLayout.HORIZONTAL);
        gradualChips.setGravity(Gravity.CENTER);
        gradualChips.setPadding(0, dpToPx(8), 0, 0);
        gradualChips.setVisibility(swGradual.isChecked() ? View.VISIBLE : View.GONE);

        int[] gradualValues = {1, 3, 5, 10, 15, 20, 30};
        String[] gradualLabels = {"1分", "3分", "5分", "10分", "15分", "20分", "30分"};
        int currentGradual = alarm.getGradualMinutes();
        if (currentGradual <= 0) currentGradual = 5;
        final int[] selectedGradual = {currentGradual};

        for (int i = 0; i < gradualValues.length; i++) {
            final int val = gradualValues[i];
            TextView tv = new TextView(requireContext());
            tv.setText(gradualLabels[i]);
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            tv.setMinWidth(dpToPx(40));
            updateChipStyle(tv, val == currentGradual);
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
        root.addView(gradualChips);

        swGradual.setOnCheckedChangeListener((btn, checked) -> {
            gradualChips.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // ===== 删除按钮（编辑时） =====
        if (isEditing) {
            TextView btnDelete = new TextView(requireContext());
            btnDelete.setText("删除闹钟");
            btnDelete.setTextSize(16);
            btnDelete.setTextColor(Color.parseColor("#FF3B30"));
            btnDelete.setGravity(Gravity.CENTER);
            btnDelete.setPadding(0, dpToPx(20), 0, dpToPx(8));
            btnDelete.setOnClickListener(v2 -> {
                dialog.dismiss();
                AlarmManagerHelper.cancelAlarm(requireContext(), alarm);
                prefsHelper.deleteAlarm(alarm.getId());
                loadAlarms();
            });
            root.addView(btnDelete);
        }

        // ===== 事件绑定 =====
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            alarm.setHour(hourPicker.getValue());
            alarm.setMinute(minutePicker.getValue());
            alarm.setName(etName.getText().toString().trim());
            alarm.setRepeatDays(selectedDays);
            alarm.setHolidaySkip(swHoliday.isChecked());

            if (tempRingtoneUri != null) {
                alarm.setRingtoneUri(tempRingtoneUri.toString());
                alarm.setRingtoneName(getRingtoneTitle(tempRingtoneUri));
            }

            int selGradual = selectedGradual[0];
            if (!swGradual.isChecked()) selGradual = 0;
            alarm.setGradualMinutes(selGradual);

            alarm.setEnabled(true);
            prefsHelper.saveAlarm(alarm);
            AlarmManagerHelper.setAlarm(requireContext(), alarm);
            loadAlarms();
        });

        dialog.setContentView(scrollView);
        dialog.show();
    }

    private void updateDayStyle(TextView tv, boolean selected) {
        GradientDrawable dayBg = new GradientDrawable();
        dayBg.setCornerRadius(dpToPx(8));
        if (selected) {
            tv.setTextColor(ThemeColors.getOnAccent(requireContext()));
            dayBg.setColor(ThemeColors.getAccent(requireContext()));
        } else {
            tv.setTextColor(ThemeColors.getTextSecondary(requireContext()));
            dayBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        }
        tv.setBackground(dayBg);
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

    private LinearLayout createSettingRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        row.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(p);

        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(requireContext());
            tvSub.setText(subtitle);
            tvSub.setTextSize(12);
            tvSub.setTextColor(ThemeColors.getTextSecondary(requireContext()));
            tvSub.setPadding(0, dpToPx(2), 0, 0);
            textCol.addView(tvSub);
        }
        row.addView(textCol);
        return row;
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

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void addSectionTitle(LinearLayout root, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        tv.setPadding(0, 20, 0, 10);
        root.addView(tv);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_REQUEST && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                tempRingtoneUri = uri;
                if (currentRingtoneValueView != null) {
                    String title = getRingtoneTitle(uri);
                    currentRingtoneValueView.setText(title);
                }
            }
        }
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

    private class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

        private List<Alarm> list;

        AlarmAdapter(List<Alarm> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_alarm, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Alarm alarm = list.get(position);
            holder.tvTime.setText(alarm.getTimeText());
            String displayLabel = alarm.getName().isEmpty() ? alarm.getLabel() : alarm.getName();
            holder.tvLabel.setText(displayLabel);
            holder.tvRepeat.setText(alarm.getRepeatDaysText());
            holder.switchEnabled.setOnCheckedChangeListener(null);
            holder.switchEnabled.setChecked(alarm.isEnabled());
            holder.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
                alarm.setEnabled(checked);
                prefsHelper.saveAlarm(alarm);
                if (checked) {
                    AlarmManagerHelper.setAlarm(requireContext(), alarm);
                } else {
                    AlarmManagerHelper.cancelAlarm(requireContext(), alarm);
                }
            });

            holder.itemView.setOnClickListener(v -> showAlarmDialog(alarm));
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("删除闹钟")
                        .setMessage("确定删除 " + alarm.getTimeText() + " 吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            AlarmManagerHelper.cancelAlarm(requireContext(), alarm);
                            prefsHelper.deleteAlarm(alarm.getId());
                            loadAlarms();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvLabel, tvRepeat;
            MaterialSwitch switchEnabled;

            ViewHolder(View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_alarm_time);
                tvLabel = itemView.findViewById(R.id.tv_alarm_label);
                tvRepeat = itemView.findViewById(R.id.tv_alarm_repeat);
                switchEnabled = itemView.findViewById(R.id.switch_alarm);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlarms();
    }
}
