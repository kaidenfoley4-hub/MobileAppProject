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

        SimpleDateFormat stampFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault());
        stampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dtStamp = stampFormat.format(new Date());

        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//MyCalendarApp//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");

        for (Task task : tasks) {
            sb.append("BEGIN:VEVENT\r\n");

            // uid makes sure outlook doesn't make duplicates if we export twice
            sb.append(foldLine("UID:" + safeValue(task.uid))).append("\r\n");
            sb.append(foldLine("DTSTAMP:" + dtStamp)).append("\r\n");
            sb.append(foldLine("SUMMARY:" + safeValue(task.title))).append("\r\n");
            // description and location may be empty so fall back to blank strings
            sb.append(foldLine("DESCRIPTION:" + safeValue(task.description))).append("\r\n");
            sb.append(foldLine("LOCATION:" + safeValue(task.location))).append("\r\n");
            sb.append("DTSTART:").append(sdf.format(new Date(task.startTime))).append("\r\n");
            sb.append("DTEND:").append(sdf.format(new Date(task.endTime))).append("\r\n");
            if (RecurrenceUtils.isRecurring(task)) {
                int interval = Math.max(task.recurrenceInterval, 1);
                String rrule = "RRULE:FREQ=" + task.recurrenceFrequency + ";INTERVAL=" + interval;
                sb.append(foldLine(rrule)).append("\r\n");
            }
            sb.append("STATUS:").append(task.isCompleted ? "COMPLETED" : "CONFIRMED").append("\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
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

    private static String safeValue(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    private static String foldLine(String line) {
        if (line.length() <= 75) {
            return line;
        }

        StringBuilder folded = new StringBuilder();
        int index = 0;
        while (index < line.length()) {
            int next = Math.min(index + 75, line.length());
            if (index == 0) {
                folded.append(line, index, next);
            } else {
                folded.append("\r\n ").append(line, index, next);
            }
            index = next;
        }
        return folded.toString();
    }
}
