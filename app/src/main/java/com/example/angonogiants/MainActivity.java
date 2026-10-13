package com.example.angonogiants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mainmenubg;
    private MediaPlayer buttonSound;
    private EditText playerNameEditText;
    private Vibrator vibrator;
    private View midLayout, bottomLayout, AbtusLayout; // Layout containers

    private final String DB_URL = "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        ImageView btnLeaderboard = findViewById(R.id.btnLeaderboard);
        ImageView backgroundGif = findViewById(R.id.backgroundGif);
        ImageView btnAboutUs = findViewById(R.id.btnAboutUs);
        playerNameEditText = findViewById(R.id.playerNameEditText);
        Button btnStart = findViewById(R.id.btnStart);

        // Layout Containers to hide/show
        midLayout = findViewById(R.id.midLayout);
        bottomLayout = findViewById(R.id.bottomLayout);
        AbtusLayout = findViewById(R.id.AbtusLayout);

        // HIDE EVERYTHING AT START
        midLayout.setVisibility(View.INVISIBLE);
        bottomLayout.setVisibility(View.INVISIBLE);
        AbtusLayout.setVisibility(View.INVISIBLE);

        // 1. LOAD BACKGROUND GIF FIRST WITH LISTENER
        Glide.with(this)
                .asGif()
                .load(R.drawable.mainmenubg)
                .listener(new RequestListener<com.bumptech.glide.load.resource.gif.GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<com.bumptech.glide.load.resource.gif.GifDrawable> target, boolean isFirstResource) {
                        showButtons(); // Show anyway if load fails so user isn't stuck
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(com.bumptech.glide.load.resource.gif.GifDrawable resource, Object model, Target<com.bumptech.glide.load.resource.gif.GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        // BACKGROUND READY! Now show the buttons
                        runOnUiThread(() -> showButtons());
                        return false;
                    }
                })
                .into(backgroundGif);

        // Load secondary GIFs normally
        Glide.with(this).asGif().load(R.drawable.ldboard).into(btnLeaderboard);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Music
        mainmenubg = MediaPlayer.create(this, R.raw.mainmenubg);
        if (mainmenubg != null) {
            mainmenubg.setLooping(true);
            mainmenubg.start();
        }
        buttonSound = MediaPlayer.create(this, R.raw.buttonsound);

        // Click Listeners (remain the same)
        btnStart.setOnClickListener(v -> {
            playSound();
            String playerName = playerNameEditText.getText().toString().trim();
            if (playerName.isEmpty()) {
                showInputError("Please enter your name!");
                vibrate();
                return;
            }
            checkNameAndStart(playerName, btnStart);
        });

        btnLeaderboard.setOnClickListener(v -> {
            playSound();
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        if (btnAboutUs != null) {
            btnAboutUs.setOnClickListener(v -> {
                playSound();
                Intent intent = new Intent(MainActivity.this, AboutUsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }
    }

    // NEW METHOD TO REVEAL BUTTONS WITH A FADE
    private void showButtons() {
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500); // 0.5 seconds fade

        midLayout.setVisibility(View.VISIBLE);
        bottomLayout.setVisibility(View.VISIBLE);
        AbtusLayout.setVisibility(View.VISIBLE);

        midLayout.startAnimation(fadeIn);
        bottomLayout.startAnimation(fadeIn);
        AbtusLayout.startAnimation(fadeIn);
    }

    private void checkNameAndStart(String name, Button btnStart) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("leaderboard");
        ref.orderByChild("playerName").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    showInputError("This name is already taken!");
                    vibrate();
                } else {
                    proceedToGame(name, btnStart);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", error.getMessage());
                Toast.makeText(MainActivity.this, "Database Error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToGame(String playerName, Button btnStart) {
        AnimationDrawable startButtonAnimation = (AnimationDrawable) btnStart.getBackground();
        if (startButtonAnimation != null) {
            startButtonAnimation.start();
        }
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, LoadingActivity.class);
            intent.putExtra("PLAYER_NAME", playerName);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 100);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {}

    private void showInputError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void vibrate() {
        if (vibrator != null) {
            vibrator.vibrate(200);
        }
    }

    private void playSound() {
        if (buttonSound != null) {
            buttonSound.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainmenubg != null && !mainmenubg.isPlaying()) {
            mainmenubg.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mainmenubg != null) {
            mainmenubg.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainmenubg != null) {
            mainmenubg.release();
            mainmenubg = null;
        }
        if (buttonSound != null) {
            buttonSound.release();
            buttonSound = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}