package com.example.FCC_app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_AUSWAHL = "com.example.abgabe1_lg.extra.AUSWAHL";

    private TextView titleTextView, definitionTextView;
    private Spinner experienceSpinner;
    private RecyclerView planRecyclerView;
    private String currentSelection;
    private String loggedInPlayerTag;
    private PlanAdapter adapter;
    private List<PlanExerciseItem> exerciseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        titleTextView = findViewById(R.id.detail_textview);
        definitionTextView = findViewById(R.id.definition_textview);
        experienceSpinner = findViewById(R.id.experience_spinner);
        planRecyclerView = findViewById(R.id.plan_recycler_view);

        planRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlanAdapter(exerciseList);
        planRecyclerView.setAdapter(adapter);

        currentSelection = getIntent().getStringExtra(EXTRA_AUSWAHL);
        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        if (currentSelection != null) {
            titleTextView.setText(currentSelection);
            setupDynamicContent();
        }

        experienceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePlanList(parent.getItemAtPosition(position).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDynamicContent() {
        List<String> verletzungsarten = Arrays.asList(getResources().getStringArray(R.array.verletzungsarten));
        String[] levels = verletzungsarten.contains(currentSelection) ? 
                getResources().getStringArray(R.array.reha_phasen) : 
                getResources().getStringArray(R.array.trainings_level);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, levels) {
            @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
            @Override public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(Color.parseColor("#1E1E1E"));
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        experienceSpinner.setAdapter(spinnerAdapter);

        if (verletzungsarten.contains(currentSelection)) {
            definitionTextView.setText("Definition für: " + currentSelection);
        } else {
            definitionTextView.setText("Erklärung für: " + currentSelection);
        }
    }

    private void updatePlanList(String selectedLevel) {
        exerciseList.clear();
        String[][] planData = getTrainingPlan(currentSelection, selectedLevel);
        SharedPreferences oneRmPrefs = getSharedPreferences("OneRmValues", Context.MODE_PRIVATE);
        
        for (String[] row : planData) {
            String name = row[0];
            String intensityStr = row[3];
            String targetWeightKg = "0";

            if (loggedInPlayerTag != null) {
                String wStr = oneRmPrefs.getString(loggedInPlayerTag + "_" + name + "_weight", "0");
                String rStr = oneRmPrefs.getString(loggedInPlayerTag + "_" + name + "_reps", "0");
                try {
                    float bw = Float.parseFloat(wStr);
                    int br = Integer.parseInt(rStr);
                    int intensity = Integer.parseInt(intensityStr);
                    if (bw > 0 && br > 0 && intensity > 0) {
                        float oneRm = (br == 1) ? bw : bw * (1 + (float) br / 30.0f);
                        targetWeightKg = String.format(Locale.US, "%.1f", oneRm * (intensity / 100.0f));
                    }
                } catch (Exception e) {}
            }
            exerciseList.add(new PlanExerciseItem(name, row[1], row[2], targetWeightKg, ExerciseDataSource.getDefaultNoteForExercise(name)));
        }
        adapter.notifyDataSetChanged();
    }

    private void showSetInputDialog(PlanExerciseItem item) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exercise_sets, null);
        TextView nameText = view.findViewById(R.id.text_exercise_name_dialog);
        LinearLayout container = view.findViewById(R.id.container_sets);
        MaterialButton saveBtn = view.findViewById(R.id.btn_save_sets);

        nameText.setText(item.name);
        int numSets = 3;
        try { numSets = Integer.parseInt(item.sets.split("-")[0].trim()); } catch (Exception e) {}

        if (item.setDetails.isEmpty()) {
            for (int i = 0; i < numSets; i++) item.setDetails.add(new CustomPlanActivity.SetData("", ""));
        }

        final List<EditText> repInputs = new ArrayList<>();
        final List<EditText> weightInputs = new ArrayList<>();

        for (int i = 0; i < item.setDetails.size(); i++) {
            View setRow = LayoutInflater.from(this).inflate(R.layout.item_set_input, container, false);
            TextView setNum = setRow.findViewById(R.id.text_set_number);
            EditText editReps = setRow.findViewById(R.id.edit_reps);
            EditText editWeight = setRow.findViewById(R.id.edit_weight);
            TextView oneRmLabel = setRow.findViewById(R.id.text_1rm_suggestion);

            setNum.setText("Satz " + (i + 1));
            editReps.setText(item.setDetails.get(i).reps);
            editWeight.setText(item.setDetails.get(i).weight);
            
            if (!item.weight.equals("0")) {
                oneRmLabel.setText("Vorgabe: " + item.weight + " kg");
                oneRmLabel.setVisibility(View.VISIBLE);
            }

            repInputs.add(editReps); weightInputs.add(editWeight);
            container.addView(setRow);
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.App_Theme_Dialog).setView(view).create();
        saveBtn.setOnClickListener(v -> {
            for (int i = 0; i < repInputs.size(); i++) {
                item.setDetails.get(i).reps = repInputs.get(i).getText().toString();
                item.setDetails.get(i).weight = weightInputs.get(i).getText().toString();
            }
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        });
        dialog.show();
    }

    private String[][] getTrainingPlan(String mainSelection, String level) {
        if (mainSelection.equals("Kraftaufbau")) {
            if (level.equals("Anfänger")) return new String[][]{{"Kniebeugen", "3", "12", "70"}, {"Liegestütze (auf Knien)", "3", "10", "0"}};
            if (level.equals("Erfahren")) return new String[][]{{"Klimmzüge", "4", "8", "0"}, {"Kreuzheben", "3", "8", "85"}};
        }
        if (mainSelection.equals("Bänderdehnung")) {
            if (level.equals("Return to Activity")) return new String[][]{{"Fußkreisen", "3", "20", "0"}, {"Isometrisches Anspannen", "4", "15s", "0"}};
            if (level.equals("Return to Training")) return new String[][]{{"Wadenheben", "3", "15", "0"}, {"Einbeinstand", "4.2", "30s", "0"}};
        }
        return new String[][]{};
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private static class PlanExerciseItem {
        String name, sets, reps, weight, note;
        List<CustomPlanActivity.SetData> setDetails = new ArrayList<>();
        PlanExerciseItem(String n, String s, String r, String w, String nt) { this.name=n; this.sets=s; this.reps=r; this.weight=w; this.note=nt; }
    }

    private class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.ViewHolder> {
        private final List<PlanExerciseItem> items;
        PlanAdapter(List<PlanExerciseItem> items) { this.items = items; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_plan_exercise, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlanExerciseItem item = items.get(position);
            holder.name.setText(item.name);
            holder.sets.setText(item.sets + " Sätze");
            if (!item.setDetails.isEmpty()) {
                String summary = "";
                for (int i=0; i<item.setDetails.size(); i++) summary += item.setDetails.get(i).reps + "@" + item.setDetails.get(i).weight + (i < item.setDetails.size()-1 ? ", " : "");
                holder.reps.setText(summary);
                holder.weight.setText("Erledigt"); holder.weight.setTextColor(Color.GREEN);
            } else {
                holder.reps.setText(item.reps + " Wdh");
                holder.weight.setText(item.weight.equals("0") ? "Körper" : item.weight + " kg");
                holder.weight.setTextColor(Color.parseColor("#FFC107"));
            }
            holder.note.setText(item.note);
            holder.itemView.setOnClickListener(v -> showSetInputDialog(item));
        }
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, sets, note, reps, weight; MaterialButton video;
            ViewHolder(View v) { super(v); name = v.findViewById(R.id.exercise_name_text); sets = v.findViewById(R.id.sets_text); reps = v.findViewById(R.id.reps_text); weight = v.findViewById(R.id.weight_text); note = v.findViewById(R.id.note_text); video = v.findViewById(R.id.video_button); }
        }
    }
}