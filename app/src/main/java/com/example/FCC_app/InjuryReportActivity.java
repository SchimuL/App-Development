package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InjuryReportActivity extends AppCompatActivity {

    private String selectedTeam;
    private RecyclerView injuryRecyclerView;
    private InjuryReportAdapter adapter;
    private final List<InjuryEntry> injuryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_injury_report);

        selectedTeam = getIntent().getStringExtra("SELECTED_TEAM");

        Toolbar toolbar = findViewById(R.id.injury_report_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (selectedTeam != null) {
                getSupportActionBar().setTitle("Verletzungen: " + selectedTeam);
            } else {
                Toast.makeText(this, "Fehler: Kein Team ausgewählt", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        injuryRecyclerView = findViewById(R.id.injury_recycler_view);
        injuryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Fix: Pass null for the click listener, as it's not used here.
        adapter = new InjuryReportAdapter(injuryList, null);
        injuryRecyclerView.setAdapter(adapter);

        loadInjuryData();
    }

    private void loadInjuryData() {
        injuryList.clear();
        Set<String> playersInTeam = getPlayersInTeam();
        if (playersInTeam.isEmpty()) {
            Toast.makeText(this, "Keine Spieler in diesem Team gefunden", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("PainJournal", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (String playerInTeam : playersInTeam) {
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                String playerPrefix = playerInTeam + "_";

                if (key.startsWith(playerPrefix)) {
                    try {
                        String restOfKey = key.substring(playerPrefix.length());
                        String date = restOfKey.substring(0, 10);
                        String bodyPart = restOfKey.substring(11).replace("_", " ");

                        String value = (String) entry.getValue();

                        String painLevelStr = parseValue(value, "painLevel");
                        int painLevel = painLevelStr.equals("N/A") ? 0 : Integer.parseInt(painLevelStr);

                        String diagnosis = parseValue(value, "diagnosis");
                        String description = parseValue(value, "description");
                        String trigger = parseValue(value, "trigger");
                        String afterExercise = parseValue(value, "afterExercise");
                        String mechanism = parseValue(value, "mechanism");

                        injuryList.add(new InjuryEntry(playerInTeam, bodyPart, date, painLevel, diagnosis, description, trigger, afterExercise, mechanism));
                    } catch (Exception e) {
                        // Ignore malformed keys/values to prevent crashes
                    }
                }
            }
        }

        Collections.sort(injuryList, (o1, o2) -> o2.date.compareTo(o1.date));
        adapter.notifyDataSetChanged();

        if (injuryList.isEmpty()) {
            Toast.makeText(this, "Keine Verletzungsmeldungen für dieses Team vorhanden", Toast.LENGTH_LONG).show();
        }
    }

    private Set<String> getPlayersInTeam() {
        Set<String> playerTags = new HashSet<>();
        SharedPreferences profilePrefs = getSharedPreferences("UserProfileData", Context.MODE_PRIVATE);
        List<String> teams = Arrays.asList(getResources().getStringArray(R.array.mannschaften));
        int targetTeamIndex = teams.indexOf(selectedTeam);
        if (targetTeamIndex == -1) return playerTags;

        Set<String> allPlayerTags = new HashSet<>();
        for (String key : profilePrefs.getAll().keySet()) {
            if (key.endsWith("_name")) {
                String playerTag = key.substring(0, key.length() - "_name".length());
                if (!playerTag.isEmpty()) {
                    allPlayerTags.add(playerTag);
                }
            }
        }

        for (String playerTag : allPlayerTags) {
            int playerTeamIndex = profilePrefs.getInt(playerTag + "_team", -1);
            if (playerTeamIndex == targetTeamIndex) {
                playerTags.add(playerTag);
            }
        }
        return playerTags;
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
