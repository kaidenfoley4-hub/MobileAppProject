package com.example.myapplication;

import android.app.Application;

public class PlannerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TaskReminderScheduler.createNotificationChannel(this);
        TaskReminderScheduler.refreshAll(this);
    }
}
