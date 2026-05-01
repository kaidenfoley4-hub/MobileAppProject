package com.example.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;

    public TaskViewModel(Application application) {
        super(application);
        repository = new TaskRepository(application);
    }

    public LiveData<List<Task>> getAllTasks() {
        return repository.getAllTasks();
    }

    public LiveData<List<Task>> getTasksByFolder(String folder) {
        return repository.getTasksByFolder(folder);
    }

    public LiveData<List<Task>> getTasksForDay(long start, long end) {
        return repository.getTasksForDay(start, end);
    }

    public LiveData<List<Task>> getTasksForDayByFolder(long start, long end, String folder) {
        return repository.getTasksForDayByFolder(start, end, folder);
    }

    public LiveData<List<Task>> getPendingTasks() {
        return repository.getPendingTasks();
    }

    public LiveData<List<Task>> getPendingTasksByFolder(String folder) {
        return repository.getPendingTasksByFolder(folder);
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return repository.getCompletedTasks();
    }

    public LiveData<List<Task>> getCompletedTasksByFolder(String folder) {
        return repository.getCompletedTasksByFolder(folder);
    }

    public void insert(Task task) {
        repository.insert(task);
    }

    public void delete(Task task) {
        repository.delete(task);
    }

    public void update(Task task) {
        repository.update(task);
    }

    public LiveData<Task> getTaskById(int taskId) {
        return repository.getTaskById(taskId);
    }
}