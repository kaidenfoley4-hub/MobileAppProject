package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPreferenceManager {

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_ENABLED = "notifications_enabled";
    private static final String KEY_LEAD_TIME_MIN = "notification_lead_time_min";

    // default to 24 hours before start time
    private static final int DEFAULT_LEAD_TIME_MIN = 24 * 60;

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        // separate prefs namespace keeps reminder settings isolated from other toggles like theme
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public static void setLeadTimeMinutes(Context context, int minutes) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_LEAD_TIME_MIN, minutes).apply();
    }

    public static int getLeadTimeMinutes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LEAD_TIME_MIN, DEFAULT_LEAD_TIME_MIN);
    }

    public static int getDefaultLeadTimeMinutes() {
        return DEFAULT_LEAD_TIME_MIN;
    }
}
