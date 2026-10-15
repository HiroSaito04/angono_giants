//app\src\main\java\com\angonogiants\application\LoadingActivity.java
package com.angonogiants.application;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button skipButton;
    private String playerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        videoView = findViewById(R.id.videoBackground);
        skipButton = findViewById(R.id.skipButton);

        playerName = getIntent().getStringExtra("PLAYER_NAME");

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.loadingpagevideo);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            videoView.start();
        });

        skipButton.setOnClickListener(v -> {
            // The delay is likely unnecessary if you're immediately starting the game
            // new Handler().postDelayed(() -> skipButton.setVisibility(View.VISIBLE), 3000);  <- Removed
            startGameplay();
        });

        videoView.setOnCompletionListener(mp -> {
            startGameplay();
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }


    private void startGameplay() {
        Intent intent = new Intent(LoadingActivity.this, GameActivity.class);
        intent.putExtra("PLAYER_NAME", playerName);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}