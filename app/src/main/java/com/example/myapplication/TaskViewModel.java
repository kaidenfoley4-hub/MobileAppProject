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

    // activities talk to this rather than the repository directly
    // and it also isn't affected by screen rotation
    public LiveData<List<Task>> getAllTasks() {
        return repository.getAllTasks();
    }

    public LiveData<List<Task>> getTasksForDay(long start, long end) {
        return repository.getTasksForDay(start, end);
    }

    public LiveData<List<Task>> getPendingTasks() {
        return repository.getPendingTasks();
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return repository.getCompletedTasks();
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