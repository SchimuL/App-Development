package com.example.FCC_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TrainerHubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_hub);

        // --- Header ---
        TextView trainerNameText = findViewById(R.id.trainer_name_text);
        // You could set the actual trainer name here if available

        // --- Featured Card: Calendar ---
        View calendarCard = findViewById(R.id.card_goto_calendar);
        calendarCard.setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, TrainerCalendarActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.button_goto_calendar_internal).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, TrainerCalendarActivity.class);
            startActivity(intent);
        });

        // --- Grid Actions ---
        findViewById(R.id.card_goto_player_files).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, TrainerTeamSelectionActivity.class);
            intent.putExtra("TARGET_ACTIVITY", "PLAYER_FILES");
            startActivity(intent);
        });

        findViewById(R.id.card_goto_plan_creation).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, TrainerPlanActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.card_create_exercise).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, CreateExerciseActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.card_goto_wellbeing).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, TrainerTeamSelectionActivity.class);
            intent.putExtra("TARGET_ACTIVITY", "WELLBEING");
            startActivity(intent);
        });

        findViewById(R.id.card_goto_injury).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, TrainerTeamSelectionActivity.class);
            intent.putExtra("TARGET_ACTIVITY", "INJURY_REPORT");
            startActivity(intent);
        });

        findViewById(R.id.card_goto_polar).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, PolarDataActivity.class);
            startActivity(intent);
        });

        // --- Bottom Card: Mailbox ---
        findViewById(R.id.card_goto_mailbox).setOnClickListener(v -> {
            Intent intent = new Intent(TrainerHubActivity.this, MailboxActivity.class);
            startActivity(intent);
        });
    }
}