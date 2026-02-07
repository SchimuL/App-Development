package com.example.FCC_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PlayerSelectionActivity extends AppCompatActivity {

    private String selectedTeam;
    private String targetActivityName;
    private final List<String> playerList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_selection);

        selectedTeam = getIntent().getStringExtra("SELECTED_TEAM");
        targetActivityName = getIntent().getStringExtra("TARGET_ACTIVITY");

        Toolbar toolbar = findViewById(R.id.player_selection_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (selectedTeam != null) {
                getSupportActionBar().setTitle("Spieler in: " + selectedTeam);
            } else {
                Toast.makeText(this, "Fehler: Kein Team ausgewählt", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        ListView playerListView = findViewById(R.id.player_list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playerList);
        playerListView.setAdapter(adapter);

        loadPlayersForTeam();

        playerListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlayerTag = playerList.get(position);
            Intent intent;
            if (targetActivityName != null) {
                switch (targetActivityName) {
                    case "PERFORMANCE":
                        intent = new Intent(PlayerSelectionActivity.this, PlayerPerformanceActivity.class);
                        break;
                    case "PLAYER_FILES":
                        intent = new Intent(PlayerSelectionActivity.this, PlayerFileActivity.class);
                        break;
                    default:
                        intent = new Intent(PlayerSelectionActivity.this, PlayerFileActivity.class);
                        break;
                }
            } else {
                intent = new Intent(PlayerSelectionActivity.this, PlayerFileActivity.class);
            }
            intent.putExtra("PLAYER_TAG", selectedPlayerTag);
            startActivity(intent);
        });
    }

    private void loadPlayersForTeam() {
        playerList.clear();
        SharedPreferences teamPrefs = getSharedPreferences("TeamAssignments", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = teamPrefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue().equals(selectedTeam)) {
                playerList.add(entry.getKey());
            }
        }

        Collections.sort(playerList);
        adapter.notifyDataSetChanged();

        if (playerList.isEmpty()) {
            Toast.makeText(this, "Keine Spieler in diesem Team gefunden.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
