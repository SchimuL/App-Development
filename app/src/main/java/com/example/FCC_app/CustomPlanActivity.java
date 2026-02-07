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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomPlanActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_EXERCISES = "SELECTED_EXERCISES_LIST";
    public static final String EXTRA_PLAN_JSON = "PLAN_JSON";
    private static final String WORKOUT_LOG_PREFS = "WorkoutLog";

    private Spinner goalSpinner;
    private RecyclerView recyclerView;
    private TextView generalNoteTextView, generalNoteTitle, goalSelectionTitle;
    private String loggedInPlayerTag;
    private List<WorkoutExerciseItem> exerciseItems = new ArrayList<>();
    private ExerciseListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_plan);

        Toolbar toolbar = findViewById(R.id.custom_plan_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        goalSpinner = findViewById(R.id.custom_plan_goal_spinner);
        recyclerView = findViewById(R.id.custom_plan_recycler_view);
        generalNoteTextView = findViewById(R.id.general_note_textview);
        generalNoteTitle = findViewById(R.id.general_note_title);
        goalSelectionTitle = findViewById(R.id.goal_selection_title);
        MaterialButton saveWorkoutButton = findViewById(R.id.save_workout_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseListAdapter(exerciseItems);
        recyclerView.setAdapter(adapter);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        String planJson = getIntent().getStringExtra(EXTRA_PLAN_JSON);
        if (planJson != null) {
            loadPlanFromJson(planJson);
        } else {
            ArrayList<ExerciseSelectionActivity.ExerciseModel> selectedExercises = 
                (ArrayList<ExerciseSelectionActivity.ExerciseModel>) getIntent().getSerializableExtra(EXTRA_SELECTED_EXERCISES);
            setupForCustomSelection(selectedExercises);
        }

        saveWorkoutButton.setOnClickListener(v -> saveWorkout());
    }

    private void setupForCustomSelection(ArrayList<ExerciseSelectionActivity.ExerciseModel> exercises) {
        goalSelectionTitle.setVisibility(View.VISIBLE);
        goalSpinner.setVisibility(View.VISIBLE);

        ArrayAdapter<CharSequence> goalAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.trainingsziele)) {
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
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalSpinner.setAdapter(goalAdapter);
        goalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { updateExerciseList(p.getItemAtPosition(pos).toString(), exercises, null); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadPlanFromJson(String json) {
        try {
            JSONObject plan = new JSONObject(json);
            String goal = plan.getString("goal");
            String generalNote = plan.getString("generalNote");
            JSONArray exercisesArray = plan.getJSONArray("exercises");

            if (generalNote != null && !generalNote.isEmpty()) {
                generalNoteTitle.setVisibility(View.VISIBLE);
                generalNoteTextView.setVisibility(View.VISIBLE);
                generalNoteTextView.setText(generalNote);
            }

            goalSelectionTitle.setText("Vorgegebenes Ziel:");
            updateExerciseList(goal, null, exercisesArray);
        } catch (JSONException e) {}
    }

    private void updateExerciseList(String goal, ArrayList<ExerciseSelectionActivity.ExerciseModel> models, JSONArray exercisesWithNotes) {
        exerciseItems.clear();
        String[] params = getParamsForGoal(goal);
        SharedPreferences oneRmPrefs = getSharedPreferences("OneRmValues", Context.MODE_PRIVATE);

        int count = (models != null) ? models.size() : (exercisesWithNotes != null ? exercisesWithNotes.length() : 0);

        for (int i = 0; i < count; i++) {
            try {
                String name, note, sets = params[0], reps = params[1];
                if (exercisesWithNotes != null) {
                    JSONObject obj = exercisesWithNotes.getJSONObject(i);
                    name = obj.getString("name");
                    note = obj.getString("note");
                } else {
                    ExerciseSelectionActivity.ExerciseModel m = models.get(i);
                    name = m.name;
                    note = ExerciseDataSource.getDefaultNoteForExercise(name);
                }

                String targetWeightKg = "0";
                if (loggedInPlayerTag != null) {
                    String wStr = oneRmPrefs.getString(loggedInPlayerTag + "_" + name + "_weight", "0");
                    String rStr = oneRmPrefs.getString(loggedInPlayerTag + "_" + name + "_reps", "0");
                    try {
                        float bw = Float.parseFloat(wStr);
                        int br = Integer.parseInt(rStr);
                        int intensity = Integer.parseInt(params[3]);
                        if (bw > 0 && br > 0 && intensity > 0) {
                            float oneRm = (br == 1) ? bw : bw * (1 + (float) br / 30.0f);
                            targetWeightKg = String.format(Locale.US, "%.1f", oneRm * (intensity / 100.0f));
                        }
                    } catch (Exception e) {}
                }

                exerciseItems.add(new WorkoutExerciseItem(name, sets, reps, targetWeightKg, note));
            } catch (JSONException e) {}
        }
        adapter.notifyDataSetChanged();
    }

    private String[] getParamsForGoal(String goal) {
        switch (goal) {
            case "Kraftaufbau": return new String[]{"3-5", "6-10", "2-3 min", "85"};
            case "Ausdauer": return new String[]{"2-3", "15-25", "30-60 sec", "60"};
            case "Schnelligkeit": return new String[]{"4-6", "3-5", "3-5 min", "90"};
            default: return new String[]{"3", "10-15", "60 sec", "0"};
        }
    }

    private void showSetInputDialog(WorkoutExerciseItem item) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exercise_sets, null);
        TextView nameText = view.findViewById(R.id.text_exercise_name_dialog);
        LinearLayout container = view.findViewById(R.id.container_sets);
        MaterialButton saveBtn = view.findViewById(R.id.btn_save_sets);

        nameText.setText(item.name);
        
        int numSets = 3;
        try {
            String rawSets = item.sets.split("-")[0].replaceAll("[^0-9]", "");
            numSets = Integer.parseInt(rawSets);
        } catch (Exception e) {}

        String lastWeekInfo = fetchLastWeekPerformance(item.name);

        if (item.setDetails.isEmpty()) {
            for (int i = 0; i < numSets; i++) {
                item.setDetails.add(new SetData("", "")); 
            }
        }

        final List<EditText> repInputs = new ArrayList<>();
        final List<EditText> weightInputs = new ArrayList<>();

        for (int i = 0; i < item.setDetails.size(); i++) {
            View setRow = LayoutInflater.from(this).inflate(R.layout.item_set_input, container, false);
            TextView setNum = setRow.findViewById(R.id.text_set_number);
            EditText editReps = setRow.findViewById(R.id.edit_reps);
            EditText editWeight = setRow.findViewById(R.id.edit_weight);
            TextView oneRmLabel = setRow.findViewById(R.id.text_1rm_suggestion);
            TextView lastWeekText = setRow.findViewById(R.id.text_last_week_info);

            setNum.setText("Satz " + (i + 1));
            editReps.setText(item.setDetails.get(i).reps);
            editWeight.setText(item.setDetails.get(i).weight);
            
            // Set the 1RM suggestion right next to the field
            if (!item.weight.equals("0")) {
                oneRmLabel.setText("Vorgabe: " + item.weight + " kg");
                oneRmLabel.setVisibility(View.VISIBLE);
            } else {
                oneRmLabel.setVisibility(View.GONE);
            }

            if (!lastWeekInfo.isEmpty()) {
                lastWeekText.setVisibility(View.VISIBLE);
                lastWeekText.setText("Vorwoche: " + lastWeekInfo);
            }

            repInputs.add(editReps);
            weightInputs.add(editWeight);
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

    private String fetchLastWeekPerformance(String exerciseName) {
        SharedPreferences logPrefs = getSharedPreferences(WORKOUT_LOG_PREFS, Context.MODE_PRIVATE);
        Map<String, ?> allLogs = logPrefs.getAll();
        String latestPerf = "";
        long latestTimestamp = 0;
        for (Map.Entry<String, ?> entry : allLogs.entrySet()) {
            if (entry.getKey().startsWith(loggedInPlayerTag + "_")) {
                try {
                    JSONObject workout = new JSONObject((String) entry.getValue());
                    long timestamp = workout.getLong("date");
                    if (timestamp > latestTimestamp && timestamp < System.currentTimeMillis() - 60000) {
                        JSONArray exercises = workout.getJSONArray("exercises");
                        for (int i = 0; i < exercises.length(); i++) {
                            JSONObject ex = exercises.getJSONObject(i);
                            if (ex.getString("name").equals(exerciseName)) {
                                latestTimestamp = timestamp;
                                latestPerf = ex.getString("reps") + "x" + ex.getString("weight") + "kg";
                                break;
                            }
                        }
                    }
                } catch (JSONException e) {}
            }
        }
        return latestPerf;
    }

    private void saveWorkout() {
        if (loggedInPlayerTag == null) return;
        SharedPreferences workoutLogPrefs = getSharedPreferences(WORKOUT_LOG_PREFS, Context.MODE_PRIVATE);
        JSONObject workoutJson = new JSONObject();
        JSONArray exercisesArray = new JSONArray();
        try {
            for (WorkoutExerciseItem item : exerciseItems) {
                JSONObject ex = new JSONObject();
                ex.put("name", item.name);
                ex.put("sets", item.sets);
                if (!item.setDetails.isEmpty()) {
                    JSONArray setArray = new JSONArray();
                    for (SetData sd : item.setDetails) {
                        JSONObject sObj = new JSONObject(); sObj.put("reps", sd.reps); sObj.put("weight", sd.weight);
                        setArray.put(sObj);
                    }
                    ex.put("setDetails", setArray);
                    ex.put("reps", item.setDetails.get(0).reps);
                    ex.put("weight", item.setDetails.get(0).weight);
                } else { ex.put("reps", item.reps); ex.put("weight", item.weight); }
                exercisesArray.put(ex);
            }
            workoutJson.put("playerTag", loggedInPlayerTag);
            workoutJson.put("goal", goalSpinner.getSelectedItem().toString());
            workoutJson.put("date", System.currentTimeMillis());
            workoutJson.put("exercises", exercisesArray);
            String workoutKey = loggedInPlayerTag + "_" + System.currentTimeMillis();
            workoutLogPrefs.edit().putString(workoutKey, workoutJson.toString()).apply();
            Toast.makeText(this, "Workout gespeichert!", Toast.LENGTH_LONG).show();
            finish();
        } catch (JSONException e) {}
    }

    public static class SetData {
        String reps, weight;
        SetData(String r, String w) { this.reps = r; this.weight = w; }
    }

    private static class WorkoutExerciseItem {
        String name, sets, reps, weight, note;
        List<SetData> setDetails = new ArrayList<>();
        WorkoutExerciseItem(String n, String s, String r, String w, String nt) { name=n; sets=s; reps=r; weight=w; note=nt; }
    }

    private class ExerciseListAdapter extends RecyclerView.Adapter<ExerciseListAdapter.ViewHolder> {
        private final List<WorkoutExerciseItem> items;
        ExerciseListAdapter(List<WorkoutExerciseItem> items) { this.items = items; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_plan_exercise, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WorkoutExerciseItem item = items.get(position);
            holder.name.setText(item.name);
            holder.sets.setText(item.sets + " Sätze");
            if (!item.setDetails.isEmpty()) {
                String summary = "";
                for (int i=0; i<item.setDetails.size(); i++) summary += item.setDetails.get(i).reps + "@" + item.setDetails.get(i).weight + (i < item.setDetails.size()-1 ? ", " : "");
                holder.reps.setText(summary);
                holder.weight.setText("Erledigt");
                holder.weight.setTextColor(Color.GREEN);
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
            TextView name, sets, note, reps, weight;
            MaterialButton video;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.exercise_name_text); sets = v.findViewById(R.id.sets_text);
                reps = v.findViewById(R.id.reps_text); weight = v.findViewById(R.id.weight_text);
                note = v.findViewById(R.id.note_text); video = v.findViewById(R.id.video_button);
            }
        }
    }
}