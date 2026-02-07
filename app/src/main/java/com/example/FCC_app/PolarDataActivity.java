package com.example.FCC_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class PolarDataActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 101;
    private String teamForImport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polar_data);

        Toolbar toolbar = findViewById(R.id.polar_data_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Team List for Viewing Performance Data
        ListView teamListView = findViewById(R.id.team_list_view);
        String[] teams = getResources().getStringArray(R.array.mannschaften);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, teams);
        teamListView.setAdapter(adapter);

        teamListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTeam = (String) parent.getItemAtPosition(position);
            // Navigate to a view showing players of that team to see their performance
            Intent intent = new Intent(PolarDataActivity.this, PlayerSelectionActivity.class);
            intent.putExtra("SELECTED_TEAM", selectedTeam);
            // We need to tell PlayerSelectionActivity what to do on player click
            intent.putExtra("TARGET_ACTIVITY", "PERFORMANCE");
            startActivity(intent);
        });

        // Import Button Logic
        Button importButton = findViewById(R.id.import_polar_data_button);
        importButton.setOnClickListener(v -> promptForTeamAndImport());
    }

    private void promptForTeamAndImport() {
        String[] teams = getResources().getStringArray(R.array.mannschaften);
        new AlertDialog.Builder(this)
                .setTitle("Mannschaft für Import auswählen")
                .setItems(teams, (dialog, which) -> {
                    teamForImport = teams[which];
                    openFilePicker();
                })
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Excel-Datei auswählen"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();

                new Thread(() -> {
                    List<PlayerData> parsedData = PolarDataParser.parseExcelFile(getApplicationContext(), fileUri);

                    if (parsedData != null && !parsedData.isEmpty()) {
                        saveDataToDatabase(parsedData);
                        saveTeamAndPlayerAssignments(parsedData, teamForImport);

                        runOnUiThread(() -> {
                            String message = "Erfolgreich Daten für " + parsedData.size() + " Spieler in Team '" + teamForImport + "' importiert.";
                            Toast.makeText(PolarDataActivity.this, message, Toast.LENGTH_LONG).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(PolarDataActivity.this, "Fehler beim Parsen oder Datei ist leer.", Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            }
        }
    }

    private void saveDataToDatabase(List<PlayerData> parsedData) {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        PerformanceDataDao dao = db.performanceDataDao();

        List<PerformanceData> performanceDataList = new ArrayList<>();
        for (PlayerData player : parsedData) {
            performanceDataList.add(new PerformanceData(
                    player.getPlayerTag(),
                    player.getDate(),
                    player.getAverageHeartRate(),
                    player.getMaxHeartRate(),
                    player.getTotalDistance(),
                    player.getSprints(),
                    player.getMaxSpeed(),
                    player.getTimeInHrZones(),
                    player.getDistanceInSpeedZones(),
                    player.getRelativeMaxHeartRate(),
                    player.getDistancePerMinute(),
                    player.getAccelerations(),
                    player.getDecelerations()
            ));
        }
        dao.insertAll(performanceDataList);
    }

    private void saveTeamAndPlayerAssignments(List<PlayerData> parsedData, String teamName) {
        SharedPreferences teamPrefs = getSharedPreferences("TeamAssignments", Context.MODE_PRIVATE);
        SharedPreferences playerPrefs = getSharedPreferences("PlayerTags", Context.MODE_PRIVATE);
        SharedPreferences.Editor teamEditor = teamPrefs.edit();
        SharedPreferences.Editor playerEditor = playerPrefs.edit();

        for (PlayerData player : parsedData) {
            String playerTag = player.getPlayerTag();
            teamEditor.putString(playerTag, teamName);
            if (!playerPrefs.contains(playerTag)) {
                playerEditor.putBoolean(playerTag, true);
            }
        }
        teamEditor.apply();
        playerEditor.apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
