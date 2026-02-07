package com.example.FCC_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExerciseSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_EXERCISES = "SELECTED_EXERCISES_LIST";

    private String loggedInPlayerTag;
    private final List<ExerciseModel> allExercises = new ArrayList<>();
    private final ArrayList<ExerciseModel> selectedExercises = new ArrayList<>();
    private ExerciseSelectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_selection);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        Toolbar toolbar = findViewById(R.id.selection_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        RecyclerView recyclerView = findViewById(R.id.selection_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ExerciseSelectionAdapter(allExercises, (exercise, isChecked) -> {
            if (isChecked) {
                selectedExercises.add(exercise);
            } else {
                selectedExercises.remove(exercise);
            }
        });
        recyclerView.setAdapter(adapter);

        Button weiterButton = findViewById(R.id.selection_weiter_button);
        weiterButton.setOnClickListener(v -> {
            if (selectedExercises.isEmpty()) {
                Toast.makeText(this, "Bitte wähle mindestens eine Übung aus", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ExerciseSelectionActivity.this, CustomPlanActivity.class);
            intent.putExtra(EXTRA_SELECTED_EXERCISES, selectedExercises);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        loadExercises();
    }

    private void loadExercises() {
        allExercises.clear();

        // 1. Load static exercises
        List<String> staticNames = ExerciseDataSource.getAllExerciseNames();
        for (String name : staticNames) {
            allExercises.add(new ExerciseModel(name, ExerciseDataSource.getVideoForExercise(name), false));
        }

        // 2. Load custom exercises from Firestore
        FirebaseFirestore.getInstance().collection("custom_exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        String videoUrl = document.getString("videoUrl");
                        boolean exists = false;
                        for (ExerciseModel em : allExercises) {
                            if (em.name.equals(name)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            allExercises.add(new ExerciseModel(name, videoUrl, true));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Trainer-Übungen konnten nicht geladen werden", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // --- Serializable Model ---
    public static class ExerciseModel implements Serializable {
        String name;
        String videoUrl;
        boolean isCustom;
        boolean isLocalVideo; // To differentiate between file name and URL

        ExerciseModel(String name, String videoPath, boolean isCustom) {
            this.name = name;
            this.isCustom = isCustom;
            if (isCustom) {
                this.videoUrl = videoPath; // This is a URL from Firestore
                this.isLocalVideo = false;
            } else {
                this.videoUrl = null; // Static exercises have no URL
                this.isLocalVideo = videoPath != null && !videoPath.isEmpty();
            }
        }
    }

    // --- Adapter ---
    private static class ExerciseSelectionAdapter extends RecyclerView.Adapter<ExerciseSelectionAdapter.ViewHolder> {

        interface OnCheckedChangeListener {
            void onCheckedChanged(ExerciseModel exercise, boolean isChecked);
        }

        private final List<ExerciseModel> exercises;
        private final OnCheckedChangeListener listener;

        ExerciseSelectionAdapter(List<ExerciseModel> exercises, OnCheckedChangeListener listener) {
            this.exercises = exercises;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_exercise_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ExerciseModel exercise = exercises.get(position);
            holder.checkBox.setText(exercise.name + (exercise.isCustom ? " (Trainer)" : ""));
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(false);

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onCheckedChanged(exercise, isChecked);
            });
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.exercise_checkbox);
            }
        }
    }
}