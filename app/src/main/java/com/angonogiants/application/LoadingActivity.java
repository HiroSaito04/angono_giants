//app\src\main\java\com\angonogiants\application\LoadingActivity.java
package com.angonogiants.application;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoadingActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button skipButton;

    private String playerName;
    private int levelNumber;
    private boolean isGuest;

    private boolean launched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);

        videoView =
                findViewById(R.id.videoBackground);

        skipButton =
                findViewById(R.id.skipButton);

        playerName = getIntent().getStringExtra(
                "PLAYER_NAME"
        );

        if (playerName == null) {
            playerName = "Guest";
        }

        levelNumber = Math.max(
                1,
                getIntent().getIntExtra(
                        "LEVEL_NUMBER",
                        1
                )
        );

        isGuest = getIntent().getBooleanExtra(
                "IS_GUEST",
                FirebaseAuth.getInstance()
                        .getCurrentUser() == null
        );

        Uri videoUri = Uri.parse(
                "android.resource://"
                        + getPackageName()
                        + "/"
                        + R.raw.loadingpagevideo
        );

        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(player -> {
            player.setLooping(false);

            player.setVideoScalingMode(
                    MediaPlayer
                            .VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            );

            videoView.start();
        });

        videoView.setOnCompletionListener(
                player -> startGameplay()
        );

        videoView.setOnErrorListener(
                (player, what, extra) -> {
                    startGameplay();
                    return true;
                }
        );

        skipButton.setOnClickListener(
                v -> startGameplay()
        );

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Loading screen back disabled.
                    }
                }
        );
    }

    private void startGameplay() {
        if (launched || isFinishing()) {
            return;
        }

        launched = true;

        videoView.stopPlayback();

        Intent intent = new Intent(
                this,
                GameActivity.class
        );

        intent.putExtra(
                "PLAYER_NAME",
                playerName
        );

        intent.putExtra(
                "LEVEL_NUMBER",
                levelNumber
        );

        intent.putExtra(
                "IS_GUEST",
                isGuest
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );

        finish();
    }

    @Override
    protected void onDestroy() {
        if (videoView != null) {
            videoView.stopPlayback();
        }

        super.onDestroy();
    }
}
