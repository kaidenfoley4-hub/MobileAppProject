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
    void insert(Task task);

    @Delete
    void delete(Task task);

    @Update
    void update(Task task);

    // called when a date is clicked on the calendar
    // start and end are midnight and 11:59pm of the specific day represented as milliseconds
    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :start AND :end ORDER BY startTime ASC")
    LiveData<List<Task>> getTasksForDay(long start, long end);

    // pulls everything for the todo list, oldest tasks show up first
    @Query("SELECT * FROM tasks ORDER BY startTime ASC")
    LiveData<List<Task>> getAllTasks();
}
