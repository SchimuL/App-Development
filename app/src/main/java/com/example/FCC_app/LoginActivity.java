package com.example.FCC_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class LoginActivity extends AppCompatActivity {

    // The master password
    private static final String TRAINER_PASSWORD = "FCC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        EditText passwordEditText = findViewById(R.id.password_edittext);
        Button loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {
            String enteredPassword = passwordEditText.getText().toString();

            if (enteredPassword.equals(TRAINER_PASSWORD)) {
                // Password is correct
                Toast.makeText(this, "Login erfolgreich!", Toast.LENGTH_SHORT).show();
                // Go to the new central Trainer Hub
                Intent intent = new Intent(LoginActivity.this, TrainerHubActivity.class);
                startActivity(intent);
                finish(); // Close the login screen
            } else {
                // Password is incorrect
                Toast.makeText(this, "Falsches Passwort", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // This makes the arrow act like a back button
        return true;
    }
}
