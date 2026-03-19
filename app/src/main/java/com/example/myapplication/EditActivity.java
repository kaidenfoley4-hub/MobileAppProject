package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditActivity extends AppCompatActivity {

    private TextInputEditText editTaskTitle;
    private TextView tvSelectedDate;
    private TaskViewModel taskViewModel;

    // the task being edited
    private Task currentTask;
    private long selectedDateMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        editTaskTitle = findViewById(R.id.editTaskTitle);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        MaterialCardView cardPickDate = findViewById(R.id.cardPickDate);
        Button btnSave = findViewById(R.id.btnSaveTask);
        Button btnDelete = findViewById(R.id.btnDeleteTask);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // the task id is passed in via Intent from whichever screen opened this one
        int taskId = getIntent().getIntExtra("TASK_ID", -1);

        if (taskId == -1) {
            // something went wrong, no task id was passed in
            Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // observe the task by its id so the fields pre-fill automatically
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task != null && currentTask == null) {
                // only pre-fill on the first load, not on every update
                // otherwise the user's edits would get overwritten
                currentTask = task;
                editTaskTitle.setText(task.title);
                selectedDateMillis = task.startTime;

                String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(task.startTime));
                tvSelectedDate.setText("Due: " + dateStr);
            }
        });

        // date picker works the same as in TodoActivity
        cardPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (selectedDateMillis != -1) {
                cal.setTimeInMillis(selectedDateMillis);
            }
            new DatePickerDialog(this, (view, year, month, day) -> {
                cal.set(year, month, day, 12, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                selectedDateMillis = cal.getTimeInMillis();

                String dateStr = day + "/" + (month + 1) + "/" + year;
                tvSelectedDate.setText("Due: " + dateStr);

            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDateMillis == -1) {
                Toast.makeText(this, "Please pick a due date", Toast.LENGTH_SHORT).show();
                return;
            }

            // update the current task fields and save
            currentTask.title = title;
            currentTask.startTime = selectedDateMillis;
            currentTask.endTime = selectedDateMillis + 3600000;
            taskViewModel.update(currentTask);

            Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
            finish(); // go back to the previous screen
        });

        btnDelete.setOnClickListener(v -> {
            if (currentTask != null) {
                taskViewModel.delete(currentTask);
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
