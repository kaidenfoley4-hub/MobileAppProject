package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
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
    //tv stands for textviews
    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private TextView tvNoTasks;
    private ListView listViewTasks;
    // the bridge between the list of tasks and the listview on screen. The adapter populates the list.
    private ArrayAdapter<String> adapter;
    private TaskViewModel taskViewModel;
    private List<Task> currentTasks = new ArrayList<>();

    // Need to keep this so we can stop observing the old date
    // before we start observing the new one
    private LiveData<List<Task>> currentLiveData;

    //this override tells which XML layout to use, for this it is activity_calendar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // findViewByID returns the view with the matching id in the xml file
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        listViewTasks = findViewById(R.id.listViewTasks);

        // this gets the ViewModel for this Activity. A check is done for if it already exists such as for rotation.
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // this creates and adapter that manages the list of strings and displays each as a simple text row.
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewTasks.setAdapter(adapter);

        // show tasks straight away rather than an empty screen
        loadTasksForCalendarDate(Calendar.getInstance());

        // This creates a listener that fires every time a date is tapped.
        // A calendar object is created for the current moment and then overwritten with the tapped date by selected.set(year,month,dayOfMonth)
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            loadTasksForCalendarDate(selected);

            // Format the date to be readable above the list, taking into account local language settings
            String dateStr = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                    .format(selected.getTime());
            tvSelectedDate.setText(dateStr);
        });

        // tap a task to edit it
        listViewTasks.setOnItemClickListener((parent, view, position, id) -> {
            Task taskToEdit = currentTasks.get(position);
            Intent intent = new Intent(CalendarActivity.this, EditActivity.class);
            intent.putExtra("TASK_ID", taskToEdit.id);
            startActivity(intent);
        });
    }


    private void loadTasksForCalendarDate(Calendar cal) {
        // sets to midnight of the selected day
        Calendar start = (Calendar) cal.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        // sets to the end of the selected day
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

        // this converts the calendar objects into long millisecond timestamps which are passed onto the ViewModel,
        // , which passes onto the Repository, which passes onto the DAO which runs the SQL query
        // the result is stored in currentLiveData which can be removed the next time the method runs
        currentLiveData = taskViewModel.getTasksForDay(
                start.getTimeInMillis(),
                end.getTimeInMillis()
        );

        // this registers the observer. "this" ties it to the activity so it automatically stops when the activity does.

        currentLiveData.observe(this, tasks -> {
            currentTasks = tasks;
            // wipes the current list before repopulating it, to prevent duplicates
            adapter.clear();

            // view.visible and view.gone toggle the visibility of the list and the empty message
            if (tasks.isEmpty()) {
                tvNoTasks.setVisibility(View.VISIBLE);
                listViewTasks.setVisibility(View.GONE);
            } else {
                tvNoTasks.setVisibility(View.GONE);
                listViewTasks.setVisibility(View.VISIBLE);
                // this checks the isCompleted field on each task and add a tick or bullet point to the title. This isnt implemented yet so it isnt used
                for (Task t : tasks) {
                    String status = t.isCompleted ? "✓ " : "• ";
                    adapter.add(status + t.title);
                }
            }
            // finally the ListView is notified that the data has changed and that it needs to be refreshed
            adapter.notifyDataSetChanged();
        });
    }
}