package com.sleepalarm.app.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

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

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private View layoutEmpty;
    private PreferencesHelper prefsHelper;
    private AlarmAdapter adapter;
    private List<Alarm> alarms;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarms, container, false);
        prefsHelper = new PreferencesHelper(requireContext());

        layoutEmpty = v.findViewById(R.id.layout_empty);
        recyclerView = v.findViewById(R.id.recycler_alarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        fab = v.findViewById(R.id.fab_add_alarm);
        fab.setOnClickListener(view -> showAddAlarmDialog());

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
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddAlarmDialog() {
        showAlarmDialog(null);
    }

    private void showAlarmDialog(@Nullable Alarm existingAlarm) {
        final Alarm alarm = existingAlarm != null ? existingAlarm : new Alarm();
        showWheelTimePicker(alarm.getHour(), alarm.getMinute(), (hour, minute) -> {
            alarm.setHour(hour);
            alarm.setMinute(minute);
            showAlarmDetailDialog(alarm);
        });
    }

    private void showWheelTimePicker(int initialHour, int initialMinute, TimePickedCallback callback) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 32, 40, 32);
        root.setBackgroundColor(0xFF1C1C1E);

        // Title
        TextView title = new TextView(requireContext());
        title.setText("设置闹钟时间");
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 20);
        root.addView(title);

        // Time display that's clickable for direct input
        TextView tvTimeDisplay = new TextView(requireContext());
        tvTimeDisplay.setText(String.format("%02d:%02d", initialHour, initialMinute));
        tvTimeDisplay.setTextSize(42);
        tvTimeDisplay.setTextColor(Color.parseColor("#FF9500"));
        tvTimeDisplay.setGravity(android.view.Gravity.CENTER);
        tvTimeDisplay.setPadding(0, 16, 0, 20);
        root.addView(tvTimeDisplay);

        // NumberPicker container
        LinearLayout pickerRow = new LinearLayout(requireContext());
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(android.view.Gravity.CENTER);

        NumberPicker hourPicker = new NumberPicker(requireContext());
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(initialHour);
        hourPicker.setFormatter(i -> String.format("%02d", i));
        hourPicker.setTextColor(Color.WHITE);
        hourPicker.setDividerColor(Color.parseColor("#FF9500"));

        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(32);
        sep.setTextColor(Color.WHITE);
        sep.setPadding(16, 0, 16, 0);

        NumberPicker minutePicker = new NumberPicker(requireContext());
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(initialMinute);
        minutePicker.setFormatter(i -> String.format("%02d", i));
        minutePicker.setTextColor(Color.WHITE);
        minutePicker.setDividerColor(Color.parseColor("#FF9500"));

        pickerRow.addView(hourPicker);
        pickerRow.addView(sep);
        pickerRow.addView(minutePicker);
        root.addView(pickerRow);

        // Update time display when wheel changes
        NumberPicker.OnValueChangeListener wheelListener = (picker, oldVal, newVal) -> {
            tvTimeDisplay.setText(String.format("%02d:%02d", hourPicker.getValue(), minutePicker.getValue()));
        };
        hourPicker.setOnValueChangedListener(wheelListener);
        minutePicker.setOnValueChangedListener(wheelListener);

        // Click time display to input directly
        tvTimeDisplay.setOnClickListener(v -> {
            android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(requireContext(),
                    (view, h, m) -> {
                        hourPicker.setValue(h);
                        minutePicker.setValue(m);
                    },
                    hourPicker.getValue(), minutePicker.getValue(), true);
            tpd.setTitle("输入时间");
            tpd.show();
        });

        // Buttons
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        btnRow.setPadding(0, 24, 0, 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(16);
        btnCancel.setTextColor(Color.parseColor("#8E8E93"));
        btnCancel.setPadding(40, 12, 40, 12);
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnOK = new TextView(requireContext());
        btnOK.setText("确定");
        btnOK.setTextSize(16);
        btnOK.setTextColor(Color.parseColor("#FF9500"));
        btnOK.setPadding(40, 12, 40, 12);
        btnOK.setOnClickListener(v2 -> {
            dialog.dismiss();
            callback.onTimePicked(hourPicker.getValue(), minutePicker.getValue());
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

    private void showAlarmDetailDialog(Alarm alarm) {
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        boolean[] selectedDays = alarm.getRepeatDays().clone();

        new AlertDialog.Builder(requireContext())
                .setTitle(alarm.getTimeText())
                .setMultiChoiceItems(dayNames, selectedDays, (dialog, which, isChecked) -> {
                    selectedDays[which] = isChecked;
                })
                .setPositiveButton("保存", (dialog, which) -> {
                    alarm.setRepeatDays(selectedDays);
                    alarm.setEnabled(true);
                    prefsHelper.saveAlarm(alarm);
                    AlarmManagerHelper.setAlarm(requireContext(), alarm);
                    loadAlarms();
                })
                .setNegativeButton("取消", null)
                .show();
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
