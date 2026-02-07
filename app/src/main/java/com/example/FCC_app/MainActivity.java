package com.example.FCC_app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText firstNameInput, lastNameInput, jerseyNumberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ZUERST: Splash-Theme durch Haupt-Theme ersetzen
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        
        // Initialisierung der Zeit-Bibliothek für den Kalender
        AndroidThreeTen.init(this);
        
        setContentView(R.layout.activity_main);

        firstNameInput = findViewById(R.id.first_name_input);
        lastNameInput = findViewById(R.id.last_name_input);
        jerseyNumberInput = findViewById(R.id.jersey_number_input);
        Button loginButton = findViewById(R.id.login_button);
        TextView trainerLoginLink = findViewById(R.id.trainer_login_link);

        requestNotificationPermission();

        loginButton.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String jerseyNumber = jerseyNumberInput.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || jerseyNumber.isEmpty()) {
                Toast.makeText(MainActivity.this, "Bitte fülle alle Felder aus", Toast.LENGTH_SHORT).show();
                return;
            }

            String playerTag = ("" + firstName.charAt(0) + lastName.charAt(0) + jerseyNumber).toUpperCase();
            savePlayerTag(playerTag);
            registerDeviceToken(playerTag);

            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("PLAYER_TAG", playerTag);
            startActivity(intent);
        });

        trainerLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void registerDeviceToken(String playerTag) {
        try {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                Map<String, Object> data = new HashMap<>();
                data.put("fcmToken", token);
                data.put("lastActive", System.currentTimeMillis());

                FirebaseFirestore.getInstance().collection("player_tokens")
                        .document(playerTag)
                        .set(data);
            });
        } catch (Exception e) {
            // Silently fail if Firebase is not properly configured yet
        }
    }

    private void savePlayerTag(String playerTag) {
        SharedPreferences prefs = getSharedPreferences("PlayerTags", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(playerTag, true);
        editor.apply();
    }
}