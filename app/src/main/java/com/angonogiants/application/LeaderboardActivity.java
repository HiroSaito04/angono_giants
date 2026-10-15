//app\src\main\java\com\angonogiants\application\LeaderboardActivity.java
package com.angonogiants.application;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull; // Added this import
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private MediaPlayer ldboardmusic;
    private ImageButton mainMenuLButton;
    private MediaPlayer buttonSound;
    private ListView leaderboardListView;
    private List<LeaderboardEntry> leaderboardList;
    private LeaderboardAdapter leaderboardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboardListView = findViewById(R.id.leaderboardListView);
        mainMenuLButton = findViewById(R.id.mainMenuLButton);

        // Initialize list and adapter immediately to avoid null references
        leaderboardList = new ArrayList<>();
        leaderboardAdapter = new LeaderboardAdapter(this, leaderboardList);
        leaderboardListView.setAdapter(leaderboardAdapter);

        buttonSound = MediaPlayer.create(this, R.raw.buttonsound);
        if (buttonSound != null) {
            buttonSound.setOnCompletionListener(mp -> mp.seekTo(0));
        }

        mainMenuLButton.setOnClickListener(v -> {
            playSound();
            AnimationDrawable animationDrawable = (AnimationDrawable) mainMenuLButton.getBackground();
            if (animationDrawable != null) {
                animationDrawable.start();
            }
            startActivity(new Intent(LeaderboardActivity.this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        playBackgroundMusic();

        // Fetch data from Firebase
        attachFirebaseListener();
    }

    private void attachFirebaseListener() {
        // ADD YOUR URL HERE just like in GameActivity
        String databaseUrl = "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

        // Pass the URL into getInstance()
        FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);
        DatabaseReference leaderboardRef = database.getReference("leaderboard");

        leaderboardRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                leaderboardList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // Ensure your LeaderboardEntry class matches the Firebase keys exactly
                    LeaderboardEntry entry = data.getValue(LeaderboardEntry.class);
                    if (entry != null) {
                        leaderboardList.add(entry);
                    }
                }

                // Sort by score descending
                Collections.sort(leaderboardList, (a, b) -> Integer.compare(b.score, a.score));

                // Refresh the UI
                leaderboardAdapter.notifyDataSetChanged();
                Log.d("LeaderboardDebug", "Data fetched! Total entries: " + leaderboardList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LeaderboardActivity", "Firebase Error: " + error.getMessage());
            }
        });
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void playSound() {
        if (buttonSound != null) {
            buttonSound.start();
        }
    }

    private void playBackgroundMusic() {
        if (ldboardmusic == null) {
            ldboardmusic = MediaPlayer.create(this, R.raw.leaderboardmuzic);
            if (ldboardmusic != null) {
                ldboardmusic.setLooping(true);
                ldboardmusic.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ldboardmusic != null) {
            ldboardmusic.release();
            ldboardmusic = null;
        }
        if (buttonSound != null) {
            buttonSound.release();
            buttonSound = null;
        }
    }
}