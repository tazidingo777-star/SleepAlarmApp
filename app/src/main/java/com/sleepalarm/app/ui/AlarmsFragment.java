package com.sleepalarm.app.ui;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
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
    private PreferencesHelper prefsHelper;
    private AlarmAdapter adapter;
    private List<Alarm> alarms;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alarms, container, false);
        prefsHelper = new PreferencesHelper(requireContext());

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
    }

    private void showAddAlarmDialog() {
        showAlarmDialog(null);
    }

    private void showAlarmDialog(@Nullable Alarm existingAlarm) {
        final Alarm alarm = existingAlarm != null ? existingAlarm : new Alarm();

        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (view, hour, minute) -> {
                    alarm.setHour(hour);
                    alarm.setMinute(minute);
                    showAlarmDetailDialog(alarm);
                },
                alarm.getHour(), alarm.getMinute(), true);
        timePicker.setTitle("设置闹钟时间");
        timePicker.show();
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
