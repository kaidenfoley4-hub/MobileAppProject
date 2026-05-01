package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.concurrent.TimeUnit;

public class TodoActivity extends AppCompatActivity {

    private TextInputEditText editTask;
    private TextInputEditText editTaskTags;
    private TextInputEditText editSearch;
    private TextView tvSelectedDate;
    private TextView tvSelectedStartTime;
    private TextView tvSelectedEndTime;
    private Spinner spinnerTaskFolder;
    private Spinner spinnerFilterFolder;
    private Spinner spinnerRecurrenceFrequency;
    private TaskViewModel taskViewModel;

    private TaskAdapter pendingAdapter;
    private TaskAdapter completedAdapter;

    private List<Task> pendingTasks = new ArrayList<>();
    private List<Task> completedTasks = new ArrayList<>();
    private List<Task> currentTasks = new ArrayList<>();

    // 保留从数据库取出的全量数据，搜索时基于它过滤
    private List<Task> allPendingFromDb = new ArrayList<>();
    private List<Task> allCompletedFromDb = new ArrayList<>();

    private LiveData<List<Task>> pendingTasksLiveData;
    private LiveData<List<Task>> completedTasksLiveData;
    private LiveData<List<Task>> allTasksLiveData;

    private final String[] taskFolders = {"General", "Study", "Work", "Personal", "Shopping"};
    private final String[] filterFolders = {"All", "General", "Study", "Work", "Personal", "Shopping"};
    private final String[] recurrenceFrequencies = {"None", "Weekly", "Biweekly", "Monthly"};

    private long selectedDateMillis = -1;
    private long selectedStartTimeMillis = -1;
    private long selectedEndTimeMillis = -1;

    private String currentSearchKeyword = "";

    private ListView pendingListView;
    private ListView completedListView;

    private static final int REQUEST_OPEN_RESULT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editTask = findViewById(R.id.editTask);
        editTaskTags = findViewById(R.id.editTaskTags);
        editSearch = findViewById(R.id.editSearch);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedStartTime = findViewById(R.id.tvSelectedStartTime);
        tvSelectedEndTime = findViewById(R.id.tvSelectedEndTime);
        spinnerTaskFolder = findViewById(R.id.spinnerTaskFolder);
        spinnerFilterFolder = findViewById(R.id.spinnerFilterFolder);
        spinnerRecurrenceFrequency = findViewById(R.id.spinnerRecurrenceFrequency);

        pendingListView = findViewById(R.id.listViewPendingTasks);
        completedListView = findViewById(R.id.listViewCompletedTasks);

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

        observeTasks("All");

        spinnerFilterFolder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFolder = parent.getItemAtPosition(position).toString();
                observeTasks(selectedFolder);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = s.toString().trim().toLowerCase(Locale.getDefault());
                applySearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerRecurrenceFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
            String tagsInput = editTaskTags.getText().toString().trim();
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
            task.tags = normalizeTags(tagsInput);
            applyRecurrenceSelection(task, selectedStartTimeMillis);
            taskViewModel.insert(task);

            editTask.setText("");
            editTaskTags.setText("");
            tvSelectedDate.setText("No date selected");
            tvSelectedStartTime.setText("No time selected");
            tvSelectedEndTime.setText("No time selected");
            selectedDateMillis = -1;
            selectedStartTimeMillis = -1;
            selectedEndTimeMillis = -1;
            spinnerTaskFolder.setSelection(0);
            spinnerRecurrenceFrequency.setSelection(0);
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
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/calendar");
            startActivityForResult(intent, REQUEST_OPEN_RESULT);
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> taskFolderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taskFolders);
        taskFolderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskFolder.setAdapter(taskFolderAdapter);

        ArrayAdapter<String> filterFolderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterFolders);
        filterFolderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterFolder.setAdapter(filterFolderAdapter);

        ArrayAdapter<String> recurrenceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recurrenceFrequencies);
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrenceFrequency.setAdapter(recurrenceAdapter);
    }

    private void applyRecurrenceSelection(Task task, long startTime) {
        int frequencyIndex = spinnerRecurrenceFrequency.getSelectedItemPosition();
        if (frequencyIndex == 0) {
            task.recurrenceFrequency = RecurrenceUtils.FREQ_NONE;
            task.recurrenceInterval = 1;
            return;
        }

        if (frequencyIndex == 1) {
            task.recurrenceFrequency = RecurrenceUtils.FREQ_WEEKLY;
            task.recurrenceInterval = 1;
        } else if (frequencyIndex == 2) {
            task.recurrenceFrequency = RecurrenceUtils.FREQ_WEEKLY;
            task.recurrenceInterval = 2;
        } else {
            task.recurrenceFrequency = RecurrenceUtils.FREQ_MONTHLY;
            task.recurrenceInterval = 1;
        }
    }

    private String normalizeTags(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] parts = raw.split(",");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(t);
            }
        }
        return sb.toString();
    }

    private void observeTasks(String folder) {
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
            long windowStart = System.currentTimeMillis();
            long windowEnd = windowStart + TimeUnit.DAYS.toMillis(7L * RecurrenceUtils.DEFAULT_WINDOW_WEEKS);
            allPendingFromDb = RecurrenceUtils.expandWeeklyOccurrences(tasks, windowStart, windowEnd);
            applySearchFilter();
        });

        completedTasksLiveData.observe(this, tasks -> {
            long windowStart = System.currentTimeMillis();
            long windowEnd = windowStart + TimeUnit.DAYS.toMillis(7L * RecurrenceUtils.DEFAULT_WINDOW_WEEKS);
            allCompletedFromDb = RecurrenceUtils.expandWeeklyOccurrences(tasks, windowStart, windowEnd);
            applySearchFilter();
        });

        allTasksLiveData.observe(this, tasks -> currentTasks = tasks);
    }

    private void applySearchFilter() {
        pendingTasks = filterByKeyword(allPendingFromDb, currentSearchKeyword);
        completedTasks = filterByKeyword(allCompletedFromDb, currentSearchKeyword);

        pendingAdapter.clear();
        pendingAdapter.addAll(pendingTasks);
        pendingAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(pendingListView);

        completedAdapter.clear();
        completedAdapter.addAll(completedTasks);
        completedAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(completedListView);
    }

    private List<Task> filterByKeyword(List<Task> source, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return new ArrayList<>(source);
        }
        List<Task> result = new ArrayList<>();
        for (Task t : source) {
            String title = t.title == null ? "" : t.title.toLowerCase(Locale.getDefault());
            String tags = t.tags == null ? "" : t.tags.toLowerCase(Locale.getDefault());
            if (title.contains(keyword) || tags.contains(keyword)) {
                result.add(t);
            }
        }
        return result;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_OPEN_RESULT || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            Toast.makeText(this, "Unable to read the selected file", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            List<Task> importedTasks = IcsImporter.importFromUri(this, uri);
            if (importedTasks.isEmpty()) {
                Toast.makeText(this, "No tasks found in the selected file", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Task task : importedTasks) {
                taskViewModel.insert(task);
            }

            Toast.makeText(this, "Imported " + importedTasks.size() + " tasks", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to import tasks", Toast.LENGTH_SHORT).show();
        }
    }
}