package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class TodoActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "todo_prefs";
    private static final String KEY_TASKS = "tasks_json";

    private EditText editTask;
    private ArrayList<String> tasks;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editTask = findViewById(R.id.editTask);
        ListView listView = findViewById(R.id.listViewTasks);

        tasks = loadTasks();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks);
        listView.setAdapter(adapter);

        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String text = editTask.getText().toString().trim();
            if (!text.isEmpty()) {
                tasks.add(text);
                adapter.notifyDataSetChanged();
                editTask.setText("");
                saveTasks();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            tasks.remove(position);
            adapter.notifyDataSetChanged();
            saveTasks();
        });

        editTask.setOnLongClickListener(v -> {
            editTask.setText("");
            return true;
        });
    }

    private void saveTasks() {
        JSONArray arr = new JSONArray();
        for (String t : tasks) arr.put(t);

        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit().putString(KEY_TASKS, arr.toString()).apply();
    }

    private ArrayList<String> loadTasks() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = sp.getString(KEY_TASKS, "[]");

        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (JSONException e) {
        }
        return list;
    }
}