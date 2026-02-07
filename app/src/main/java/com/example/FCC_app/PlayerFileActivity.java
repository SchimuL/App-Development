package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlayerFileActivity extends AppCompatActivity implements InjuryReportAdapter.OnItemClickListener {

    private String selectedPlayerTag;
    private final List<InjuryEntry> injuryList = new ArrayList<>();
    private InjuryReportAdapter injuryAdapter;

    private TextInputEditText inputWeight, inputHeight, inputBenchPress, inputSquat, inputDeadlift;
    private Button saveProfileButton;
    private static final String PROFILE_PREFS_NAME = "PlayerProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_file);

        selectedPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        Toolbar toolbar = findViewById(R.id.player_file_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Digitale Akte");
        }

        if (selectedPlayerTag == null) {
            Toast.makeText(this, "Fehler: Kein Spieler ausgewählt", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView playerNameHeader = findViewById(R.id.player_name_header);
        playerNameHeader.setText("Akte: " + selectedPlayerTag);

        // Profile Views
        inputWeight = findViewById(R.id.input_weight);
        inputHeight = findViewById(R.id.input_height);
        inputBenchPress = findViewById(R.id.input_bench_press);
        inputSquat = findViewById(R.id.input_squat);
        inputDeadlift = findViewById(R.id.input_deadlift);
        saveProfileButton = findViewById(R.id.save_profile_button);

        RecyclerView injuryRecyclerView = findViewById(R.id.player_injury_list);
        injuryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        injuryAdapter = new InjuryReportAdapter(injuryList, this);
        injuryRecyclerView.setAdapter(injuryAdapter);

        loadProfileData();
        loadWellbeingData();
        loadInjuryData();

        saveProfileButton.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS_NAME, Context.MODE_PRIVATE);
        inputWeight.setText(prefs.getString(selectedPlayerTag + "_weight", ""));
        inputHeight.setText(prefs.getString(selectedPlayerTag + "_height", ""));
        inputBenchPress.setText(prefs.getString(selectedPlayerTag + "_bench_press", ""));
        inputSquat.setText(prefs.getString(selectedPlayerTag + "_squat", ""));
        inputDeadlift.setText(prefs.getString(selectedPlayerTag + "_deadlift", ""));
    }

    private void saveProfileData() {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(selectedPlayerTag + "_weight", inputWeight.getText().toString());
        editor.putString(selectedPlayerTag + "_height", inputHeight.getText().toString());
        editor.putString(selectedPlayerTag + "_bench_press", inputBenchPress.getText().toString());
        editor.putString(selectedPlayerTag + "_squat", inputSquat.getText().toString());
        editor.putString(selectedPlayerTag + "_deadlift", inputDeadlift.getText().toString());

        editor.apply();
        Toast.makeText(this, "Profildaten gespeichert!", Toast.LENGTH_SHORT).show();
    }

    private void loadWellbeingData() {
        LineChart hooperChart = findViewById(R.id.hooper_chart);
        SharedPreferences wellbeingPrefs = getSharedPreferences("WellbeingValues", Context.MODE_PRIVATE);
        List<Entry> hooperEntries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -29);

        for (int i = 0; i < 30; i++) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            dateLabels.add(new SimpleDateFormat("dd.MM", Locale.getDefault()).format(cal.getTime()));
            String key = selectedPlayerTag + "_" + date;
            String data = wellbeingPrefs.getString(key, "0,1,1,1,1,0");
            String[] parts = data.split(",");
            if(parts.length > 4) {
                int hooperScore = Integer.parseInt(parts[1]) + Integer.parseInt(parts[2]) + Integer.parseInt(parts[3]) + Integer.parseInt(parts[4]);
                if (hooperScore > 0) {
                    hooperEntries.add(new Entry(i, hooperScore));
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        LineDataSet dataSet = new LineDataSet(hooperEntries, "Hooper Index");
        LineData lineData = new LineData(dataSet);
        hooperChart.setData(lineData);
        hooperChart.getDescription().setEnabled(false);
        hooperChart.invalidate();
    }

    private void loadInjuryData() {
        injuryList.clear();
        SharedPreferences prefs = getSharedPreferences("PainJournal", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(selectedPlayerTag + "_")) {
                 try {
                    String restOfKey = key.substring((selectedPlayerTag + "_").length());
                    String date = restOfKey.substring(0, 10);
                    String bodyPart = restOfKey.substring(11).replace("_", " ");

                    String value = (String) entry.getValue();
                    String painLevelStr = parseValue(value, "painLevel");
                    int painLevel = painLevelStr.equals("N/A") ? 0 : Integer.parseInt(painLevelStr);

                    injuryList.add(new InjuryEntry(selectedPlayerTag, bodyPart, date, painLevel,
                            parseValue(value, "diagnosis"), parseValue(value, "description"),
                            parseValue(value, "trigger"), parseValue(value, "afterExercise"),
                            parseValue(value, "mechanism")));
                } catch (Exception e) { /* Ignore malformed data */ }
            }
        }

        Collections.sort(injuryList, (o1, o2) -> o2.date.compareTo(o1.date));
        injuryAdapter.notifyDataSetChanged();
    }

    private String parseValue(String data, String key) {
        if (data == null) return "N/A";
        String[] pairs = data.split(";");
        String prefix = key + "=";
        for (String pair : pairs) {
            if (pair.startsWith(prefix)) {
                return pair.substring(prefix.length());
            }
        }
        return "N/A";
    }

    @Override
    public void onItemClick(InjuryEntry item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_injury_details, null);
        builder.setView(dialogView);

        ((TextView) dialogView.findViewById(R.id.detail_body_part)).setText(item.bodyPart);
        ((TextView) dialogView.findViewById(R.id.detail_date)).setText(item.date);
        ((TextView) dialogView.findViewById(R.id.detail_pain_level)).setText("Schmerzlevel: " + item.painLevel + "/10");
        ((TextView) dialogView.findViewById(R.id.detail_description)).setText("Beschreibung: " + item.description);
        ((TextView) dialogView.findViewById(R.id.detail_trigger)).setText("Auslöser: " + item.trigger);
        ((TextView) dialogView.findViewById(R.id.detail_after_exercise)).setText("Nach Belastung: " + item.afterExercise);
        ((TextView) dialogView.findViewById(R.id.detail_mechanism)).setText("Unfallmechanismus: " + item.mechanism);
        ((TextView) dialogView.findViewById(R.id.detail_diagnosis)).setText("Arzt-Diagnose: " + item.diagnosis);

        builder.setPositiveButton("Schließen", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
