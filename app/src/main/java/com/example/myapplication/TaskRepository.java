package com.example.myapplication;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {

    private final TaskDao taskDao;
    private final ExecutorService executor;
    private final Context appContext;

    public TaskRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        taskDao = db.taskDao();
        executor = Executors.newSingleThreadExecutor();
        appContext = application.getApplicationContext();
    }

    public LiveData<List<Task>> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public LiveData<List<Task>> getTasksByFolder(String folder) {
        return taskDao.getTasksByFolder(folder);
    }

    public LiveData<List<Task>> getTasksForDay(long start, long end) {
        return taskDao.getTasksForDay(start, end);
    }

    public LiveData<List<Task>> getTasksForDayByFolder(long start, long end, String folder) {
        return taskDao.getTasksForDayByFolder(start, end, folder);
    }

    public LiveData<List<Task>> getPendingTasks() {
        return taskDao.getPendingTasks();
    }

    public LiveData<List<Task>> getPendingTasksByFolder(String folder) {
        return taskDao.getPendingTasksByFolder(folder);
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks();
    }

    public LiveData<List<Task>> getCompletedTasksByFolder(String folder) {
        return taskDao.getCompletedTasksByFolder(folder);
    }

    public LiveData<Task> getTaskById(int taskId) {
        return taskDao.getTaskById(taskId);
    }

    public void insert(Task task) {
        executor.execute(() -> {
            long newId = taskDao.insert(task);
            task.id = (int) newId;
            TaskReminderScheduler.updateReminder(appContext, task);
        });
    }

    public void delete(Task task) {
        executor.execute(() -> {
            taskDao.delete(task);
            TaskReminderScheduler.cancelReminder(appContext, task);
        });
    }

    public void update(Task task) {
        executor.execute(() -> {
            taskDao.update(task);
            TaskReminderScheduler.updateReminder(appContext, task);
        });
    }
}