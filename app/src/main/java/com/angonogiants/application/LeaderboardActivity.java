
package com.angonogiants.application;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private static final String TAG = "LeaderboardActivity";

    private ListView leaderboardListView;
    private ImageButton mainMenuButton;
    private TextView emptyMessage;

    private final List<LeaderboardEntry> leaderboardList =
            new ArrayList<>();

    private LeaderboardAdapter adapter;

    private DatabaseReference leaderboardReference;
    private ValueEventListener leaderboardListener;

    private MediaPlayer backgroundMusic;
    private MediaPlayer buttonSound;

    private boolean navigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_leaderboard);

        initializeViews();
        initializeLeaderboard();
        initializeFirebase();
        initializeAudio();
        setupNavigation();
    }

    private void initializeViews() {
        leaderboardListView =
                findViewById(R.id.leaderboardListView);

        mainMenuButton =
                findViewById(R.id.mainMenuLButton);

        emptyMessage =
                findViewById(R.id.emptyLeaderboardText);
    }

    private void initializeLeaderboard() {
        adapter = new LeaderboardAdapter(
                this,
                leaderboardList
        );

        leaderboardListView.setAdapter(adapter);

        if (emptyMessage != null) {
            leaderboardListView.setEmptyView(emptyMessage);
        }
    }

    private void initializeFirebase() {
        leaderboardReference =
                new LeaderboardRepository()
                        .getLeaderboardReference();
    }

    private void attachLeaderboardListener() {
        if (leaderboardListener != null) return;

        leaderboardListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                List<LeaderboardEntry> results =
                        new ArrayList<>();

                for (DataSnapshot child :
                        snapshot.getChildren()) {

                    try {
                        PlayerScore score =
                                child.getValue(PlayerScore.class);

                        if (score == null) continue;

                        results.add(
                                new LeaderboardEntry(score)
                        );

                    } catch (RuntimeException error) {
                        Log.w(
                                TAG,
                                "Invalid leaderboard entry",
                                error
                        );
                    }
                }

                // Higher level first.
                // Higher score breaks level ties.
                results.sort((a, b) -> {
                    int levelComparison =
                            Integer.compare(
                                    b.highestLevel,
                                    a.highestLevel
                            );

                    if (levelComparison != 0) {
                        return levelComparison;
                    }

                    int scoreComparison =
                            Integer.compare(
                                    b.score,
                                    a.score
                            );

                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    String nameA = a.playerName == null
                            ? ""
                            : a.playerName;

                    String nameB = b.playerName == null
                            ? ""
                            : b.playerName;

                    return nameA.compareToIgnoreCase(nameB);
                });

                leaderboardList.clear();
                leaderboardList.addAll(results);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
                Log.e(
                        TAG,
                        "Could not load leaderboard",
                        error.toException()
                );

                Toast.makeText(
                        LeaderboardActivity.this,
                        "Failed to load leaderboard.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        leaderboardReference.addValueEventListener(
                leaderboardListener
        );
    }

    private void detachLeaderboardListener() {
        if (leaderboardListener != null) {
            leaderboardReference.removeEventListener(
                    leaderboardListener
            );

            leaderboardListener = null;
        }
    }

    private void initializeAudio() {
        backgroundMusic = MediaPlayer.create(
                this,
                R.raw.leaderboardmuzic
        );

        if (backgroundMusic != null) {
            backgroundMusic.setLooping(true);
        }

        buttonSound = MediaPlayer.create(
                this,
                R.raw.buttonsound
        );
    }

    private void playMusic() {
        if (backgroundMusic == null) return;

        try {
            if (!backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            }
        } catch (IllegalStateException error) {
            Log.e(TAG, "Cannot play music", error);
        }
    }

    private void pauseMusic() {
        if (backgroundMusic == null) return;

        try {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            }
        } catch (IllegalStateException error) {
            Log.e(TAG, "Cannot pause music", error);
        }
    }

    private void setupNavigation() {
        mainMenuButton.setOnClickListener(
                v -> navigateToMainMenu()
        );

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navigateToMainMenu();
                    }
                }
        );
    }

    private void navigateToMainMenu() {
        if (navigating || isFinishing()) return;

        navigating = true;
        mainMenuButton.setEnabled(false);

        if (buttonSound != null) {
            try {
                buttonSound.seekTo(0);
                buttonSound.start();
            } catch (IllegalStateException ignored) {
            }
        }

        Drawable drawable =
                mainMenuButton.getBackground();

        if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).start();
        }

        startActivity(
                new Intent(this, MainActivity.class)
        );

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachLeaderboardListener();
        playMusic();
    }

    @Override
    protected void onStop() {
        detachLeaderboardListener();
        pauseMusic();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        detachLeaderboardListener();

        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }

        if (buttonSound != null) {
            buttonSound.release();
            buttonSound = null;
        }

        super.onDestroy();
    }
}
