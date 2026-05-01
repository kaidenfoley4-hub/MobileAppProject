package com.example.myapplication;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RecurrenceUtils {

    public static final String FREQ_NONE = "NONE";
    public static final String FREQ_WEEKLY = "WEEKLY";
    public static final String FREQ_MONTHLY = "MONTHLY";

    public static final int DEFAULT_WINDOW_WEEKS = 8;

    private RecurrenceUtils() {
    }

    public static List<Task> expandWeeklyOccurrences(List<Task> tasks, long windowStart, long windowEnd) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> expanded = new ArrayList<>();
        for (Task task : tasks) {
            if (!isRecurring(task)) {
                expanded.add(task);
                continue;
            }

            long duration = Math.max(0L, task.endTime - task.startTime);
            long firstStart = computeNextOccurrence(task, windowStart);

            if (firstStart <= 0L || firstStart > windowEnd) {
                expanded.add(task);
                continue;
            }

            long current = firstStart;
            while (current <= windowEnd) {
                long end = current + duration;
                expanded.add(copyOccurrence(task, current, end));
                long next = computeNextOccurrence(task, current + 1L);
                if (next <= current) {
                    break;
                }
                current = next;
            }
        }

        expanded.sort(Comparator.comparingLong(t -> t.startTime));
        return expanded;
    }

    public static List<Task> filterTasksForDay(List<Task> tasks, long dayStart, long dayEnd) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks) {
            if (!isRecurring(task)) {
                if (task.startTime >= dayStart && task.startTime <= dayEnd) {
                    filtered.add(task);
                }
                continue;
            }

            long occurrence = computeNextOccurrence(task, dayStart);
            if (occurrence >= dayStart && occurrence <= dayEnd) {
                long duration = Math.max(0L, task.endTime - task.startTime);
                filtered.add(copyOccurrence(task, occurrence, occurrence + duration));
            }
        }

        filtered.sort(Comparator.comparingLong(t -> t.startTime));
        return filtered;
    }

    public static long computeNextWeeklyOccurrence(long baseStart, int intervalWeeks, long afterTimeMillis) {
        Task temp = new Task("", "", "", baseStart, baseStart + 1L);
        temp.recurrenceFrequency = FREQ_WEEKLY;
        temp.recurrenceInterval = Math.max(intervalWeeks, 1);
        return computeNextOccurrence(temp, afterTimeMillis);
    }

    public static long computeNextOccurrence(Task task, long afterTimeMillis) {
        if (task == null || task.startTime <= 0L) {
            return -1L;
        }

        if (!isRecurring(task)) {
            return task.startTime >= afterTimeMillis ? task.startTime : -1L;
        }

        String frequency = task.recurrenceFrequency == null ? FREQ_NONE : task.recurrenceFrequency;
        int interval = Math.max(task.recurrenceInterval, 1);

        if (FREQ_WEEKLY.equals(frequency)) {
            long intervalMillis = TimeUnit.DAYS.toMillis(7L * interval);
            if (task.startTime >= afterTimeMillis) {
                return task.startTime;
            }
            long diff = afterTimeMillis - task.startTime;
            long steps = (diff + intervalMillis - 1L) / intervalMillis;
            return task.startTime + (steps * intervalMillis);
        }

        if (FREQ_MONTHLY.equals(frequency)) {
            return computeNextMonthlyOccurrence(task.startTime, interval, afterTimeMillis);
        }

        return -1L;
    }

    public static boolean isRecurring(Task task) {
        return task != null
                && task.recurrenceFrequency != null
                && !FREQ_NONE.equals(task.recurrenceFrequency);
    }

    private static long computeNextMonthlyOccurrence(long baseStart, int intervalMonths, long afterTimeMillis) {
        if (baseStart >= afterTimeMillis) {
            return baseStart;
        }

        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(baseStart);

        Calendar cursor = (Calendar) base.clone();
        int guard = 0;
        while (cursor.getTimeInMillis() < afterTimeMillis && guard < 120) {
            cursor.add(Calendar.MONTH, intervalMonths);
            guard++;
        }

        return cursor.getTimeInMillis();
    }

    private static Task copyOccurrence(Task base, long start, long end) {
        Task occurrence = new Task(
                base.title,
                base.description,
                base.location,
                start,
                end,
                base.isCompleted,
                base.uid,
                base.folder
        );
        occurrence.id = base.id;
        occurrence.recurrenceFrequency = base.recurrenceFrequency;
        occurrence.recurrenceInterval = base.recurrenceInterval;
        return occurrence;
    }
}
