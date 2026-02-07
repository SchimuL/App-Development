package com.example.FCC_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {

    private String loggedInPlayerTag;
    private String playerTeam;

    private RecyclerView mailboxRecyclerView;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        // --- Header ---
        TextView welcomeText = findViewById(R.id.welcome_text);
        ImageButton profileIconButton = findViewById(R.id.profile_icon_button);
        if (loggedInPlayerTag != null) {
            welcomeText.setText(loggedInPlayerTag);
        }

        // --- Category Pills ---
        findViewById(R.id.pill_logbook).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, WorkoutLogActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        findViewById(R.id.pill_calendar).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PlayerCalendarActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        findViewById(R.id.pill_wellbeing).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, WellbeingActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        findViewById(R.id.pill_injury).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PainMapActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        // --- Featured Card ---
        findViewById(R.id.weiter_button).setOnClickListener(v -> showPreTrainingDialog());

        // --- Bottom Grid ---
        findViewById(R.id.card_plan_create).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ExerciseSelectionActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        findViewById(R.id.card_trainer_plan).setOnClickListener(v -> {
            if (loggedInPlayerTag == null) return;
            SharedPreferences prefs = getSharedPreferences("TrainerPlans", Context.MODE_PRIVATE);
            String planJson = prefs.getString(loggedInPlayerTag, null);
            if (planJson != null) {
                Intent intent = new Intent(HomeActivity.this, CustomPlanActivity.class);
                intent.putExtra("PLAN_JSON", planJson);
                intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Kein Trainer-Plan für dich gefunden", Toast.LENGTH_SHORT).show();
            }
        });

        profileIconButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });

        db = FirebaseFirestore.getInstance();
        setupMailboxRecyclerView();

        if (loggedInPlayerTag != null) {
            loadPlayerTeamAndListenForPosts();
        }
    }

    private void loadPlayerTeamAndListenForPosts() {
        SharedPreferences teamPrefs = getSharedPreferences("TeamAssignments", Context.MODE_PRIVATE);
        playerTeam = teamPrefs.getString(loggedInPlayerTag, null);
        if (playerTeam != null && !playerTeam.isEmpty()) {
            listenForPosts();
        }
    }

    private void setupMailboxRecyclerView() {
        mailboxRecyclerView = findViewById(R.id.mailbox_recyclerview);
        mailboxRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        postAdapter = new PostAdapter(postList, loggedInPlayerTag, this);
        mailboxRecyclerView.setAdapter(postAdapter);
    }

    private void listenForPosts() {
        db.collection("posts")
                .whereArrayContains("targetTeams", playerTeam)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5) 
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        postList.clear();
                        postList.addAll(snapshots.toObjects(Post.class));
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onPostClick(Post post) {
        if (post == null || post.getDocumentId() == null) return;
        new AlertDialog.Builder(this)
                .setTitle(post.getSubject())
                .setMessage(post.getMessage())
                .setPositiveButton("Schließen", null)
                .show();
    }
    
    private void showPreTrainingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pre_training_selection, null);

        SwitchMaterial verletzungSwitch = dialogView.findViewById(R.id.dialog_verletzung_switch);
        TableRow verletzungDetailsRow = dialogView.findViewById(R.id.dialog_verletzung_details_row);
        TableRow trainingszielDetailsRow = dialogView.findViewById(R.id.dialog_trainingsziel_details_row);
        Spinner verletzungSpinner = dialogView.findViewById(R.id.dialog_verletzung_spinner);
        Spinner trainingszielSpinner = dialogView.findViewById(R.id.dialog_trainingsziel_spinner);

        // --- Custom Adapters for High Contrast ---
        setupContrastSpinner(verletzungSpinner, R.array.verletzungsarten);
        setupContrastSpinner(trainingszielSpinner, R.array.trainingsziele);

        trainingszielDetailsRow.setVisibility(View.VISIBLE);
        verletzungDetailsRow.setVisibility(View.GONE);

        verletzungSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            verletzungDetailsRow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            trainingszielDetailsRow.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        builder.setView(dialogView)
                .setPositiveButton("Weiter", (dialog, id) -> {
                    Intent intent = new Intent(HomeActivity.this, DetailActivity.class);
                    String auswahl = verletzungSwitch.isChecked() ? verletzungSpinner.getSelectedItem().toString() : trainingszielSpinner.getSelectedItem().toString();
                    intent.putExtra(DetailActivity.EXTRA_AUSWAHL, auswahl);
                    intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
                    startActivity(intent);
                })
                .setNegativeButton("Abbrechen", (dialog, id) -> dialog.cancel());

        builder.create().show();
    }

    private void setupContrastSpinner(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(arrayResId)) {
            @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
            @Override public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(Color.parseColor("#1E1E1E"));
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}