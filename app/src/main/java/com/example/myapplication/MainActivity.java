package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // apply saved theme before setContentView so the screen
        // doesnt flash the wrong theme on startup
        applyTheme();

        setContentView(R.layout.activity_main);

        // wire up the toolbar so the menu shows
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationSettingsActivity.class);
            startActivity(intent);
        });

        MaterialCardView cardCalendar = findViewById(R.id.cardCalendar);
        cardCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        MaterialCardView cardTodo = findViewById(R.id.cardTodo);
        cardTodo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TodoActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // find the switch inside the menu item and set its initial state
        MenuItem item = menu.findItem(R.id.action_dark_mode);
        SwitchCompat darkModeSwitch = (SwitchCompat) item.getActionView();
        if (darkModeSwitch != null) {
            // adds a label so the user knows what the switch does
            darkModeSwitch.setText("🌙");
            darkModeSwitch.setChecked(ThemePreferenceManager.isDarkMode(this));
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ThemePreferenceManager.setDarkMode(this, isChecked);
                applyTheme();
            });
        }

        return true;
    }

    private void applyTheme() {
        if (ThemePreferenceManager.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}