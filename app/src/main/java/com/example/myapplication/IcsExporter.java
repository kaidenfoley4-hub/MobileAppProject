package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class IcsExporter {

    // builds the .ics file content as a string from the task list
    public static String generateIcs(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();

        // has to be UTC or outlook gets the times wrong
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        sb.append("BEGIN:VCALENDAR\n");
        sb.append("VERSION:2.0\n");
        sb.append("PRODID:-//MyCalendarApp//EN\n");

        for (Task task : tasks) {
            sb.append("BEGIN:VEVENT\n");

            // uid makes sure outlook doesn't make duplicates if we export twice
            sb.append("UID:").append(task.uid).append("\n");
            sb.append("SUMMARY:").append(task.title).append("\n");
            sb.append("DESCRIPTION:").append(
                    task.description != null ? task.description : "").append("\n");
            // description and location may be empty so fall back to blank strings
            sb.append("LOCATION:").append(
                    task.location != null ? task.location : "").append("\n");
            sb.append("DTSTART:").append(sdf.format(new Date(task.startTime))).append("\n");
            sb.append("DTEND:").append(sdf.format(new Date(task.endTime))).append("\n");
            sb.append("STATUS:").append(
                    task.isCompleted ? "COMPLETED" : "CONFIRMED").append("\n");
            sb.append("END:VEVENT\n");
        }

        sb.append("END:VCALENDAR");
        return sb.toString();
    }

    public static void exportToFile(Context context, List<Task> tasks) {
        // if there is nothing to export
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        String icsContent = generateIcs(tasks);
        File file = new File(context.getExternalFilesDir(null), "tasks_export.ics");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(icsContent);

            // FileProvider handles turning the filep ath into something
            // that outlook can open
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );

            // opens the share widget so the usre can pick an app
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/calendar");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Export to Outlook"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
