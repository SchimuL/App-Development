package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class TrainerPlanActivity extends AppCompatActivity {

    private List<ExerciseItem> exerciseItemList = new ArrayList<>();
    private ExerciseAdapter adapter;

    private EditText generalNoteEditText;
    private Spinner playerSpinner, goalSpinner;
    private ArrayAdapter<String> goalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_plan);

        Toolbar toolbar = findViewById(R.id.trainer_plan_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.exercises_recycler_view);
        generalNoteEditText = findViewById(R.id.general_note_edittext);
        playerSpinner = findViewById(R.id.player_spinner);
        goalSpinner = findViewById(R.id.goal_spinner_trainer);
        MaterialButton saveButton = findViewById(R.id.save_plan_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseAdapter(exerciseItemList);
        recyclerView.setAdapter(adapter);

        loadAllExercises();
        loadPlayersAsync();

        // --- Custom Adapters for High Contrast ---
        setupContrastSpinner(goalSpinner, getResources().getStringArray(R.array.trainingsziele));

        playerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                loadPlanForPlayer(p.getItemAtPosition(pos).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> p) { resetPlanView(); }
        });

        saveButton.setOnClickListener(v -> savePlan());
    }

    private void setupContrastSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @NonNull @Override public View getView(int pos, @Nullable View conv, @NonNull ViewGroup parent) {
                View v = super.getView(pos, conv, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
            @Override public View getDropDownView(int pos, @Nullable View conv, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(pos, conv, parent);
                v.setBackgroundColor(Color.parseColor("#1E1E1E"));
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadAllExercises() {
        exerciseItemList.clear();
        for (String name : ExerciseDataSource.getAllExerciseNames()) { exerciseItemList.add(new ExerciseItem(name)); }
        FirebaseFirestore.getInstance().collection("custom_exercises").get().addOnSuccessListener(shots -> {
            for (QueryDocumentSnapshot doc : shots) {
                String name = doc.getString("name");
                exerciseItemList.add(new ExerciseItem(name + " (Custom)"));
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void loadPlayersAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences playerPrefs = getSharedPreferences("PlayerTags", MODE_PRIVATE);
            ArrayList<String> playerTags = new ArrayList<>(playerPrefs.getAll().keySet());
            runOnUiThread(() -> setupContrastSpinner(playerSpinner, playerTags.toArray(new String[0])));
        });
    }

    private void loadPlanForPlayer(String playerTag) {
        SharedPreferences prefs = getSharedPreferences("TrainerPlans", Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(playerTag, null);
        resetPlanView();
        if (jsonStr != null) {
            try {
                JSONObject json = new JSONObject(jsonStr);
                generalNoteEditText.setText(json.getString("generalNote"));
                JSONArray arr = json.getJSONArray("exercises");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.getString("name");
                    for (ExerciseItem item : exerciseItemList) {
                        if (item.name.replace(" (Custom)", "").equals(name)) {
                            item.isSelected = true;
                            item.note = obj.getString("note");
                        }
                    }
                }
            } catch (JSONException e) {}
        }
        adapter.notifyDataSetChanged();
    }

    private void resetPlanView() {
        generalNoteEditText.setText("");
        for (ExerciseItem item : exerciseItemList) { item.isSelected = false; item.note = ""; }
        adapter.notifyDataSetChanged();
    }

    private void savePlan() {
        if (playerSpinner.getSelectedItem() == null) return;
        String playerTag = playerSpinner.getSelectedItem().toString();
        JSONObject json = new JSONObject();
        try {
            json.put("goal", goalSpinner.getSelectedItem().toString());
            json.put("generalNote", generalNoteEditText.getText().toString());
            JSONArray arr = new JSONArray();
            for (ExerciseItem item : exerciseItemList) {
                if (item.isSelected) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", item.name.replace(" (Custom)", ""));
                    obj.put("note", item.note);
                    arr.put(obj);
                }
            }
            json.put("exercises", arr);
            getSharedPreferences("TrainerPlans", MODE_PRIVATE).edit().putString(playerTag, json.toString()).apply();
            Toast.makeText(this, "Plan gespeichert!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (JSONException e) {}
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private static class ExerciseItem {
        String name, note = "";
        boolean isSelected = false;
        ExerciseItem(String name) { this.name = name; }
    }

    private class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {
        private final List<ExerciseItem> items;
        ExerciseAdapter(List<ExerciseItem> items) { this.items = items; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.list_item_exercise_plan, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            ExerciseItem item = items.get(pos);
            h.checkBox.setText(item.name);
            h.checkBox.setTextColor(Color.WHITE);
            h.checkBox.setChecked(item.isSelected);
            h.checkBox.setOnCheckedChangeListener((b, isChecked) -> item.isSelected = isChecked);
            h.note.setText(item.note);
            h.note.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { item.note = s.toString(); }
            });
        }
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox; EditText note;
            ViewHolder(View v) { super(v); checkBox = v.findViewById(R.id.exercise_checkbox); note = v.findViewById(R.id.exercise_note_edittext); }
        }
    }
}