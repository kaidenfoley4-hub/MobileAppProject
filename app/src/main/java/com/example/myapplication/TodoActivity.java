package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TodoActivity extends AppCompatActivity {

    private TextInputEditText editTask;
    private TextView tvSelectedDate;
    private TextView tvSelectedStartTime;
    private TextView tvSelectedEndTime;
    private Spinner spinnerTaskFolder;
    private Spinner spinnerFilterFolder;
    private TaskViewModel taskViewModel;

    private TaskAdapter pendingAdapter;
    private TaskAdapter completedAdapter;

    private List<Task> pendingTasks = new ArrayList<>();
    private List<Task> completedTasks = new ArrayList<>();
    private List<Task> currentTasks = new ArrayList<>();

    private LiveData<List<Task>> pendingTasksLiveData;
    private LiveData<List<Task>> completedTasksLiveData;
    private LiveData<List<Task>> allTasksLiveData;

    private final String[] taskFolders = {"General", "Study", "Work", "Personal", "Shopping"};
    private final String[] filterFolders = {"All", "General", "Study", "Work", "Personal", "Shopping"};

    private long selectedDateMillis = -1;
    private long selectedStartTimeMillis = -1;
    private long selectedEndTimeMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editTask = findViewById(R.id.editTask);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedStartTime = findViewById(R.id.tvSelectedStartTime);
        tvSelectedEndTime = findViewById(R.id.tvSelectedEndTime);
        spinnerTaskFolder = findViewById(R.id.spinnerTaskFolder);
        spinnerFilterFolder = findViewById(R.id.spinnerFilterFolder);

        ListView pendingListView = findViewById(R.id.listViewPendingTasks);
        ListView completedListView = findViewById(R.id.listViewCompletedTasks);

        MaterialCardView btnPickDate = findViewById(R.id.btnPickDate);
        MaterialCardView cardPickStartTime = findViewById(R.id.cardPickStartTime);
        MaterialCardView cardPickEndTime = findViewById(R.id.cardPickEndTime);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnImport = findViewById(R.id.btnImport);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        setupSpinners();

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

        observeTasks("All", pendingListView, completedListView);

        spinnerFilterFolder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFolder = parent.getItemAtPosition(position).toString();
                observeTasks(selectedFolder, pendingListView, completedListView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                cal.set(year, month, day, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                selectedDateMillis = cal.getTimeInMillis();

                String dateStr = day + "/" + (month + 1) + "/" + year;
                tvSelectedDate.setText("Due: " + dateStr);

                selectedStartTimeMillis = -1;
                selectedEndTimeMillis = -1;
                tvSelectedStartTime.setText("No time selected");
                tvSelectedEndTime.setText("No time selected");

            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        cardPickStartTime.setOnClickListener(v -> {
            if (selectedDateMillis == -1) {
                Toast.makeText(this, "Please pick a date first", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                Calendar combined = Calendar.getInstance();
                combined.setTimeInMillis(selectedDateMillis);
                combined.set(Calendar.HOUR_OF_DAY, hour);
                combined.set(Calendar.MINUTE, minute);
                combined.set(Calendar.SECOND, 0);
                combined.set(Calendar.MILLISECOND, 0);
                selectedStartTimeMillis = combined.getTimeInMillis();

                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                tvSelectedStartTime.setText(timeStr);

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        cardPickEndTime.setOnClickListener(v -> {
            if (selectedStartTimeMillis == -1) {
                Toast.makeText(this, "Please pick a start time first", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                Calendar combined = Calendar.getInstance();
                combined.setTimeInMillis(selectedDateMillis);
                combined.set(Calendar.HOUR_OF_DAY, hour);
                combined.set(Calendar.MINUTE, minute);
                combined.set(Calendar.SECOND, 0);
                combined.set(Calendar.MILLISECOND, 0);
                selectedEndTimeMillis = combined.getTimeInMillis();

                if (selectedEndTimeMillis <= selectedStartTimeMillis) {
                    Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                    selectedEndTimeMillis = -1;
                    tvSelectedEndTime.setText("No time selected");
                    return;
                }

                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                tvSelectedEndTime.setText(timeStr);

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        btnAdd.setOnClickListener(v -> {
            String title = editTask.getText().toString().trim();
            String selectedFolder = spinnerTaskFolder.getSelectedItem().toString();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDateMillis == -1) {
                Toast.makeText(this, "Please pick a due date", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedStartTimeMillis == -1) {
                Toast.makeText(this, "Please pick a start time", Toast.LENGTH_SHORT).show();
                return;
            }

            long endMillis = selectedEndTimeMillis != -1
                    ? selectedEndTimeMillis
                    : selectedStartTimeMillis + 3600000;

            Task task = new Task(title, "", "", selectedStartTimeMillis, endMillis, selectedFolder);
            taskViewModel.insert(task);

            editTask.setText("");
            tvSelectedDate.setText("No date selected");
            tvSelectedStartTime.setText("No time selected");
            tvSelectedEndTime.setText("No time selected");
            selectedDateMillis = -1;
            selectedStartTimeMillis = -1;
            selectedEndTimeMillis = -1;
            spinnerTaskFolder.setSelection(0);
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

        btnImport.setOnClickListener(v -> {
            // Request code for selecting a ICS file.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("downloads/ics");
            IcsImporter.launch(intent);
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> taskFolderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taskFolders);
        taskFolderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskFolder.setAdapter(taskFolderAdapter);

        ArrayAdapter<String> filterFolderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterFolders);
        filterFolderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterFolder.setAdapter(filterFolderAdapter);
    }

    private void observeTasks(String folder, ListView pendingListView, ListView completedListView) {
        if (pendingTasksLiveData != null) {
            pendingTasksLiveData.removeObservers(this);
        }

        if (completedTasksLiveData != null) {
            completedTasksLiveData.removeObservers(this);
        }

        if (allTasksLiveData != null) {
            allTasksLiveData.removeObservers(this);
        }

        if (folder.equals("All")) {
            pendingTasksLiveData = taskViewModel.getPendingTasks();
            completedTasksLiveData = taskViewModel.getCompletedTasks();
            allTasksLiveData = taskViewModel.getAllTasks();
        } else {
            pendingTasksLiveData = taskViewModel.getPendingTasksByFolder(folder);
            completedTasksLiveData = taskViewModel.getCompletedTasksByFolder(folder);
            allTasksLiveData = taskViewModel.getTasksByFolder(folder);
        }

        pendingTasksLiveData.observe(this, tasks -> {
            pendingTasks = tasks;
            pendingAdapter.clear();
            pendingAdapter.addAll(tasks);
            pendingAdapter.notifyDataSetChanged();
            setListViewHeightBasedOnChildren(pendingListView);
        });

        completedTasksLiveData.observe(this, tasks -> {
            completedTasks = tasks;
            completedAdapter.clear();
            completedAdapter.addAll(tasks);
            completedAdapter.notifyDataSetChanged();
            setListViewHeightBasedOnChildren(completedListView);
        });

        allTasksLiveData.observe(this, tasks -> currentTasks = tasks);
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();

        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);

            if (listItem.getLayoutParams() == null) {
                listItem.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
            }

            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * Math.max(listAdapter.getCount() - 1, 0));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}