package com.example.myapplication;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import java.util.HashMap;

public class CalendarActivity extends AppCompatActivity {

    CalendarView calendarView;
    TextView eventText;

    HashMap<String, String> eventMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        eventText = findViewById(R.id.eventText);

        eventMap.put("3/3/2026", "Team Meeting at 3PM");
        eventMap.put("5/3/2026", "Project Deadline");
        eventMap.put("10/3/2026", "Client Presentation");

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {

            String selectedDate =
                    dayOfMonth + "/" + (month + 1) + "/" + year;

            if (eventMap.containsKey(selectedDate)) {
                eventText.setText("Event: " + eventMap.get(selectedDate));
            } else {
                eventText.setText("No events for this date");
            }
        });
    }
}