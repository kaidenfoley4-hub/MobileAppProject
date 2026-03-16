package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private TextView tvNoTasks;
    private ListView listViewTasks;
    private ArrayAdapter<String> adapter;
    private TaskViewModel taskViewModel;
    private List<Task> currentTasks = new ArrayList<>();

    // Need to keep this so we can stop observing the old date
    // before we start observing the new one
    private LiveData<List<Task>> currentLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        listViewTasks = findViewById(R.id.listViewTasks);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);


        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewTasks.setAdapter(adapter);

        // show tasks straight away rather than an empty screen
        loadTasksForCalendarDate(Calendar.getInstance());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            loadTasksForCalendarDate(selected);

            // Format the date above the list
            String dateStr = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                    .format(selected.getTime());
            tvSelectedDate.setText(dateStr);
        });

        // only deletes for now, will be marking complete later
        listViewTasks.setOnItemClickListener((parent, view, position, id) -> {
            Task task = currentTasks.get(position);
            taskViewModel.delete(task);
            Toast.makeText(this, "Task removed", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadTasksForCalendarDate(Calendar cal) {
        // gets midnight of the selected day
        Calendar start = (Calendar) cal.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        // gets the end of the selected day
        Calendar end = (Calendar) cal.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        // stops watching the previous date query before starting a new one
        // this prevents multiple stale observers from piling up
        if (currentLiveData != null) {
            currentLiveData.removeObservers(this);
        }

        currentLiveData = taskViewModel.getTasksForDay(
                start.getTimeInMillis(),
                end.getTimeInMillis()
        );

        currentLiveData.observe(this, tasks -> {
            currentTasks = tasks;
            adapter.clear();

            if (tasks.isEmpty()) {
                tvNoTasks.setVisibility(View.VISIBLE);
                listViewTasks.setVisibility(View.GONE);
            } else {
                tvNoTasks.setVisibility(View.GONE);
                listViewTasks.setVisibility(View.VISIBLE);
                for (Task t : tasks) {
                    String status = t.isCompleted ? "✓ " : "• ";
                    adapter.add(status + t.title);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }
}