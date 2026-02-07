package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PainMapActivity extends AppCompatActivity {

    private String loggedInPlayerTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pain_map);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        Toolbar toolbar = findViewById(R.id.pain_map_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (loggedInPlayerTag == null || loggedInPlayerTag.isEmpty()) {
            Toast.makeText(this, "Fehler: Spieler-ID nicht gefunden.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void onBodyPartClick(View view) {
        String bodyPart = (String) view.getTag();
        showPainInputDialog(bodyPart);
    }

    private void showPainInputDialog(final String bodyPart) {
        // Use the High-Contrast Dialog Theme
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.App_Theme_Dialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pain_input, null);
        builder.setView(dialogView);

        final TextView painLevelText = dialogView.findViewById(R.id.pain_level_text);
        final SeekBar painSeekBar = dialogView.findViewById(R.id.pain_level_seekbar);
        final EditText diagnosisEditText = dialogView.findViewById(R.id.diagnosis_edittext);

        // Force white text color for visibility
        painLevelText.setTextColor(Color.WHITE);
        diagnosisEditText.setTextColor(Color.WHITE);
        diagnosisEditText.setHintTextColor(Color.LTGRAY);

        builder.setTitle("Schmerzdetails: " + bodyPart);

        painSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                painLevelText.setText("Schmerzlevel: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("Weiter", (dialog, which) -> {
            int painLevel = painSeekBar.getProgress();
            String diagnosis = diagnosisEditText.getText().toString();
            if (painLevel >= 3) {
                showPainDescriptionDialog(bodyPart, painLevel, diagnosis);
            } else {
                savePainData(bodyPart, painLevel, diagnosis, "", "", "", "");
            }
        });

        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }

    private void showPainDescriptionDialog(final String bodyPart, final int painLevel, final String diagnosis) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.App_Theme_Dialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pain_description, null);
        builder.setView(dialogView);

        // Styling the view programmatically to ensure max contrast
        setupPainDescriptionStyling(dialogView);

        builder.setTitle("Details für " + bodyPart);
        builder.setPositiveButton("Speichern", (dialog, which) -> {
            EditText desc = dialogView.findViewById(R.id.pain_description_edittext);
            EditText trig = dialogView.findViewById(R.id.pain_trigger_edittext);
            RadioGroup rg = dialogView.findViewById(R.id.pain_after_exercise_radiogroup);
            EditText mech = dialogView.findViewById(R.id.accident_mechanism_edittext);

            int selectedRadioId = rg.getCheckedRadioButtonId();
            String afterExercise = "Unbeantwortet";
            if (selectedRadioId != -1) {
                RadioButton rb = dialogView.findViewById(selectedRadioId);
                afterExercise = rb.getText().toString();
            }
            savePainData(bodyPart, painLevel, diagnosis, desc.getText().toString(), trig.getText().toString(), afterExercise, mech.getText().toString());
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }

    private void setupPainDescriptionStyling(View v) {
        int white = Color.WHITE;
        ((TextView)v.findViewById(R.id.pain_description_edittext)).setTextColor(white);
        ((TextView)v.findViewById(R.id.pain_trigger_edittext)).setTextColor(white);
        ((TextView)v.findViewById(R.id.accident_mechanism_edittext)).setTextColor(white);
        
        // Find all radio buttons and labels in the dialog and set to white
        RadioGroup rg = v.findViewById(R.id.pain_after_exercise_radiogroup);
        for(int i=0; i<rg.getChildCount(); i++) {
            View child = rg.getChildAt(i);
            if(child instanceof RadioButton) ((RadioButton)child).setTextColor(white);
        }
        
        // Set all labels/hints if they exist
        View label = v.findViewById(R.id.label_after_exercise);
        if(label instanceof TextView) ((TextView)label).setTextColor(white);
    }

    private void savePainData(String bodyPart, int painLevel, String diagnosis, String description, String trigger, String afterExercise, String mechanism) {
        SharedPreferences prefs = getSharedPreferences("PainJournal", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String key = loggedInPlayerTag + "_" + date + "_" + bodyPart.replaceAll(" ", "_");
        String value = "painLevel=" + painLevel + ";diagnosis=" + diagnosis + ";description=" + description + ";trigger=" + trigger + ";afterExercise=" + afterExercise + ";mechanism=" + mechanism;
        editor.putString(key, value);
        editor.apply();
        Toast.makeText(this, "Verletzung für " + bodyPart + " gespeichert.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}