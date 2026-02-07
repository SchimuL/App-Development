package com.example.FCC_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_KEY = "WORKOUT_KEY";
    private String playerTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        Toolbar toolbar = findViewById(R.id.workout_detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView dateText = findViewById(R.id.detail_workout_date);
        TextView goalText = findViewById(R.id.detail_workout_goal);
        RecyclerView recyclerView = findViewById(R.id.workout_detail_recycler_view);

        String workoutKey = getIntent().getStringExtra(EXTRA_WORKOUT_KEY);

        if (workoutKey == null) {
            Toast.makeText(this, "Fehler: Workout nicht gefunden", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("WorkoutLog", Context.MODE_PRIVATE);
        String workoutJsonString = prefs.getString(workoutKey, null);

        if (workoutJsonString == null) {
            Toast.makeText(this, "Fehler: Workout-Daten konnten nicht geladen werden", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            JSONObject workoutJson = new JSONObject(workoutJsonString);
            playerTag = workoutJson.getString("playerTag");
            long dateMillis = workoutJson.getLong("date");
            String goal = workoutJson.getString("goal");
            JSONArray exercisesArray = workoutJson.getJSONArray("exercises");

            SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy", Locale.GERMANY);
            dateText.setText(sdf.format(new Date(dateMillis)));
            goalText.setText("Ziel: " + goal);

            List<WorkoutExercise> exerciseList = new ArrayList<>();
            for (int i = 0; i < exercisesArray.length(); i++) {
                JSONObject exerciseJson = exercisesArray.getJSONObject(i);
                
                String videoUrl = exerciseJson.optString("videoUrl", null);
                boolean isLocal = exerciseJson.optBoolean("isLocalVideo", false);
                
                // Fallback: Check ExerciseDataSource if no video info in log
                if (videoUrl == null || videoUrl.isEmpty()) {
                    String name = exerciseJson.getString("name");
                    String localVideo = ExerciseDataSource.getVideoForExercise(name);
                    if (localVideo != null && !localVideo.isEmpty()) {
                        videoUrl = localVideo;
                        isLocal = true;
                    }
                }

                exerciseList.add(new WorkoutExercise(
                        exerciseJson.getString("name"),
                        exerciseJson.getString("sets"),
                        exerciseJson.getString("reps"),
                        exerciseJson.getString("weight"),
                        videoUrl,
                        isLocal
                ));
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            WorkoutExerciseAdapter adapter = new WorkoutExerciseAdapter(exerciseList, playerTag);
            recyclerView.setAdapter(adapter);

        } catch (JSONException e) {
            Toast.makeText(this, "Fehler beim Parsen der Workout-Daten", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static class WorkoutExercise {
        final String name, sets, reps, weight, videoUrl;
        final boolean isLocalVideo;

        WorkoutExercise(String name, String sets, String reps, String weight, String videoUrl, boolean isLocalVideo) {
            this.name = name;
            this.sets = sets;
            this.reps = reps;
            this.weight = weight;
            this.videoUrl = videoUrl;
            this.isLocalVideo = isLocalVideo;
        }
    }

    private static class WorkoutExerciseAdapter extends RecyclerView.Adapter<WorkoutExerciseAdapter.ExerciseViewHolder> {
        private final List<WorkoutExercise> exercises;
        private final String playerTag;

        WorkoutExerciseAdapter(List<WorkoutExercise> exercises, String playerTag) {
            this.exercises = exercises;
            this.playerTag = playerTag;
        }

        @NonNull
        @Override
        public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_workout_exercise, parent, false);
            return new ExerciseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
            holder.bind(exercises.get(position), playerTag);
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }

        static class ExerciseViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameText, setsText, repsText, weightText;
            private final ImageButton chartButton, playButton;

            ExerciseViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.exercise_name_text);
                setsText = itemView.findViewById(R.id.exercise_sets_text);
                repsText = itemView.findViewById(R.id.exercise_reps_text);
                weightText = itemView.findViewById(R.id.exercise_weight_text);
                chartButton = itemView.findViewById(R.id.progress_chart_button);
                playButton = itemView.findViewById(R.id.video_play_button);
            }

            void bind(final WorkoutExercise exercise, final String playerTag) {
                nameText.setText(exercise.name);
                setsText.setText("Sätze: " + exercise.sets);
                repsText.setText("Wdh: " + exercise.reps);
                weightText.setText("Gewicht: " + exercise.weight + " kg");

                if (exercise.videoUrl != null && !exercise.videoUrl.isEmpty()) {
                    playButton.setVisibility(View.VISIBLE);
                    playButton.setOnClickListener(v -> {
                        Intent intent = new Intent(itemView.getContext(), VideoPlayerActivity.class);
                        intent.putExtra("VIDEO_NAME", exercise.videoUrl);
                        intent.putExtra("IS_URL", !exercise.isLocalVideo);
                        itemView.getContext().startActivity(intent);
                    });
                } else {
                    playButton.setVisibility(View.GONE);
                }

                chartButton.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), ChartActivity.class);
                    intent.putExtra(ChartActivity.EXTRA_EXERCISE_NAME, exercise.name);
                    intent.putExtra(ChartActivity.EXTRA_PLAYER_TAG, playerTag);
                    itemView.getContext().startActivity(intent);
                });
            }
        }
    }
}