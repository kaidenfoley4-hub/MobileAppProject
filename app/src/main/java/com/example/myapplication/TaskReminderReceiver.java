package com.example.myapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.Date;

public class TaskReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_TASK_START = "extra_task_start";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!NotificationPreferenceManager.areNotificationsEnabled(context)) {
            return;
        }

        String title = intent.getStringExtra(EXTRA_TASK_TITLE);
        long startTime = intent.getLongExtra(EXTRA_TASK_START, -1L);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis());

        Intent openIntent = new Intent(context, TodoActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String timeText;
        if (startTime > 0) {
            timeText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(startTime));
        } else {
            timeText = context.getString(R.string.task_notification_fallback_time);
        }

        String safeTitle = (title == null || title.trim().isEmpty())
                ? context.getString(R.string.task_notification_default_title)
                : title;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_bell)
                .setContentTitle(safeTitle)
                .setContentText(context.getString(R.string.task_notification_body, timeText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                // launching TodoActivity keeps the flow simple when the user taps the reminder
                .setContentIntent(contentIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        manager.notify(notificationId, builder.build());
    }
}
