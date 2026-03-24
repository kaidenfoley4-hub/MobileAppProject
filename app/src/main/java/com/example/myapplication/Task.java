package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public String location;
    public long startTime;
    public long endTime;
    public boolean isCompleted;
    public String uid;

    // Room uses this constructor
    public Task(String title, String description, String location,
                long startTime, long endTime, boolean isCompleted, String uid) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = isCompleted;
        this.uid = uid;
    }

    // Use this one when creating a new task in the app
    @Ignore
    public Task(String title, String description, String location,
                long startTime, long endTime) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = false;
        this.uid = java.util.UUID.randomUUID().toString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}