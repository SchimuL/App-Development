package com.example.FCC_app;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class VideoPlayerActivity extends AppCompatActivity {

    // Using simple names for extras is fine
    public static final String EXTRA_VIDEO_PATH = "VIDEO_NAME";
    public static final String EXTRA_IS_URL = "IS_URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Toolbar toolbar = findViewById(R.id.video_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        VideoView videoView = findViewById(R.id.video_view);
        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        boolean isUrl = getIntent().getBooleanExtra(EXTRA_IS_URL, false);

        if (videoPath != null && !videoPath.isEmpty()) {
            Uri uri;
            if (isUrl) {
                // It's a full URL from Firebase Storage
                uri = Uri.parse(videoPath);
            } else {
                // It's a local file name, build the resource path
                String path = "android.resource://" + getPackageName() + "/raw/" + videoPath;
                uri = Uri.parse(path);
            }

            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);

            videoView.setOnPreparedListener(mp -> videoView.start());
            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Video konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
                return true;
            });

        } else {
            Toast.makeText(this, "Kein Video für diese Übung verfügbar.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}