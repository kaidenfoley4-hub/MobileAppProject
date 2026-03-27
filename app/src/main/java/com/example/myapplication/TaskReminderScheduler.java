package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class TaskReminderScheduler {

    public static final String CHANNEL_ID = "task_reminder_channel";
    private static final String TAG = "TaskReminderScheduler";

    private static final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private TaskReminderScheduler() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Context appContext = context.getApplicationContext();
        NotificationManager manager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.task_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(appContext.getString(R.string.task_notification_channel_desc));
        manager.createNotificationChannel(channel);
    }

    public static void updateReminder(Context context, Task task) {
        if (task == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (!NotificationPreferenceManager.areNotificationsEnabled(appContext) || task.isCompleted) {
            cancelReminder(appContext, task);
            return;
        }
        if (task.startTime <= 0) {
            cancelReminder(appContext, task);
            return;
        }

        long leadMinutes = NotificationPreferenceManager.getLeadTimeMinutes(appContext);
        long triggerAtMillis = task.startTime - TimeUnit.MINUTES.toMillis(leadMinutes);
        long now = System.currentTimeMillis();
        if (triggerAtMillis <= now) {
            // ignore anything already in the past so we do not spam old tasks
            cancelReminder(appContext, task);
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(appContext, task);
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarms not permitted; scheduling inexact reminder instead");
            // still schedule something so the user gets a best-effort reminder
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException ex) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", ex);
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelReminder(Context context, Task task) {
        if (task == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        PendingIntent pendingIntent = buildPendingIntent(appContext, task);
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
    }

    public static void refreshAll(Context context) {
        Context appContext = context.getApplicationContext();
        ioExecutor.execute(() -> {
            List<Task> tasks = AppDatabase.getInstance(appContext).taskDao().getAllTasksSnapshot();
            boolean enabled = NotificationPreferenceManager.areNotificationsEnabled(appContext);
            for (Task task : tasks) {
                if (enabled) {
                    updateReminder(appContext, task);
                } else {
                    cancelReminder(appContext, task);
                }
            }
            // keeps the executor from leaking threads when the app is backgrounded for a while
        });
    }

    private static PendingIntent buildPendingIntent(Context context, Task task) {
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.setAction("com.example.myapplication.ACTION_TASK_REMINDER_" + computeRequestCode(task));
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_START, task.startTime);
        intent.putExtra(TaskReminderReceiver.EXTRA_NOTIFICATION_ID, computeRequestCode(task));

        int requestCode = computeRequestCode(task);
        // immutable + update current ensures edits overwrite the old pending intent
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int computeRequestCode(Task task) {
        if (task.id != 0) {
            return task.id;
        }
        return Math.abs(task.hashCode());
    }
}
