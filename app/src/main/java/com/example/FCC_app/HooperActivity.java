package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HooperActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PlayerDailyEntries";
    private String loggedInPlayerTag;

    // Wellbeing Views
    private SeekBar stressSeekBar, fatigueSeekBar, sorenessSeekBar, painSeekBar;
    private TextView stressValue, fatigueValue, sorenessValue, painValue;

    // Trainingsload Views
    private Spinner sportTypeSpinner;
    private TextInputEditText trainingDurationInput;
    private SeekBar rpeSeekBar;
    private TextView rpeValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hooper);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.hooper_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tagesprotokoll");
        }

        // --- Find Wellbeing Views ---
        stressSeekBar = findViewById(R.id.seekbar_stress);
        stressValue = findViewById(R.id.value_stress);
        fatigueSeekBar = findViewById(R.id.seekbar_fatigue);
        fatigueValue = findViewById(R.id.value_fatigue);
        sorenessSeekBar = findViewById(R.id.seekbar_soreness);
        sorenessValue = findViewById(R.id.value_soreness);
        painSeekBar = findViewById(R.id.seekbar_pain);
        painValue = findViewById(R.id.value_pain);

        // --- Find Trainingsload Views ---
        sportTypeSpinner = findViewById(R.id.spinner_sport_type);
        trainingDurationInput = findViewById(R.id.input_training_duration);
        rpeSeekBar = findViewById(R.id.seekbar_rpe);
        rpeValue = findViewById(R.id.value_rpe);

        // --- Find Button ---
        Button saveButton = findViewById(R.id.save_daily_entry_button);

        // --- Setup Listeners for SeekBars ---
        setupSeekBarListener(stressSeekBar, stressValue, 1);
        setupSeekBarListener(fatigueSeekBar, fatigueValue, 1);
        setupSeekBarListener(sorenessSeekBar, sorenessValue, 1);
        setupSeekBarListener(painSeekBar, painValue, 1);
        setupSeekBarListener(rpeSeekBar, rpeValue, 1); // RPE starts at 1

        // --- Setup Spinner ---
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sport_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sportTypeSpinner.setAdapter(adapter);

        // --- Save Button Logic ---
        saveButton.setOnClickListener(v -> saveDailyEntry());
    }

    private void saveDailyEntry() {
        if (loggedInPlayerTag == null) {
            Toast.makeText(this, "Fehler: Kein Spieler eingeloggt", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Get Wellbeing Values ---
        int stress = stressSeekBar.getProgress() + 1;
        int fatigue = fatigueSeekBar.getProgress() + 1;
        int soreness = sorenessSeekBar.getProgress() + 1;
        int pain = painSeekBar.getProgress() + 1;

        // --- Get Trainingsload Values ---
        String sportType = sportTypeSpinner.getSelectedItem().toString();
        String durationStr = trainingDurationInput.getText() != null ? trainingDurationInput.getText().toString() : "0";
        int duration = durationStr.isEmpty() ? 0 : Integer.parseInt(durationStr);
        int rpe = rpeSeekBar.getProgress() + 1;

        // Format: "stress,fatigue,soreness,pain;duration,rpe,sportType"
        String dailyData = String.format(Locale.US, "%d,%d,%d,%d;%d,%d,%s",
                stress, fatigue, soreness, pain, duration, rpe, sportType);

        // --- Save to SharedPreferences ---
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String key = loggedInPlayerTag + "_" + todayDate;

        editor.putString(key, dailyData);
        editor.apply();

        Toast.makeText(this, "Tagesprotokoll gespeichert!", Toast.LENGTH_SHORT).show();
        finish(); // Close activity after saving
    }

    private void setupSeekBarListener(SeekBar seekBar, TextView valueTextView, int offset) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueTextView.setText(String.format(Locale.getDefault(), "%d", progress + offset));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        // Set initial value
        valueTextView.setText(String.format(Locale.getDefault(), "%d", seekBar.getProgress() + offset));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
