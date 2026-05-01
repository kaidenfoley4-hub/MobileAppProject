package com.example.myapplication;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class IcsImporter {

    public static List<Task> importFromUri(Context context, Uri uri) throws IOException {
        if (uri == null) {
            return Collections.emptyList();
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return Collections.emptyList();
            }
            return parseIcs(new BufferedReader(new InputStreamReader(inputStream)));
        }
    }

    private static List<Task> parseIcs(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        StringBuilder current = new StringBuilder();

        // Unfold lines that wrap with a leading space or tab.
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                current.append(line.substring(1));
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(line);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }

        List<Task> tasks = new ArrayList<>();
        Map<String, String> eventProps = null;
        Map<String, Map<String, String>> eventParams = null;

        for (String rawLine : lines) {
            if ("BEGIN:VEVENT".equalsIgnoreCase(rawLine)) {
                eventProps = new HashMap<>();
                eventParams = new HashMap<>();
                continue;
            }

            if ("END:VEVENT".equalsIgnoreCase(rawLine)) {
                if (eventProps != null) {
                    Task task = buildTask(eventProps, eventParams);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
                eventProps = null;
                eventParams = null;
                continue;
            }

            if (eventProps == null) {
                continue;
            }

            int colonIndex = rawLine.indexOf(':');
            if (colonIndex == -1) {
                continue;
            }

            String nameAndParams = rawLine.substring(0, colonIndex);
            String value = rawLine.substring(colonIndex + 1);

            String[] nameParts = nameAndParams.split(";");
            String propName = nameParts[0].toUpperCase(Locale.US);

            eventProps.put(propName, unescapeValue(value));

            if (nameParts.length > 1) {
                Map<String, String> params = new HashMap<>();
                for (int i = 1; i < nameParts.length; i++) {
                    String[] kv = nameParts[i].split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].toUpperCase(Locale.US), kv[1]);
                    }
                }
                eventParams.put(propName, params);
            }
        }

        return tasks;
    }

    private static Task buildTask(Map<String, String> props, Map<String, Map<String, String>> params) {
        String title = props.getOrDefault("SUMMARY", "Imported task").trim();
        String description = props.getOrDefault("DESCRIPTION", "").trim();
        String location = props.getOrDefault("LOCATION", "").trim();
        String uid = props.getOrDefault("UID", java.util.UUID.randomUUID().toString());

        Long start = parseDateTime(props.get("DTSTART"), params.get("DTSTART"));
        Long end = parseDateTime(props.get("DTEND"), params.get("DTEND"));
        if (end == null) {
            end = parseDateTime(props.get("DUE"), params.get("DUE"));
        }

        if (start == null) {
            return null;
        }

        if (end == null || end <= start) {
            end = start + 60 * 60 * 1000;
        }

        boolean isCompleted = "COMPLETED".equalsIgnoreCase(props.get("STATUS"));
        return new Task(title, description, location, start, end, isCompleted, uid, "General");
    }

    private static Long parseDateTime(String value, Map<String, String> params) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String raw = value.trim();
        String tzid = params != null ? params.get("TZID") : null;
        boolean isDateOnly = (params != null && "DATE".equalsIgnoreCase(params.get("VALUE")))
                || (!raw.contains("T") && raw.length() == 8);
        boolean isUtc = raw.endsWith("Z");

        if (isUtc) {
            raw = raw.substring(0, raw.length() - 1);
        }

        String[] patterns = isDateOnly
                ? new String[]{"yyyyMMdd"}
                : new String[]{"yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmm"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                if (isUtc) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                } else if (tzid != null && !tzid.isEmpty()) {
                    sdf.setTimeZone(TimeZone.getTimeZone(tzid));
                }
                Date parsed = sdf.parse(raw);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private static String unescapeValue(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }
}
