package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerManagementActivity extends AppCompatActivity {

    private ListView playerListView;
    private ArrayAdapter<String> adapter;
    private List<String> playerList = new ArrayList<>();
    private SharedPreferences profilePrefs;
    private String[] teamOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_management);

        Toolbar toolbar = findViewById(R.id.player_management_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        profilePrefs = getSharedPreferences("UserProfileData", Context.MODE_PRIVATE);
        teamOptions = getResources().getStringArray(R.array.mannschaften);

        playerListView = findViewById(R.id.player_management_list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playerList);
        playerListView.setAdapter(adapter);

        loadPlayers();

        FloatingActionButton fab = findViewById(R.id.fab_add_player);
        fab.setOnClickListener(v -> showAddEditDialog(null));

        playerListView.setOnItemClickListener((parent, view, position, id) -> {
            String playerTag = playerList.get(position);
            showPlayerOptions(playerTag);
        });
    }

    private void loadPlayers() {
        playerList.clear();
        Set<String> allKeys = profilePrefs.getAll().keySet();
        Set<String> playerTags = new HashSet<>();
        for (String key : allKeys) {
            if (key.endsWith("_name")) {
                playerTags.add(key.substring(0, key.length() - "_name".length()));
            }
        }
        playerList.addAll(playerTags);
        Collections.sort(playerList);
        adapter.notifyDataSetChanged();
    }

    private void showPlayerOptions(String playerTag) {
        String[] options = {"Bearbeiten", "Löschen"};
        new AlertDialog.Builder(this)
                .setTitle(playerTag)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddEditDialog(playerTag);
                    } else {
                        deletePlayer(playerTag);
                    }
                })
                .show();
    }

    private void showAddEditDialog(String playerTag) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_player, null);
        EditText firstNameEdit = dialogView.findViewById(R.id.edit_text_first_name);
        EditText lastNameEdit = dialogView.findViewById(R.id.edit_text_last_name);
        Spinner teamSpinner = dialogView.findViewById(R.id.spinner_team);

        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamOptions);
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSpinner.setAdapter(teamAdapter);

        if (playerTag != null) {
            String fullName = profilePrefs.getString(playerTag + "_name", "");
            if (fullName.contains(" ")) {
                int lastSpace = fullName.lastIndexOf(" ");
                firstNameEdit.setText(fullName.substring(0, lastSpace));
                lastNameEdit.setText(fullName.substring(lastSpace + 1));
            } else {
                firstNameEdit.setText(fullName);
            }
            int teamIndex = profilePrefs.getInt(playerTag + "_team", 0);
            teamSpinner.setSelection(teamIndex);
        }

        new AlertDialog.Builder(this)
                .setTitle(playerTag == null ? "Neuer Spieler" : "Spieler bearbeiten")
                .setView(dialogView)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String first = firstNameEdit.getText().toString().trim();
                    String last = lastNameEdit.getText().toString().trim();
                    int teamIdx = teamSpinner.getSelectedItemPosition();

                    if (!first.isEmpty() && !last.isEmpty()) {
                        savePlayer(playerTag, first, last, teamIdx);
                    } else {
                        Toast.makeText(this, "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void savePlayer(String oldTag, String first, String last, int teamIdx) {
        String newTag = first + " " + last;
        SharedPreferences.Editor editor = profilePrefs.edit();
        
        if (oldTag != null && !oldTag.equals(newTag)) {
            // Remove old tag data if name changed
            editor.remove(oldTag + "_name");
            editor.remove(oldTag + "_team");
            // Note: In a real app, you'd want to migrate performance/wellbeing data too
        }

        editor.putString(newTag + "_name", newTag);
        editor.putInt(newTag + "_team", teamIdx);
        editor.apply();

        loadPlayers();
        Toast.makeText(this, "Gespeichert", Toast.LENGTH_SHORT).show();
    }

    private void deletePlayer(String playerTag) {
        new AlertDialog.Builder(this)
                .setTitle("Spieler löschen")
                .setMessage("Möchten Sie " + playerTag + " wirklich löschen?")
                .setPositiveButton("Ja", (dialog, which) -> {
                    profilePrefs.edit()
                            .remove(playerTag + "_name")
                            .remove(playerTag + "_team")
                            .apply();
                    loadPlayers();
                })
                .setNegativeButton("Nein", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
