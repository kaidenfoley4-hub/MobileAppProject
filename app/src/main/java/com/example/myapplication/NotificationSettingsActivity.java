package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashMap;
import java.util.Map;

public class NotificationSettingsActivity extends AppCompatActivity {

    private final Map<Integer, Integer> leadTimeOptions = new HashMap<>();
    // toggle and chips make up the user-facing reminder configuration
    private SwitchMaterial switchNotifications;
    private ChipGroup leadTimeGroup;
    private TextView tvLeadTimeSummary;
    private ActivityResultLauncher<String> permissionLauncher;
    private boolean pendingEnableRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchNotifications = findViewById(R.id.switchNotifications);
        leadTimeGroup = findViewById(R.id.groupLeadTimes);
        tvLeadTimeSummary = findViewById(R.id.tvLeadTimeSummary);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (pendingEnableRequest) {
                        pendingEnableRequest = false;
                        if (isGranted) {
                            applyNotificationToggle(true);
                        } else {
                            switchNotifications.setChecked(false);
                            Toast.makeText(this,
                                    "Notifications remain off without permission",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        seedLeadTimeOptions();
        // keep the chip set and summary text in sync with the saved preference
        boolean enabled = NotificationPreferenceManager.areNotificationsEnabled(this);
        int selectedMinutes = NotificationPreferenceManager.getLeadTimeMinutes(this);

        switchNotifications.setChecked(enabled);
        int normalizedMinutes = setLeadTimeSelection(selectedMinutes);
        updateLeadTimeSummary(normalizedMinutes);
        setLeadTimeGroupEnabled(enabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !ensureNotificationPermission()) {
                pendingEnableRequest = true;
                switchNotifications.setChecked(false);
                return;
            }
            applyNotificationToggle(isChecked);
        });

        leadTimeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Integer minutes = leadTimeOptions.get(checkedIds.get(0));
            if (minutes != null) {
                NotificationPreferenceManager.setLeadTimeMinutes(this, minutes);
                updateLeadTimeSummary(minutes);
                // every lead-time change needs the alarms to be rescheduled immediately
                TaskReminderScheduler.refreshAll(getApplicationContext());
            }
        });
    }

    private void seedLeadTimeOptions() {
        leadTimeOptions.put(R.id.chipLead15, 15);
        leadTimeOptions.put(R.id.chipLead30, 30);
        leadTimeOptions.put(R.id.chipLead60, 60);
        leadTimeOptions.put(R.id.chipLead360, 360);
        leadTimeOptions.put(R.id.chipLead1440, 1440);
    }

    private int setLeadTimeSelection(int minutes) {
        int targetId = -1;
        for (Map.Entry<Integer, Integer> entry : leadTimeOptions.entrySet()) {
            if (entry.getValue() == minutes) {
                targetId = entry.getKey();
                break;
            }
        }

        if (targetId == -1) {
            targetId = R.id.chipLead1440;
            minutes = NotificationPreferenceManager.getDefaultLeadTimeMinutes();
            NotificationPreferenceManager.setLeadTimeMinutes(this, minutes);
        }

        leadTimeGroup.check(targetId);
        return minutes;
    }

    private void updateLeadTimeSummary(int minutes) {
        String label;
        if (minutes < 60) {
            label = minutes + " minute" + (minutes == 1 ? "" : "s");
        } else {
            int hours = minutes / 60;
            label = hours + " hour" + (hours == 1 ? "" : "s");
        }
        tvLeadTimeSummary.setText("Notify " + label + " before start time");
    }

    private void setLeadTimeGroupEnabled(boolean enabled) {
        leadTimeGroup.setEnabled(enabled);
        for (int i = 0; i < leadTimeGroup.getChildCount(); i++) {
            Chip chip = (Chip) leadTimeGroup.getChildAt(i);
            chip.setEnabled(enabled);
        }
    }

    private boolean ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        return granted;
    }

    private void applyNotificationToggle(boolean enabled) {
        NotificationPreferenceManager.setNotificationsEnabled(this, enabled);
        setLeadTimeGroupEnabled(enabled);
        // flip all pending reminders on/off to match the new toggle state
        TaskReminderScheduler.refreshAll(getApplicationContext());
    }
}
