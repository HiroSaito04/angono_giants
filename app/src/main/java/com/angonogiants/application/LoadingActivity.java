
package com.angonogiants.application;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoadingActivity extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";

    private static final String EXTRA_PLAYER_NAME =
            "PLAYER_NAME";

    private static final String EXTRA_LEVEL_NUMBER =
            "LEVEL_NUMBER";

    private static final String STATE_VIDEO_POSITION =
            "video_position";

    private VideoView videoView;
    private Button skipButton;

    private String playerName;
    private int levelNumber = 1;
    private int videoPosition = 0;

    private boolean prepared = false;
    private boolean foreground = false;
    private boolean navigating = false;
    private boolean videoFailed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance()
                .getCurrentUser() == null) {

            startActivity(
                    new Intent(this, AuthActivity.class)
            );

            finish();
            return;
        }

        setContentView(R.layout.activity_loading);

        initializeViews();
        retrievePlayerData();

        if (playerName == null ||
                playerName.trim().isEmpty()) {

            startActivity(
                    new Intent(this, MainActivity.class)
            );

            finish();
            return;
        }

        if (savedInstanceState != null) {
            videoPosition = savedInstanceState.getInt(
                    STATE_VIDEO_POSITION,
                    0
            );
        }

        setupVideo();
        setupSkipButton();
        setupBackNavigation();
    }

    private void initializeViews() {
        videoView = findViewById(R.id.videoBackground);
        skipButton = findViewById(R.id.skipButton);
    }

    private void retrievePlayerData() {
        Intent intent = getIntent();

        playerName = intent.getStringExtra(
                EXTRA_PLAYER_NAME
        );

        levelNumber = Math.max(
                1,
                intent.getIntExtra(EXTRA_LEVEL_NUMBER, 1)
        );
    }

    // -----------------------------------------
    // Video
    // -----------------------------------------

    private void setupVideo() {
        Uri uri = Uri.parse(
                "android.resource://"
                        + getPackageName()
                        + "/"
                        + R.raw.loadingpagevideo
        );

        videoView.setOnPreparedListener(
                this::onVideoPrepared
        );

        videoView.setOnCompletionListener(
                player -> startGameplay()
        );

        videoView.setOnErrorListener(
                (player, what, extra) -> {
                    Log.e(
                            TAG,
                            "Video failed: " + what
                                    + "/" + extra
                    );

                    videoFailed = true;

                    if (foreground) {
                        startGameplay();
                    }

                    return true;
                }
        );

        videoView.setVideoURI(uri);
    }

    private void onVideoPrepared(MediaPlayer player) {
        if (navigating) return;

        prepared = true;

        player.setLooping(false);

        player.setVideoScalingMode(
                MediaPlayer
                        .VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        );

        if (videoPosition > 0) {
            videoView.seekTo(videoPosition);
        }

        if (foreground) {
            videoView.start();
        }
    }

    private void setupSkipButton() {
        skipButton.setOnClickListener(v ->
                startGameplay()
        );
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Preserve the original disabled Back.
                    }
                }
        );
    }

    // -----------------------------------------
    // Navigation
    // -----------------------------------------

    private void startGameplay() {
        if (navigating ||
                isFinishing() ||
                isDestroyed()) {
            return;
        }

        navigating = true;
        skipButton.setEnabled(false);

        videoView.stopPlayback();

        Intent intent = new Intent(
                this,
                GameActivity.class
        );

        intent.putExtra(
                EXTRA_PLAYER_NAME,
                playerName
        );

        intent.putExtra(
                EXTRA_LEVEL_NUMBER,
                levelNumber
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );

        finish();
    }

    // -----------------------------------------
    // Lifecycle
    // -----------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        foreground = true;

        if (videoFailed && !navigating) {
            startGameplay();
            return;
        }

        if (prepared && !navigating) {
            if (videoPosition > 0) {
                videoView.seekTo(videoPosition);
            }

            videoView.start();
        }
    }

    @Override
    protected void onPause() {
        foreground = false;

        if (prepared && !navigating) {
            videoPosition =
                    videoView.getCurrentPosition();

            if (videoView.isPlaying()) {
                videoView.pause();
            }
        }

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (prepared && !navigating) {
            videoPosition =
                    videoView.getCurrentPosition();
        }

        outState.putInt(
                STATE_VIDEO_POSITION,
                videoPosition
        );

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (videoView != null) {
            videoView.setOnPreparedListener(null);
            videoView.setOnCompletionListener(null);
            videoView.setOnErrorListener(null);

            videoView.stopPlayback();
        }

        super.onDestroy();
    }
}
