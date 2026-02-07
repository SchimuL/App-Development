package com.example.FCC_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserProfileData_Encrypted";
    private static final String KEY_NAME = "_name";
    private static final String KEY_BIRTHDAY = "_birthday";
    private static final String KEY_HEIGHT = "_height";
    private static final String KEY_WEIGHT = "_weight";
    private static final String KEY_GENDER_ID = "_gender_id";
    private static final String KEY_POSITION = "_position";
    private static final String KEY_TEAM = "_team";

    private TextInputEditText nameEditText, birthdayEditText, heightEditText, weightEditText;
    private RadioGroup genderRadioGroup;
    private Spinner positionSpinner, teamSpinner;
    private String loggedInPlayerTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Abgabe1_LG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        nameEditText = findViewById(R.id.edittext_name);
        birthdayEditText = findViewById(R.id.edittext_birthday);
        heightEditText = findViewById(R.id.edittext_height);
        weightEditText = findViewById(R.id.edittext_weight);
        genderRadioGroup = findViewById(R.id.radiogroup_gender);
        positionSpinner = findViewById(R.id.spinner_position);
        teamSpinner = findViewById(R.id.spinner_team);
        Button saveButton = findViewById(R.id.save_profile_button);
        Button oneRmButton = findViewById(R.id.one_rm_button);

        // Apply high contrast adapters to spinners
        setupHighContrastSpinner(positionSpinner, R.array.spieler_positionen);
        setupHighContrastSpinner(teamSpinner, R.array.mannschaften);

        if (loggedInPlayerTag != null) {
            loadProfileDataAsync();
        }

        saveButton.setOnClickListener(v -> saveProfileData());
        oneRmButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, OneRmActivity.class);
            intent.putExtra("PLAYER_TAG", loggedInPlayerTag);
            startActivity(intent);
        });
    }

    private void setupHighContrastSpinner(Spinner spinner, int arrayResId) {
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

    private SharedPreferences getEncryptedSharedPreferences() throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        return EncryptedSharedPreferences.create(
                PREFS_NAME, masterKeyAlias, this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private void loadProfileDataAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                SharedPreferences prefs = getEncryptedSharedPreferences();
                String name = prefs.getString(loggedInPlayerTag + KEY_NAME, "");
                String bday = prefs.getString(loggedInPlayerTag + KEY_BIRTHDAY, "");
                String h = prefs.getString(loggedInPlayerTag + KEY_HEIGHT, "");
                String w = prefs.getString(loggedInPlayerTag + KEY_WEIGHT, "");
                int gId = prefs.getInt(loggedInPlayerTag + KEY_GENDER_ID, -1);
                int pos = prefs.getInt(loggedInPlayerTag + KEY_POSITION, 0);
                int team = prefs.getInt(loggedInPlayerTag + KEY_TEAM, 0);

                handler.post(() -> {
                    nameEditText.setText(name); birthdayEditText.setText(bday);
                    heightEditText.setText(h); weightEditText.setText(w);
                    if (gId != -1) genderRadioGroup.check(gId);
                    positionSpinner.setSelection(pos); teamSpinner.setSelection(team);
                });
            } catch (Exception e) {}
        });
    }

    private void saveProfileData() {
        if (loggedInPlayerTag == null) return;
        try {
            SharedPreferences prefs = getEncryptedSharedPreferences();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(loggedInPlayerTag + KEY_NAME, nameEditText.getText().toString());
            editor.putString(loggedInPlayerTag + KEY_BIRTHDAY, birthdayEditText.getText().toString());
            editor.putString(loggedInPlayerTag + KEY_HEIGHT, heightEditText.getText().toString());
            editor.putString(loggedInPlayerTag + KEY_WEIGHT, weightEditText.getText().toString());
            editor.putInt(loggedInPlayerTag + KEY_GENDER_ID, genderRadioGroup.getCheckedRadioButtonId());
            editor.putInt(loggedInPlayerTag + KEY_POSITION, positionSpinner.getSelectedItemPosition());
            editor.putInt(loggedInPlayerTag + KEY_TEAM, teamSpinner.getSelectedItemPosition());
            editor.apply();
            Toast.makeText(this, "Profil gespeichert!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}