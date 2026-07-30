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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private FloatingActionButton fabEmpty, fabNormal;
    private View layoutEmpty;
    private PreferencesHelper prefsHelper;
    private AlarmAdapter adapter;
    private List<Alarm> alarms;

    // 临时保存新建闹钟的铃声 URI
    private Uri tempRingtoneUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarms, container, false);
        prefsHelper = new PreferencesHelper(requireContext());

        layoutEmpty = v.findViewById(R.id.layout_empty);
        recyclerView = v.findViewById(R.id.recycler_alarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        fabEmpty = v.findViewById(R.id.fab_add_alarm_empty);
        fabNormal = v.findViewById(R.id.fab_add_alarm);
        PressFeedbackHelper.apply(fabEmpty);
        PressFeedbackHelper.apply(fabNormal);
        fabEmpty.setOnClickListener(view -> showAddAlarmDialog());
        fabNormal.setOnClickListener(view -> showAddAlarmDialog());

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
            fabNormal.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            fabNormal.setVisibility(View.VISIBLE);
        }
    }

    private void showAddAlarmDialog() {
        showAlarmDialog(null);
    }

    private void showAlarmDialog(@Nullable Alarm existingAlarm) {
        final Alarm alarm = existingAlarm != null ? existingAlarm : new Alarm();
        tempRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        showUnifiedAlarmDialog(alarm, existingAlarm != null);
    }

    /**
     * 统一的闹钟设置弹窗：80%宽度、顶部对齐、可滚动
     * 包含：时间轮盘、重复日、标签、铃音、渐强
     */
    private void showUnifiedAlarmDialog(Alarm alarm, boolean isEditing) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // 80% 宽度，顶部留20%边距
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.80);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.08);
        dialog.getWindow().setAttributes(lp);

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 28, 32, 28);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setCornerRadius(dpToPx(16));
        rootBg.setColor(ThemeColors.getSurface(requireContext()));
        root.setBackground(rootBg);

        // === 标题 ===
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(alarm.getTimeText());
        tvTitle.setTextSize(24);
        tvTitle.setTextColor(ThemeColors.getAccent(requireContext()));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        // === 时间选择（轮盘） ===
        addSectionTitle(root, "时间");
        LinearLayout pickerRow = new LinearLayout(requireContext());
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(Gravity.CENTER);

        NumberPicker hourPicker = new NumberPicker(requireContext());
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(alarm.getHour());
        hourPicker.setFormatter(i -> String.format("%02d", i));
        hourPicker.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        ViewUtils.setNumberPickerDividerColor(hourPicker, ThemeColors.getAccent(requireContext()));

        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(28);
        sep.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        sep.setPadding(12, 0, 12, 0);

        NumberPicker minutePicker = new NumberPicker(requireContext());
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(alarm.getMinute());
        minutePicker.setFormatter(i -> String.format("%02d", i));
        minutePicker.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        ViewUtils.setNumberPickerDividerColor(minutePicker, ThemeColors.getAccent(requireContext()));

        pickerRow.addView(hourPicker);
        pickerRow.addView(sep);
        pickerRow.addView(minutePicker);
        root.addView(pickerRow);

        PressFeedbackHelper.apply(tvTitle);
        tvTitle.setOnClickListener(v -> {
            android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(requireContext(),
                    (view, h, m) -> {
                        hourPicker.setValue(h);
                        minutePicker.setValue(m);
                        tvTitle.setText(String.format("%02d:%02d", h, m));
                    },
                    hourPicker.getValue(), minutePicker.getValue(), true);
            tpd.setTitle("输入时间");
            tpd.show();
        });

        NumberPicker.OnValueChangeListener wheelListener = (picker, oldVal, newVal) -> {
            tvTitle.setText(String.format("%02d:%02d", hourPicker.getValue(), minutePicker.getValue()));
        };
        hourPicker.setOnValueChangedListener(wheelListener);
        minutePicker.setOnValueChangedListener(wheelListener);

        // === 标签 ===
        addSectionTitle(root, "标签");
        EditText etLabel = new EditText(requireContext());
        etLabel.setText(alarm.getLabel());
        etLabel.setTextSize(16);
        etLabel.setTextColor(ThemeColors.getTextPrimary(requireContext()));
        etLabel.setHintTextColor(Color.parseColor("#636366"));
        etLabel.setHint("闹钟名称");
        GradientDrawable etBg = new GradientDrawable();
        etBg.setCornerRadius(dpToPx(12));
        etBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        etLabel.setBackground(etBg);
        etLabel.setPadding(24, 16, 24, 16);
        etLabel.setSingleLine(true);
        root.addView(etLabel);

        // === 重复日 ===
        addSectionTitle(root, "重复");
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean[] selectedDays = alarm.getRepeatDays().clone();

        LinearLayout daysContainer = new LinearLayout(requireContext());
        daysContainer.setOrientation(LinearLayout.HORIZONTAL);
        daysContainer.setGravity(Gravity.CENTER);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView dayView = new TextView(requireContext());
            dayView.setText(dayNames[i]);
            dayView.setTextSize(13);
            dayView.setPadding(6, 8, 6, 8);
            dayView.setGravity(Gravity.CENTER);
            dayView.setMinWidth(36);
            updateDayStyle(dayView, selectedDays[idx]);
            PressFeedbackHelper.apply(dayView);
            dayView.setOnClickListener(v2 -> {
                selectedDays[idx] = !selectedDays[idx];
                updateDayStyle(dayView, selectedDays[idx]);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 0, 2, 0);
            dayView.setLayoutParams(params);
            daysContainer.addView(dayView);
        }
        root.addView(daysContainer);

        // === 铃音 ===
        addSectionTitle(root, "铃音");
        TextView tvRingtone = new TextView(requireContext());
        tvRingtone.setText("点击选择铃声");
        tvRingtone.setTextSize(16);
        tvRingtone.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        tvRingtone.setPadding(24, 14, 24, 14);
        GradientDrawable ringtoneBg = new GradientDrawable();
        ringtoneBg.setCornerRadius(dpToPx(12));
        ringtoneBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        tvRingtone.setBackground(ringtoneBg);
        PressFeedbackHelper.apply(tvRingtone);
        tvRingtone.setOnClickListener(v2 -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tempRingtoneUri);
            startActivityForResult(intent, RINGTONE_REQUEST);
            // 用 liveData 方式：暂存到 fragment 成员
        });
        root.addView(tvRingtone);

        // === 渐响开关 ===
        addSectionTitle(root, "渐响音量");
        LinearLayout gradualRow = new LinearLayout(requireContext());
        gradualRow.setOrientation(LinearLayout.HORIZONTAL);
        gradualRow.setGravity(Gravity.CENTER_VERTICAL);
        gradualRow.setPadding(24, 12, 24, 12);
        GradientDrawable gradRowBg = new GradientDrawable();
        gradRowBg.setCornerRadius(dpToPx(12));
        gradRowBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        gradualRow.setBackground(gradRowBg);

        TextView tvGradual = new TextView(requireContext());
        tvGradual.setText("渐响");
        tvGradual.setTextSize(16);
        tvGradual.setTextColor(ThemeColors.getTextPrimary(requireContext()));

        MaterialSwitch swGradual = new MaterialSwitch(requireContext());
        swGradual.setChecked(alarm.getGradualMinutes() > 0);

        gradualRow.addView(tvGradual);
        gradualRow.addView(new View(requireContext()) {{
            setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        }});
        gradualRow.addView(swGradual);
        root.addView(gradualRow);

        // 渐响时长选择
        LinearLayout gradualDurationRow = new LinearLayout(requireContext());
        gradualDurationRow.setOrientation(LinearLayout.HORIZONTAL);
        gradualDurationRow.setGravity(Gravity.CENTER);
        gradualDurationRow.setPadding(0, 8, 0, 0);
        gradualDurationRow.setVisibility(swGradual.isChecked() ? View.VISIBLE : View.GONE);

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
            tv.setPadding(6, 6, 6, 6);
            tv.setGravity(Gravity.CENTER);
            tv.setMinWidth(36);
            GradientDrawable gradChipBg = new GradientDrawable();
            gradChipBg.setCornerRadius(dpToPx(8));
            if (val == currentGradual) {
                tv.setTextColor(ThemeColors.getOnAccent(requireContext()));
                gradChipBg.setColor(ThemeColors.getAccent(requireContext()));
            } else {
                tv.setTextColor(ThemeColors.getTextSecondary(requireContext()));
                gradChipBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
            }
            tv.setBackground(gradChipBg);
            final int idx = i;
            PressFeedbackHelper.apply(tv);
            tv.setOnClickListener(v3 -> {
                selectedGradual[0] = val;
                for (int j = 0; j < gradualDurationRow.getChildCount(); j++) {
                    View child = gradualDurationRow.getChildAt(j);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(ThemeColors.getTextSecondary(requireContext()));
                        GradientDrawable unselBg = new GradientDrawable();
                        unselBg.setCornerRadius(dpToPx(8));
                        unselBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
                        child.setBackground(unselBg);
                    }
                }
                GradientDrawable selBg = new GradientDrawable();
                selBg.setCornerRadius(dpToPx(8));
                selBg.setColor(ThemeColors.getAccent(requireContext()));
                tv.setBackground(selBg);
                tv.setTextColor(ThemeColors.getOnAccent(requireContext()));
            });
            gradualDurationRow.addView(tv);
        }
        root.addView(gradualDurationRow);

        swGradual.setOnCheckedChangeListener((btn, checked) -> {
            gradualDurationRow.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // === 振动 ===
        LinearLayout vibrateRow = new LinearLayout(requireContext());
        vibrateRow.setOrientation(LinearLayout.HORIZONTAL);
        vibrateRow.setGravity(Gravity.CENTER_VERTICAL);
        vibrateRow.setPadding(24, 12, 24, 12);
        GradientDrawable vibRowBg = new GradientDrawable();
        vibRowBg.setCornerRadius(dpToPx(12));
        vibRowBg.setColor(ThemeColors.getSurfaceLight(requireContext()));
        vibrateRow.setBackground(vibRowBg);
        vibrateRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvVibrate = new TextView(requireContext());
        tvVibrate.setText("振动");
        tvVibrate.setTextSize(16);
        tvVibrate.setTextColor(ThemeColors.getTextPrimary(requireContext()));

        MaterialSwitch swVibrate = new MaterialSwitch(requireContext());
        swVibrate.setChecked(alarm.isVibrate());

        vibrateRow.addView(tvVibrate);
        vibrateRow.addView(new View(requireContext()) {{
            setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1));
        }});
        vibrateRow.addView(swVibrate);
        root.addView(vibrateRow);

        // === 按钮 ===
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        btnRow.setPadding(0, 24, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(ThemeColors.getTextSecondary(requireContext()));
        btnCancel.setPadding(48, 0, 48, 0);
        btnCancel.setHeight(dpToPx(48));
        btnCancel.setGravity(Gravity.CENTER);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setCornerRadius(dpToPx(12));
        cancelBg.setColor(android.graphics.Color.TRANSPARENT);
        btnCancel.setBackground(cancelBg);
        PressFeedbackHelper.apply(btnCancel);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnSave = new TextView(requireContext());
        btnSave.setText("保存");
        btnSave.setTextSize(16);
        btnSave.setTextColor(ThemeColors.getOnAccent(requireContext()));
        btnSave.setPadding(48, 0, 48, 0);
        btnSave.setHeight(dpToPx(48));
        btnSave.setGravity(Gravity.CENTER);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(dpToPx(12));
        saveBg.setColor(ThemeColors.getAccent(requireContext()));
        btnSave.setBackground(saveBg);
        PressFeedbackHelper.apply(btnSave);
        btnSave.setOnClickListener(v2 -> {
            dialog.dismiss();
            alarm.setHour(hourPicker.getValue());
            alarm.setMinute(minutePicker.getValue());
            alarm.setLabel(etLabel.getText().toString().trim().isEmpty() ? "闹钟" : etLabel.getText().toString().trim());
            alarm.setRepeatDays(selectedDays);

            // 渐强时长
            int selGradual = selectedGradual[0];
            if (!swGradual.isChecked()) selGradual = 0;
            alarm.setGradualMinutes(selGradual);

            alarm.setVibrate(swVibrate.isChecked());
            alarm.setEnabled(true);

            prefsHelper.saveAlarm(alarm);
            AlarmManagerHelper.setAlarm(requireContext(), alarm);
            loadAlarms();
        });

        // 删除按钮（仅编辑已有闹钟时显示）
        btnRow.addView(btnCancel);
        if (isEditing) {
            TextView btnDelete = new TextView(requireContext());
            btnDelete.setText("删除");
            btnDelete.setTextSize(16);
            btnDelete.setTextColor(Color.parseColor("#FF3B30"));
            btnDelete.setPadding(48, 0, 48, 0);
            btnDelete.setHeight(dpToPx(48));
            btnDelete.setGravity(Gravity.CENTER);
            GradientDrawable deleteBg = new GradientDrawable();
            deleteBg.setCornerRadius(dpToPx(12));
            deleteBg.setColor(android.graphics.Color.TRANSPARENT);
            btnDelete.setBackground(deleteBg);
            PressFeedbackHelper.apply(btnDelete);
            btnDelete.setOnClickListener(v2 -> {
                dialog.dismiss();
                AlarmManagerHelper.cancelAlarm(requireContext(), alarm);
                prefsHelper.deleteAlarm(alarm.getId());
                loadAlarms();
            });
            btnRow.addView(btnDelete);
        }
        btnRow.addView(btnSave);

        root.addView(btnRow);
        scrollView.addView(root);
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
            }
        }
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
            holder.tvLabel.setText(alarm.getLabel());
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
