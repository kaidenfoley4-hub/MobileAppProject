package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TodoActivity extends AppCompatActivity {

    private TextInputEditText editTask;
    private TextView tvSelectedDate;
    private TaskViewModel taskViewModel;

    private TaskAdapter pendingAdapter;
    private TaskAdapter completedAdapter;

    private List<Task> pendingTasks = new ArrayList<>();
    private List<Task> completedTasks = new ArrayList<>();
    private List<Task> currentTasks = new ArrayList<>();

    private long selectedDateMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editTask = findViewById(R.id.editTask);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        ListView pendingListView = findViewById(R.id.listViewPendingTasks);
        ListView completedListView = findViewById(R.id.listViewCompletedTasks);

        MaterialCardView btnPickDate = findViewById(R.id.btnPickDate);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnExport = findViewById(R.id.btnExport);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        pendingAdapter = new TaskAdapter(this, new ArrayList<>(), (task, isChecked) -> {
            task.setCompleted(isChecked);
            taskViewModel.update(task);
        });

        completedAdapter = new TaskAdapter(this, new ArrayList<>(), (task, isChecked) -> {
            task.setCompleted(isChecked);
            taskViewModel.update(task);
        });

        pendingListView.setAdapter(pendingAdapter);
        completedListView.setAdapter(completedAdapter);

        taskViewModel.getPendingTasks().observe(this, tasks -> {
            pendingTasks = tasks;
            pendingAdapter.clear();
            pendingAdapter.addAll(tasks);
            pendingAdapter.notifyDataSetChanged();
        });

        taskViewModel.getCompletedTasks().observe(this, tasks -> {
            completedTasks = tasks;
            completedAdapter.clear();
            completedAdapter.addAll(tasks);
            completedAdapter.notifyDataSetChanged();
        });

        taskViewModel.getAllTasks().observe(this, tasks -> {
            currentTasks = tasks;
        });

        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();

            new DatePickerDialog(this, (view, year, month, day) -> {
                cal.set(year, month, day, 12, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);

                selectedDateMillis = cal.getTimeInMillis();

                String dateStr = day + "/" + (month + 1) + "/" + year;
                tvSelectedDate.setText("Due: " + dateStr);

            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnAdd.setOnClickListener(v -> {
            String title = editTask.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDateMillis == -1) {
                Toast.makeText(this, "Please pick a due date", Toast.LENGTH_SHORT).show();
                return;
            }

            Task task = new Task(title, "", "", selectedDateMillis, selectedDateMillis + 3600000);
            taskViewModel.insert(task);

            editTask.setText("");
            tvSelectedDate.setText("No date selected");
            selectedDateMillis = -1;
        });

        pendingListView.setOnItemClickListener((parent, view, position, id) -> {
            Task taskToEdit = pendingTasks.get(position);
            Intent intent = new Intent(TodoActivity.this, EditActivity.class);
            intent.putExtra("TASK_ID", taskToEdit.id);
            startActivity(intent);
        });

        completedListView.setOnItemClickListener((parent, view, position, id) -> {
            Task taskToEdit = completedTasks.get(position);
            Intent intent = new Intent(TodoActivity.this, EditActivity.class);
            intent.putExtra("TASK_ID", taskToEdit.id);
            startActivity(intent);
        });

        btnExport.setOnClickListener(v -> {
            if (currentTasks == null || currentTasks.isEmpty()) {
                Toast.makeText(this, "No tasks to export", Toast.LENGTH_SHORT).show();
                return;
            }

            IcsExporter.exportToFile(this, currentTasks);
        });
    }
}