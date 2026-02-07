package com.example.FCC_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class TrainerTeamSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_team_selection);

        Toolbar toolbar = findViewById(R.id.team_selection_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        TextView headerTitle = findViewById(R.id.team_selection_header_title);
        headerTitle.setText("Mannschaft wählen");

        ListView teamListView = findViewById(R.id.team_list_view);
        String[] teams = getResources().getStringArray(R.array.mannschaften);
        
        // Use the custom layout with white text
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_simple_text, teams);
        teamListView.setAdapter(adapter);

        teamListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTeam = (String) parent.getItemAtPosition(position);
            String targetActivityName = getIntent().getStringExtra("TARGET_ACTIVITY");

            Intent intent;
            if ("WELLBEING".equals(targetActivityName)) {
                intent = new Intent(TrainerTeamSelectionActivity.this, TrainerDashboardActivity.class);
            } else if ("INJURY_REPORT".equals(targetActivityName)) {
                intent = new Intent(TrainerTeamSelectionActivity.this, InjuryReportActivity.class);
            } else {
                intent = new Intent(TrainerTeamSelectionActivity.this, PlayerSelectionActivity.class);
                intent.putExtra("TARGET_ACTIVITY", "PLAYER_FILES");
            }
            intent.putExtra("SELECTED_TEAM", selectedTeam);
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
