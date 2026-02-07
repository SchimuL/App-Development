package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OneRmActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "OneRmValues";
    private TableLayout oneRmTable;
    private Map<String, EditText[]> exerciseRowViews = new HashMap<>();
    private String loggedInPlayerTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_rm);

        // Get the player tag from the Profile screen
        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.one_rm_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        oneRmTable = findViewById(R.id.one_rm_table);
        Button saveButton = findViewById(R.id.save_all_1rm_button);

        populateExerciseTable();

        saveButton.setOnClickListener(v -> saveAllOneRmValues());
    }

    private void populateExerciseTable() {
        List<String> allExercises = ExerciseDataSource.getAllExerciseNames();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        for (String exerciseName : allExercises) {
            TableRow row = new TableRow(this);

            TextView exerciseTextView = new TextView(this);
            exerciseTextView.setText(exerciseName);
            exerciseTextView.setPadding(4, 8, 4, 8);

            EditText weightEditText = new EditText(this);
            weightEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            weightEditText.setGravity(Gravity.CENTER);

            EditText repsEditText = new EditText(this);
            repsEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            repsEditText.setGravity(Gravity.CENTER);

            TextView oneRmTextView = new TextView(this);
            oneRmTextView.setGravity(Gravity.CENTER);
            oneRmTextView.setPadding(4, 8, 4, 8);

            row.addView(exerciseTextView);
            row.addView(weightEditText);
            row.addView(repsEditText);
            row.addView(oneRmTextView);

            exerciseRowViews.put(exerciseName, new EditText[]{weightEditText, repsEditText});

            // Load saved values for the specific player
            if (loggedInPlayerTag != null) {
                weightEditText.setText(prefs.getString(loggedInPlayerTag + "_" + exerciseName + "_weight", ""));
                repsEditText.setText(prefs.getString(loggedInPlayerTag + "_" + exerciseName + "_reps", ""));
            }

            TextWatcher textWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    calculateOneRm(weightEditText, repsEditText, oneRmTextView);
                }
            };

            weightEditText.addTextChangedListener(textWatcher);
            repsEditText.addTextChangedListener(textWatcher);

            oneRmTable.addView(row);
        }
    }

    private void calculateOneRm(EditText weightEt, EditText repsEt, TextView oneRmTv) {
        String weightStr = weightEt.getText().toString();
        String repsStr = repsEt.getText().toString();

        if (weightStr.isEmpty() || repsStr.isEmpty()) {
            oneRmTv.setText("");
            return;
        }

        try {
            float weight = Float.parseFloat(weightStr);
            int reps = Integer.parseInt(repsStr);

            if (reps > 10) {
                oneRmTv.setText("Ungültig");
                return;
            }
            if (reps == 0) {
                oneRmTv.setText("");
                return;
            }
            if (reps == 1) {
                oneRmTv.setText(String.format(Locale.getDefault(), "%.1f", weight));
                return;
            }

            float oneRm = weight * (1 + (float) reps / 30.0f);
            oneRmTv.setText(String.format(Locale.getDefault(), "%.1f", oneRm));

        } catch (NumberFormatException e) {
            oneRmTv.setText("");
        }
    }

    private void saveAllOneRmValues() {
        if (loggedInPlayerTag == null) {
            Toast.makeText(this, "Fehler: Kein Spieler eingeloggt", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, EditText[]> entry : exerciseRowViews.entrySet()) {
            String exerciseName = entry.getKey();
            String weight = entry.getValue()[0].getText().toString();
            String reps = entry.getValue()[1].getText().toString();

            editor.putString(loggedInPlayerTag + "_" + exerciseName + "_weight", weight);
            editor.putString(loggedInPlayerTag + "_" + exerciseName + "_reps", reps);
        }

        editor.apply();
        Toast.makeText(this, "1RM-Werte für " + loggedInPlayerTag + " gespeichert!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}