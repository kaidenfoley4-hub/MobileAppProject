package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;          // Maps to SUMMARY in .ics
    public String description;    // Maps to DESCRIPTION in .ics
    public String location;       // Maps to LOCATION in .ics
    public long startTime;        // Maps to DTSTART - replaces dueDate, includes time
    public long endTime;          // Maps to DTEND in .ics
    public boolean isCompleted;
    public String uid;            // Unique ID required by .ics format

    public Task(String title, String description, String location,
                long startTime, long endTime) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = false;
        // Generate a unique ID for each task
        this.uid = java.util.UUID.randomUUID().toString();
    }
}
