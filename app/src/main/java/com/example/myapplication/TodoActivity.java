package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodoActivity extends AppCompatActivity {

    private EditText editTask;
    private TextView tvSelectedDate;
    private TaskViewModel taskViewModel;
    private ArrayAdapter<String> adapter;
    private List<Task> currentTasks = new ArrayList<>();
    private long selectedDateMillis = -1;  // stores the picked date and -1 means no date is picked yet

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editTask = findViewById(R.id.editTask);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        ListView listView = findViewById(R.id.listViewTasks);
        Button btnPickDate = findViewById(R.id.btnPickDate);
        Button btnAdd = findViewById(R.id.btnAdd);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        // any time the task list changes in the database this runs automatically
        taskViewModel.getAllTasks().observe(this, tasks -> {
            currentTasks = tasks;
            adapter.clear();
            for (Task t : tasks) {

                String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(t.startTime));
                adapter.add(t.title + " — " + dateStr);
            }
            adapter.notifyDataSetChanged();
        });


        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            // set to noon so that the wrong day cannot be accidentally chosen by timezone offsets
            new DatePickerDialog(this, (view, year, month, day) -> {

                cal.set(year, month, day, 12, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                selectedDateMillis = cal.getTimeInMillis();


                String dateStr = day + "/" + (month + 1) + "/" + year;
                tvSelectedDate.setText("Due: " + dateStr);

            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
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

            // endtime defaults to 1 hour after start
            // this will be customizable later by the user
            Task task = new Task(title, "", "", selectedDateMillis, selectedDateMillis + 3600000);
            taskViewModel.insert(task);

            // clear the form to allow a new task to be created
            editTask.setText("");
            tvSelectedDate.setText("No date selected");
            selectedDateMillis = -1;
        });

        // currently tap to delete, can be changed to a long press or completion later
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Task taskToDelete = currentTasks.get(position);
            taskViewModel.delete(taskToDelete);
            Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
        });

        Button btnExport = findViewById(R.id.btnExport);

        btnExport.setOnClickListener(v -> {
            if (currentTasks == null || currentTasks.isEmpty()) {
                Toast.makeText(this, "No tasks to export", Toast.LENGTH_SHORT).show();
                return;
            }
            IcsExporter.exportToFile(this, currentTasks);
        });
    }
}