package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditActivity extends AppCompatActivity {

    private TextInputEditText editTaskTitle;
    private TextView tvSelectedDate;
    private TextView tvSelectedStartTime;
    private TextView tvSelectedEndTime;
    private Spinner spinnerEditTaskFolder;
    private TaskViewModel taskViewModel;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final String[] taskFolders = {"General", "Study", "Work", "Personal", "Shopping"};

    private Task currentTask;
    private long selectedDateMillis = -1;
    private long selectedStartTimeMillis = -1;
    private long selectedEndTimeMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        editTaskTitle = findViewById(R.id.editTaskTitle);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedStartTime = findViewById(R.id.tvSelectedStartTime);
        tvSelectedEndTime = findViewById(R.id.tvSelectedEndTime);
        spinnerEditTaskFolder = findViewById(R.id.spinnerEditTaskFolder);

        MaterialCardView btnPickDate = findViewById(R.id.btnPickDate);
        MaterialCardView cardPickStartTime = findViewById(R.id.cardPickStartTime);
        MaterialCardView cardPickEndTime = findViewById(R.id.cardPickEndTime);
        Button btnSave = findViewById(R.id.btnSaveTask);
        Button btnDelete = findViewById(R.id.btnDeleteTask);

        setupFolderSpinner();

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        int taskId = getIntent().getIntExtra("TASK_ID", -1);

        if (taskId == -1) {
            Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task != null && currentTask == null) {
                currentTask = task;
                editTaskTitle.setText(task.title);
                selectedStartTimeMillis = task.startTime;
                selectedEndTimeMillis = task.endTime;
                selectedDateMillis = startOfDay(task.startTime);
                setFolderSelection(task.folder);
                updateDateLabel();
                updateTimeLabels();
            }
        });

        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();

            if (selectedStartTimeMillis != -1) {
                cal.setTimeInMillis(selectedStartTimeMillis);
            } else if (selectedDateMillis != -1) {
                cal.setTimeInMillis(selectedDateMillis);
            }

            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar pickedDate = Calendar.getInstance();
                pickedDate.set(year, month, day, 0, 0, 0);
                pickedDate.set(Calendar.MILLISECOND, 0);
                selectedDateMillis = pickedDate.getTimeInMillis();

                if (selectedStartTimeMillis != -1) {
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTimeInMillis(selectedStartTimeMillis);
                    Calendar adjustedStart = (Calendar) pickedDate.clone();
                    adjustedStart.set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY));
                    adjustedStart.set(Calendar.MINUTE, startCal.get(Calendar.MINUTE));
                    adjustedStart.set(Calendar.SECOND, 0);
                    adjustedStart.set(Calendar.MILLISECOND, 0);
                    selectedStartTimeMillis = adjustedStart.getTimeInMillis();
                }

                if (selectedEndTimeMillis != -1) {
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTimeInMillis(selectedEndTimeMillis);
                    Calendar adjustedEnd = (Calendar) pickedDate.clone();
                    adjustedEnd.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY));
                    adjustedEnd.set(Calendar.MINUTE, endCal.get(Calendar.MINUTE));
                    adjustedEnd.set(Calendar.SECOND, 0);
                    adjustedEnd.set(Calendar.MILLISECOND, 0);

                    long recalculatedEnd = adjustedEnd.getTimeInMillis();

                    if (selectedStartTimeMillis != -1 && recalculatedEnd <= selectedStartTimeMillis) {
                        selectedEndTimeMillis = -1;
                        Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                    } else {
                        selectedEndTimeMillis = recalculatedEnd;
                    }
                }

                updateDateLabel();
                updateTimeLabels();

            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        cardPickStartTime.setOnClickListener(v -> {
            if (selectedDateMillis == -1) {
                Toast.makeText(this, "Please pick a date first", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar cal = Calendar.getInstance();

            if (selectedStartTimeMillis != -1) {
                cal.setTimeInMillis(selectedStartTimeMillis);
            } else {
                cal.setTimeInMillis(selectedDateMillis);
            }

            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                Calendar combined = Calendar.getInstance();
                combined.setTimeInMillis(selectedDateMillis);
                combined.set(Calendar.HOUR_OF_DAY, hourOfDay);
                combined.set(Calendar.MINUTE, minute);
                combined.set(Calendar.SECOND, 0);
                combined.set(Calendar.MILLISECOND, 0);
                selectedStartTimeMillis = combined.getTimeInMillis();

                if (selectedEndTimeMillis != -1 && selectedEndTimeMillis <= selectedStartTimeMillis) {
                    selectedEndTimeMillis = -1;
                    Toast.makeText(this, "End time cleared because it was before the start time", Toast.LENGTH_SHORT).show();
                }

                updateTimeLabels();

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        cardPickEndTime.setOnClickListener(v -> {
            if (selectedStartTimeMillis == -1) {
                Toast.makeText(this, "Please pick a start time first", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar cal = Calendar.getInstance();

            if (selectedEndTimeMillis != -1) {
                cal.setTimeInMillis(selectedEndTimeMillis);
            } else {
                cal.setTimeInMillis(selectedStartTimeMillis + 3600000);
            }

            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                Calendar combined = Calendar.getInstance();
                combined.setTimeInMillis(selectedDateMillis);
                combined.set(Calendar.HOUR_OF_DAY, hourOfDay);
                combined.set(Calendar.MINUTE, minute);
                combined.set(Calendar.SECOND, 0);
                combined.set(Calendar.MILLISECOND, 0);

                long candidateEnd = combined.getTimeInMillis();

                if (candidateEnd <= selectedStartTimeMillis) {
                    Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedEndTimeMillis = candidateEnd;
                updateTimeLabels();

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            String selectedFolder = spinnerEditTaskFolder.getSelectedItem().toString();

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

            if (currentTask == null) {
                Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
                return;
            }

            long resolvedEndTime = selectedEndTimeMillis != -1
                    ? selectedEndTimeMillis
                    : selectedStartTimeMillis + 3600000;

            currentTask.title = title;
            currentTask.startTime = selectedStartTimeMillis;
            currentTask.endTime = resolvedEndTime;
            currentTask.folder = selectedFolder;

            taskViewModel.update(currentTask);

            Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            if (currentTask != null) {
                taskViewModel.delete(currentTask);
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void setupFolderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taskFolders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditTaskFolder.setAdapter(adapter);
    }

    private void setFolderSelection(String folder) {
        if (folder == null || folder.trim().isEmpty()) {
            spinnerEditTaskFolder.setSelection(0);
            return;
        }

        for (int i = 0; i < taskFolders.length; i++) {
            if (taskFolders[i].equals(folder)) {
                spinnerEditTaskFolder.setSelection(i);
                return;
            }
        }

        spinnerEditTaskFolder.setSelection(0);
    }

    private void updateDateLabel() {
        if (selectedDateMillis == -1) {
            tvSelectedDate.setText("No date selected");
        } else {
            tvSelectedDate.setText("Due: " + dateFormat.format(new Date(selectedDateMillis)));
        }
    }

    private void updateTimeLabels() {
        if (selectedStartTimeMillis == -1) {
            tvSelectedStartTime.setText("No time selected");
        } else {
            tvSelectedStartTime.setText(timeFormat.format(new Date(selectedStartTimeMillis)));
        }

        if (selectedEndTimeMillis == -1) {
            tvSelectedEndTime.setText("No time selected");
        } else {
            tvSelectedEndTime.setText(timeFormat.format(new Date(selectedEndTimeMillis)));
        }
    }

    private long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}