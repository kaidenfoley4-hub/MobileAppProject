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

    // for the task title
    private EditText editTask;
    // date user picked
    private TextView tvSelectedDate;
    // connection to the database layer
    private TaskViewModel taskViewModel;
    // bridge between the list of tasks and the ListView on screen
    private ArrayAdapter<String> adapter;
    // always holds the most recently loaded list of tasks and starts as an empty array so that exporting doesn't crash
    private List<Task> currentTasks = new ArrayList<>();
    // stores the data the user picks from the date picker as a millisecond timestamp
    private long selectedDateMillis = -1;  // stores the picked date and -1 means no date is picked yet


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // same as calendar activity in terms of setting up the screen and xml layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editTask = findViewById(R.id.editTask);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        ListView listView = findViewById(R.id.listViewTasks);
        Button btnPickDate = findViewById(R.id.btnPickDate);
        Button btnAdd = findViewById(R.id.btnAdd);

        // explained in calendarActivity
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // explained in calendarActivity
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        // any time the task list changes in the database this runs automatically
        taskViewModel.getAllTasks().observe(this, tasks -> {
            // updates the class level list every time the observer fires, keeping it in sync with the database
            // this is what the exporter and delete listener use for tasks so it is important that it is in sync at all times
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
            // set to be the selected date at noon so that the wrong day cannot be accidentally chosen by timezone offsets
            new DatePickerDialog(this, (view, year, month, day) -> {

                cal.set(year, month, day, 12, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                // converts the date to a millisecond timestamp for use later
                selectedDateMillis = cal.getTimeInMillis();

                // the calendar class stores zero-based months so +1 is added to match actual month number
                String dateStr = day + "/" + (month + 1) + "/" + year;
                tvSelectedDate.setText("Due: " + dateStr);

                },
                    // these set the initial date the calendar opens on which is today
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });


        btnAdd.setOnClickListener(v -> {
            // takes whatever the user types and converts it to a plain string
            String title = editTask.getText().toString().trim();

            // if the string is empty after trimming the task is not saved and a toast appears
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }
            // if a date is not selected the task is not saved and a toast is shown
            if (selectedDateMillis == -1) {
                Toast.makeText(this, "Please pick a due date", Toast.LENGTH_SHORT).show();
                return;
            }

            // endtime defaults to 1 hour after start
            // this will be customizable later by the user
            // the two empty strings are description and location whic cannot be entered yet
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

        // should probably be moved to the top with the other views
        Button btnExport = findViewById(R.id.btnExport);

        // the null and empty checks prevent a crash when the export button is tapped if the database is empty
        // the list of currenttasks is then converted to an ics file
        btnExport.setOnClickListener(v -> {
            if (currentTasks == null || currentTasks.isEmpty()) {
                Toast.makeText(this, "No tasks to export", Toast.LENGTH_SHORT).show();
                return;
            }
            IcsExporter.exportToFile(this, currentTasks);
        });
    }
}