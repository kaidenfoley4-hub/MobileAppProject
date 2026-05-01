package com.example.myapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(Task task);

    @Delete
    void delete(Task task);

    @Update
    void update(Task task);

    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :start AND :end ORDER BY isCompleted ASC, startTime ASC")
    LiveData<List<Task>> getTasksForDay(long start, long end);

    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :start AND :end AND folder = :folder ORDER BY isCompleted ASC, startTime ASC")
    LiveData<List<Task>> getTasksForDayByFolder(long start, long end, String folder);

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, startTime ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE folder = :folder ORDER BY isCompleted ASC, startTime ASC")
    LiveData<List<Task>> getTasksByFolder(String folder);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY startTime ASC")
    LiveData<List<Task>> getPendingTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND folder = :folder ORDER BY startTime ASC")
    LiveData<List<Task>> getPendingTasksByFolder(String folder);

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY startTime ASC")
    LiveData<List<Task>> getCompletedTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND folder = :folder ORDER BY startTime ASC")
    LiveData<List<Task>> getCompletedTasksByFolder(String folder);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskById(int taskId);

    @Query("SELECT * FROM tasks")
    List<Task> getAllTasksSnapshot();

    @Query("SELECT * FROM tasks WHERE folder = :folder")
    List<Task> getTasksSnapshotByFolder(String folder);
}