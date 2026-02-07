package com.example.FCC_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateExerciseActivity extends AppCompatActivity {

    private TextInputEditText exerciseNameEditText;
    private TextInputEditText exerciseCommentEditText;
    private TextView videoStatusTextView;
    private ProgressBar uploadProgressBar;
    private Button saveButton;

    private Uri videoUri = null;

    // Launcher for picking a video from the gallery
    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                videoUri = result.getData().getData();
                videoStatusTextView.setText("Video ausgewählt: " + videoUri.getLastPathSegment());
            } else {
                Toast.makeText(this, "Kein Video ausgewählt", Toast.LENGTH_SHORT).show();
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exercise);

        Toolbar toolbar = findViewById(R.id.toolbar_create_exercise);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        exerciseNameEditText = findViewById(R.id.edit_exercise_name);
        exerciseCommentEditText = findViewById(R.id.edit_exercise_comment);
        Button selectVideoButton = findViewById(R.id.btn_select_video);
        videoStatusTextView = findViewById(R.id.txt_video_status);
        uploadProgressBar = findViewById(R.id.progress_upload);
        saveButton = findViewById(R.id.btn_save_exercise);

        selectVideoButton.setOnClickListener(v -> openVideoPicker());
        saveButton.setOnClickListener(v -> saveExercise());
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        videoPickerLauncher.launch(intent);
    }

    private void saveExercise() {
        String exerciseName = exerciseNameEditText.getText().toString().trim();
        String comment = exerciseCommentEditText.getText().toString().trim();

        if (exerciseName.isEmpty()) {
            exerciseNameEditText.setError("Name ist erforderlich");
            return;
        }

        if (videoUri == null) {
            Toast.makeText(this, "Bitte ein Video auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // 1. Upload video to Firebase Storage
        final StorageReference videoRef = FirebaseStorage.getInstance().getReference()
                .child("exercise_videos/" + UUID.randomUUID().toString());

        videoRef.putFile(videoUri)
            .addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                uploadProgressBar.setProgress((int) progress);
            })
            .addOnSuccessListener(taskSnapshot -> videoRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                // 2. Save exercise metadata to Firestore
                saveExerciseToFirestore(exerciseName, comment, downloadUri.toString());
            }))
            .addOnFailureListener(e -> {
                Toast.makeText(CreateExerciseActivity.this, "Upload fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                setLoading(false);
            });
    }

    private void saveExerciseToFirestore(String name, String comment, String videoUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("name", name);
        exercise.put("comment", comment);
        exercise.put("videoUrl", videoUrl);
        exercise.put("createdAt", System.currentTimeMillis());

        db.collection("custom_exercises")
            .add(exercise)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(CreateExerciseActivity.this, "Übung erfolgreich gespeichert!", Toast.LENGTH_SHORT).show();
                setLoading(false);
                finish(); // Go back to the previous screen
            })
            .addOnFailureListener(e -> {
                Toast.makeText(CreateExerciseActivity.this, "Speichern in DB fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                setLoading(false);
            });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            uploadProgressBar.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
        } else {
            uploadProgressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}