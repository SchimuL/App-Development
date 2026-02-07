package com.example.FCC_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailboxActivity extends AppCompatActivity {

    private TextInputEditText subjectInput, messageInput;
    private LinearLayout teamCheckboxContainer;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mailbox);

        Toolbar toolbar = findViewById(R.id.mailbox_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        subjectInput = findViewById(R.id.input_subject);
        messageInput = findViewById(R.id.input_message);
        teamCheckboxContainer = findViewById(R.id.team_checkbox_container);
        sendButton = findViewById(R.id.send_button);

        populateTeamCheckboxes();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void populateTeamCheckboxes() {
        String[] teams = getResources().getStringArray(R.array.mannschaften);
        for (String team : teams) {
            MaterialCheckBox checkBox = new MaterialCheckBox(this);
            checkBox.setText(team);
            teamCheckboxContainer.addView(checkBox);
        }
    }

    private void sendMessage() {
        String subject = subjectInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();
        List<String> selectedTeams = new ArrayList<>();

        for (int i = 0; i < teamCheckboxContainer.getChildCount(); i++) {
            MaterialCheckBox checkBox = (MaterialCheckBox) teamCheckboxContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                selectedTeams.add(checkBox.getText().toString());
            }
        }

        if (subject.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Bitte Betreff und Nachricht eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTeams.isEmpty()) {
            Toast.makeText(this, "Bitte mindestens eine Mannschaft auswählen.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a new post object
        Map<String, Object> post = new HashMap<>();
        post.put("subject", subject);
        post.put("message", message);
        post.put("targetTeams", selectedTeams);
        post.put("timestamp", new Date());
        post.put("readBy", new ArrayList<String>()); // To track read status

        // Add a new document with a generated ID to the "posts" collection
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MailboxActivity.this, "Mitteilung erfolgreich gesendet!", Toast.LENGTH_LONG).show();
                    finish(); // Go back to the trainer hub
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MailboxActivity.this, "Fehler beim Senden: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
