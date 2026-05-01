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


    public String folder;
    public String tags;

    public String recurrenceFrequency;
    public int recurrenceInterval;
    public long recurrenceEndTime;


    public Task(String title, String description, String location,
                long startTime, long endTime, boolean isCompleted, String uid, String folder, String tags) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = isCompleted;
        this.uid = uid;
        this.folder = folder;
        this.tags = tags;
        this.recurrenceFrequency = RecurrenceUtils.FREQ_NONE;
        this.recurrenceInterval = 1;
        this.recurrenceEndTime = -1L;
    }


    @Ignore
    public Task(String title, String description, String location,
                long startTime, long endTime, String folder) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = false;
        this.uid = java.util.UUID.randomUUID().toString();
        this.folder = folder;
        this.tags = "";
        this.recurrenceFrequency = RecurrenceUtils.FREQ_NONE;
        this.recurrenceInterval = 1;
        this.recurrenceEndTime = -1L;
    }


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
        this.folder = "General";
        this.tags = "";
        this.recurrenceFrequency = RecurrenceUtils.FREQ_NONE;
        this.recurrenceInterval = 1;
        this.recurrenceEndTime = -1L;
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

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}