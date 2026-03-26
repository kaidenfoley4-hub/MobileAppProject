package com.example.myapplication;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends ArrayAdapter<Task> {

    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChanged(Task task, boolean isChecked);
    }

    private final OnTaskCheckedChangeListener listener;

    public TaskAdapter(@NonNull Context context, @NonNull List<Task> tasks,
                       OnTaskCheckedChangeListener listener) {
        super(context, 0, tasks);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Task task = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_list_item, parent, false);
        }

        CheckBox checkComplete = convertView.findViewById(R.id.checkComplete);
        TextView textTaskTitle = convertView.findViewById(R.id.textTaskTitle);
        TextView textTaskDate = convertView.findViewById(R.id.textTaskDate);

        // Ensure the checkbox does not steal focus from the list item.
        // This allows the list item's click used for editing to be received while
        // the checkbox still handles marking complete when the checkbox itself is clicked.
        checkComplete.setFocusable(false);
        checkComplete.setFocusableInTouchMode(false);
        checkComplete.setClickable(true);

        if (task != null) {
            textTaskTitle.setText(task.title);

            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(task.startTime));
            textTaskDate.setText("Due: " + dateStr);

            checkComplete.setOnCheckedChangeListener(null);
            checkComplete.setChecked(task.isCompleted);

            if (task.isCompleted) {
                convertView.setAlpha(0.5f);

                textTaskTitle.setPaintFlags(
                        textTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            } else {
                convertView.setAlpha(1.0f);

                textTaskTitle.setPaintFlags(
                        textTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            }

            checkComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onTaskCheckedChanged(task, isChecked);
            });
        }

        return convertView;
    }
}