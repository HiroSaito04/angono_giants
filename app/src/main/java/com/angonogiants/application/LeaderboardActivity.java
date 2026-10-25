//app/src/main/java/com/angonogiants/application/LeaderboardActivity.java
package com.angonogiants.application;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity
        extends AppCompatActivity {

    private static final String TAG =
            "LeaderboardActivity";

    private static final String DATABASE_URL =
            "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private MediaPlayer leaderboardMusic;
    private MediaPlayer buttonSound;

    private ImageButton mainMenuLButton;
    private ListView leaderboardListView;
    private TextView emptyLeaderboardText;

    private final List<LeaderboardEntry> entries =
            new ArrayList<>();

    private LeaderboardAdapter leaderboardAdapter;

    private DatabaseReference leaderboardRef;
    private ValueEventListener leaderboardListener;

    private boolean navigating = false;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        // Use the XML provided for this screen.
        setContentView(
                R.layout.activity_leaderboard
        );

        initializeViews();
        initializeLeaderboard();
        initializeAudio();
        initializeButtons();
        loadLeaderboard();
    }

    private void initializeViews() {
        leaderboardListView =
                findViewById(
                        R.id.leaderboardListView
                );

        emptyLeaderboardText =
                findViewById(
                        R.id.emptyLeaderboardText
                );

        mainMenuLButton =
                findViewById(
                        R.id.mainMenuLButton
                );
    }

    private void initializeLeaderboard() {
        leaderboardAdapter =
                new LeaderboardAdapter(
                        this,
                        entries
                );

        leaderboardListView.setAdapter(
                leaderboardAdapter
        );

        emptyLeaderboardText.setText(
                "Loading leaderboard..."
        );

        emptyLeaderboardText.setVisibility(
                View.VISIBLE
        );

        leaderboardListView.setVisibility(
                View.GONE
        );
    }

    private void initializeAudio() {
        buttonSound = MediaPlayer.create(
                this,
                R.raw.buttonsound
        );

        leaderboardMusic = MediaPlayer.create(
                this,
                R.raw.leaderboardmuzic
        );

        if (leaderboardMusic != null) {
            leaderboardMusic.setLooping(true);
        }
    }

    private void initializeButtons() {
        mainMenuLButton.setOnClickListener(v -> {
            returnToMainMenu();
        });
    }

    private void returnToMainMenu() {
        if (navigating) {
            return;
        }

        navigating = true;

        playButtonSound();

        if (mainMenuLButton.getBackground()
                instanceof AnimationDrawable) {

            AnimationDrawable animation =
                    (AnimationDrawable)
                            mainMenuLButton
                                    .getBackground();

            animation.stop();
            animation.start();
        }

        Intent intent = new Intent(
                this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );

        finish();
    }

    private void playButtonSound() {
        if (buttonSound == null) {
            return;
        }

        try {
            buttonSound.seekTo(0);
            buttonSound.start();
        } catch (IllegalStateException e) {
            Log.w(
                    TAG,
                    "Button sound unavailable",
                    e
            );
        }
    }

    private void loadLeaderboard() {
        leaderboardRef = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("leaderboard");

        leaderboardListener =
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        entries.clear();

                        for (DataSnapshot child :
                                snapshot.getChildren()) {

                            String playerName = child
                                    .child("playerName")
                                    .getValue(String.class);

                            if (playerName == null
                                    || playerName.trim()
                                    .isEmpty()) {
                                continue;
                            }

                            LeaderboardEntry entry =
                                    new LeaderboardEntry();

                            entry.uid = child
                                    .child("uid")
                                    .getValue(String.class);

                            if (entry.uid == null) {
                                entry.uid = child.getKey();
                            }

                            entry.playerName =
                                    playerName.trim();

                            entry.highestLevel =
                                    Math.max(
                                            1,
                                            readInt(
                                                    child,
                                                    "highestLevel",
                                                    1
                                            )
                                    );

                            entry.score = readInt(
                                    child,
                                    "score",
                                    0
                            );

                            entry.wavesSurvived =
                                    Math.max(
                                            0,
                                            readInt(
                                                    child,
                                                    "wavesSurvived",
                                                    0
                                            )
                                    );

                            entry.timePlayed = child
                                    .child("timePlayed")
                                    .getValue(String.class);

                            if (entry.timePlayed == null
                                    || entry.timePlayed
                                    .isEmpty()) {
                                entry.timePlayed =
                                        "Unknown";
                            }

                            entries.add(entry);
                        }

                        sortLeaderboard();

                        leaderboardAdapter
                                .notifyDataSetChanged();

                        updateEmptyState();

                        Log.d(
                                TAG,
                                "Loaded " + entries.size()
                                        + " leaderboard players"
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Leaderboard read failed",
                                error.toException()
                        );

                        entries.clear();

                        leaderboardAdapter
                                .notifyDataSetChanged();

                        leaderboardListView.setVisibility(
                                View.GONE
                        );

                        emptyLeaderboardText.setText(
                                "Could not load leaderboard: "
                                        + error.getMessage()
                        );

                        emptyLeaderboardText.setVisibility(
                                View.VISIBLE
                        );
                    }
                };

        leaderboardRef.addValueEventListener(
                leaderboardListener
        );
    }

    private int readInt(
            DataSnapshot snapshot,
            String fieldName,
            int defaultValue
    ) {
        Object value = snapshot
                .child(fieldName)
                .getValue();

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    private void sortLeaderboard() {
        Collections.sort(
                entries,
                (first, second) -> {

                    // Primary: highest level first.
                    int levelComparison =
                            Integer.compare(
                                    second.highestLevel,
                                    first.highestLevel
                            );

                    if (levelComparison != 0) {
                        return levelComparison;
                    }

                    // Secondary: highest score first.
                    int scoreComparison =
                            Integer.compare(
                                    second.score,
                                    first.score
                            );

                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    // Stable alphabetical tie breaker.
                    return first.playerName
                            .compareToIgnoreCase(
                                    second.playerName
                            );
                }
        );
    }

    private void updateEmptyState() {
        if (entries.isEmpty()) {
            leaderboardListView.setVisibility(
                    View.GONE
            );

            emptyLeaderboardText.setText(
                    "No scores yet. Be the first "
                            + "to climb the levels!"
            );

            emptyLeaderboardText.setVisibility(
                    View.VISIBLE
            );

        } else {
            emptyLeaderboardText.setVisibility(
                    View.GONE
            );

            leaderboardListView.setVisibility(
                    View.VISIBLE
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (leaderboardMusic != null) {
            try {
                if (!leaderboardMusic.isPlaying()) {
                    leaderboardMusic.start();
                }
            } catch (IllegalStateException e) {
                Log.w(
                        TAG,
                        "Music playback failed",
                        e
                );
            }
        }
    }

    @Override
    protected void onStop() {
        if (leaderboardMusic != null) {
            try {
                if (leaderboardMusic.isPlaying()) {
                    leaderboardMusic.pause();
                }
            } catch (IllegalStateException ignored) {
            }
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (leaderboardRef != null
                && leaderboardListener != null) {

            leaderboardRef.removeEventListener(
                    leaderboardListener
            );
        }

        if (leaderboardMusic != null) {
            leaderboardMusic.release();
            leaderboardMusic = null;
        }

        if (buttonSound != null) {
            buttonSound.release();
            buttonSound = null;
        }

        super.onDestroy();
    }
}
