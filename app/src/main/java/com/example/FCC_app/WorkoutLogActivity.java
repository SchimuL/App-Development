package com.example.FCC_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class WorkoutLogActivity extends AppCompatActivity {

    private String loggedInPlayerTag;
    private final List<WorkoutLogEntry> workoutList = new ArrayList<>();
    private WorkoutLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_log);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        Toolbar toolbar = findViewById(R.id.workout_log_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.workout_log_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create the click listener
        WorkoutLogAdapter.OnItemClickListener listener = workoutKey -> {
            Intent intent = new Intent(WorkoutLogActivity.this, WorkoutDetailActivity.class);
            intent.putExtra(WorkoutDetailActivity.EXTRA_WORKOUT_KEY, workoutKey);
            startActivity(intent);
        };

        adapter = new WorkoutLogAdapter(workoutList, listener);
        recyclerView.setAdapter(adapter);

        if (loggedInPlayerTag != null) {
            loadWorkouts();
        } else {
            Toast.makeText(this, "Fehler: Kein Spieler-Tag gefunden", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWorkouts() {
        // Run data loading in background thread to keep UI smooth
        Executors.newSingleThreadExecutor().execute(() -> {
            final List<WorkoutLogEntry> tempLoadedList = new ArrayList<>();
            SharedPreferences prefs = getSharedPreferences("WorkoutLog", Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getKey().startsWith(loggedInPlayerTag + "_")) {
                    try {
                        JSONObject workoutJson = new JSONObject((String) entry.getValue());
                        long dateMillis = workoutJson.getLong("date");
                        String goal = workoutJson.getString("goal");
                        tempLoadedList.add(new WorkoutLogEntry(entry.getKey(), dateMillis, goal));
                    } catch (JSONException e) {
                        // Ignore malformed entries
                    }
                }
            }

            // Sort by date, newest first
            Collections.sort(tempLoadedList, (o1, o2) -> Long.compare(o2.dateMillis, o1.dateMillis));

            // Switch back to UI thread to update the adapter
            runOnUiThread(() -> {
                workoutList.clear();
                workoutList.addAll(tempLoadedList);
                adapter.notifyDataSetChanged();

                if (workoutList.isEmpty()) {
                    Toast.makeText(WorkoutLogActivity.this, "Noch keine Workouts aufgezeichnet", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // --- Inner Data Class for the Log Entry ---
    private static class WorkoutLogEntry {
        final String workoutKey;
        final long dateMillis;
        final String goal;

        WorkoutLogEntry(String workoutKey, long dateMillis, String goal) {
            this.workoutKey = workoutKey;
            this.dateMillis = dateMillis;
            this.goal = goal;
        }
    }

    // --- Inner Adapter for the RecyclerView ---
    private static class WorkoutLogAdapter extends RecyclerView.Adapter<WorkoutLogAdapter.WorkoutViewHolder> {
        
        public interface OnItemClickListener {
            void onItemClick(String workoutKey);
        }

        private final List<WorkoutLogEntry> workoutList;
        private final OnItemClickListener listener;

        WorkoutLogAdapter(List<WorkoutLogEntry> workoutList, OnItemClickListener listener) {
            this.workoutList = workoutList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_workout_log, parent, false);
            return new WorkoutViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
            WorkoutLogEntry entry = workoutList.get(position);
            holder.bind(entry, listener);
        }

        @Override
        public int getItemCount() {
            return workoutList.size();
        }

        static class WorkoutViewHolder extends RecyclerView.ViewHolder {
            private final TextView dateText;
            private final TextView goalText;
            private static final SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy", Locale.GERMANY);

            WorkoutViewHolder(@NonNull View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.workout_date_text);
                goalText = itemView.findViewById(R.id.workout_goal_text);
            }

            void bind(final WorkoutLogEntry entry, final OnItemClickListener listener) {
                dateText.setText(sdf.format(new Date(entry.dateMillis)));
                goalText.setText("Ziel: " + entry.goal);
                itemView.setOnClickListener(v -> listener.onItemClick(entry.workoutKey));
            }
        }
    }
}