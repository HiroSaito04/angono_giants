
package com.angonogiants.application;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String DATABASE_URL =
            "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private static final long START_DELAY = 600L;
    private static final long MENU_FALLBACK_DELAY = 1500L;

    private FirebaseAuth auth;
    private PlayerProgressRepository progressRepository;

    private String playerName;
    private int savedLevel = 1;

    private ImageView backgroundGif;
    private ImageView leaderboardButton;
    private ImageView aboutUsButton;

    private Button startButton;
    private Button signOutButton;

    private EditText playerNameEditText;
    private TextView levelStatusText;

    private View midLayout;
    private View bottomLayout;
    private View aboutUsLayout;

    private MediaPlayer menuMusic;
    private MediaPlayer buttonSound;
    private Vibrator vibrator;

    private boolean menuVisible = false;
    private boolean accountReady = false;
    private boolean loadingLevel = false;
    private boolean navigating = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Runnable pendingNavigation;

    private final Runnable fallbackTask =
            this::showMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        initializeViews();
        initializeServices();
        setupClickListeners();
        loadMenuAnimations();
        loadPlayerProfile();
    }

    private void initializeViews() {
        backgroundGif = findViewById(R.id.backgroundGif);
        leaderboardButton =
                findViewById(R.id.btnLeaderboard);
        aboutUsButton = findViewById(R.id.btnAboutUs);

        startButton = findViewById(R.id.btnStart);
        signOutButton = findViewById(R.id.btnSignOut);

        playerNameEditText =
                findViewById(R.id.playerNameEditText);

        levelStatusText =
                findViewById(R.id.levelStatusText);

        midLayout = findViewById(R.id.midLayout);
        bottomLayout = findViewById(R.id.bottomLayout);
        aboutUsLayout = findViewById(R.id.AbtusLayout);
    }

    private void initializeServices() {
        progressRepository =
                new PlayerProgressRepository();

        vibrator = (Vibrator)
                getSystemService(Context.VIBRATOR_SERVICE);

        menuMusic = MediaPlayer.create(
                this,
                R.raw.mainmenubg
        );

        if (menuMusic != null) {
            menuMusic.setLooping(true);
        }

        buttonSound = MediaPlayer.create(
                this,
                R.raw.buttonsound
        );
    }

    // -----------------------------------------
    // Animated menu
    // -----------------------------------------

    private void loadMenuAnimations() {
        Glide.with(this)
                .asGif()
                .load(R.drawable.mainmenubg01)
                .placeholder(R.drawable.mainmenubg00)
                .error(R.drawable.mainmenubg00)
                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<GifDrawable> target,
                            boolean isFirstResource
                    ) {
                        backgroundGif.post(
                                MainActivity.this::showMenu
                        );
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            GifDrawable resource,
                            Object model,
                            Target<GifDrawable> target,
                            DataSource dataSource,
                            boolean isFirstResource
                    ) {
                        backgroundGif.post(
                                MainActivity.this::showMenu
                        );
                        return false;
                    }
                })
                .into(backgroundGif);

        Glide.with(this)
                .asGif()
                .load(R.drawable.leaderboard_button)
                .into(leaderboardButton);
    }

    private void showMenu() {
        if (menuVisible || isFinishing() || isDestroyed()) {
            return;
        }

        menuVisible = true;

        fadeIn(midLayout);
        fadeIn(bottomLayout);
        fadeIn(aboutUsLayout);
        fadeIn(levelStatusText);
        fadeIn(signOutButton);
    }

    private void fadeIn(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        view.animate()
                .alpha(1f)
                .setDuration(500L)
                .start();
    }

    // -----------------------------------------
    // Account loading
    // -----------------------------------------

    private void loadPlayerProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            redirectToLogin();
            return;
        }

        FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("playerProfiles")
                .child(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    PlayerProfile profile =
                            snapshot.getValue(PlayerProfile.class);

                    if (profile == null ||
                            profile.playerName == null ||
                            profile.playerName.trim().isEmpty()) {

                        showMessage(
                                "Complete your account setup first."
                        );

                        redirectToLogin();
                        return;
                    }

                    playerName = profile.playerName;
                    playerNameEditText.setText(playerName);

                    loadSavedLevel();
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Profile load failed", error);

                    levelStatusText.setText(
                            "Unable to load account"
                    );

                    showMessage(
                            "Check your internet connection."
                    );
                });
    }

    private void loadSavedLevel() {
        accountReady = false;
        startButton.setEnabled(false);

        progressRepository.loadProgress()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    PlayerProgress progress =
                            snapshot.getValue(PlayerProgress.class);

                    savedLevel = progress == null
                            ? 1
                            : Math.max(
                                    1,
                                    progress.highestUnlockedLevel
                            );

                    levelStatusText.setText(
                            "LEVEL " + savedLevel
                    );

                    accountReady = true;
                    startButton.setEnabled(true);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Level load failed", error);

                    accountReady = false;
                    levelStatusText.setText(
                            "Progress unavailable"
                    );

                    showMessage(
                            "Could not load saved progress."
                    );
                });
    }

    // -----------------------------------------
    // Buttons
    // -----------------------------------------

    private void setupClickListeners() {
        startButton.setOnClickListener(v ->
                handleStartClick()
        );

        leaderboardButton.setOnClickListener(v -> {
            if (navigating) return;

            navigating = true;
            playButtonSound();

            openActivity(
                    LeaderboardActivity.class,
                    true
            );
        });

        aboutUsButton.setOnClickListener(v -> {
            if (navigating) return;

            playButtonSound();

            openActivity(
                    AboutUsActivity.class,
                    false
            );
        });

        signOutButton.setOnClickListener(v ->
                signOut()
        );
    }

    private void handleStartClick() {
        if (!accountReady ||
                loadingLevel ||
                navigating ||
                playerName == null) {
            return;
        }

        playButtonSound();

        loadingLevel = true;
        startButton.setEnabled(false);

        // Refresh level before every new run.
        progressRepository.loadProgress()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed() ||
                            !loadingLevel) {
                        return;
                    }

                    PlayerProgress progress =
                            snapshot.getValue(PlayerProgress.class);

                    savedLevel = progress == null
                            ? 1
                            : Math.max(
                                    1,
                                    progress.highestUnlockedLevel
                            );

                    loadingLevel = false;
                    proceedToLoading();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    loadingLevel = false;
                    startButton.setEnabled(true);

                    Log.e(TAG, "Cannot start game", error);

                    showMessage(
                            "Could not load your level."
                    );

                    vibrate();
                });
    }

    private void proceedToLoading() {
        if (navigating) return;

        navigating = true;

        Drawable drawable = startButton.getBackground();

        if (drawable instanceof AnimationDrawable) {
            AnimationDrawable animation =
                    (AnimationDrawable) drawable;

            animation.stop();
            animation.start();
        }

        pendingNavigation = () -> {
            pendingNavigation = null;

            if (isFinishing() || isDestroyed()) {
                return;
            }

            Intent intent = new Intent(
                    this,
                    LoadingActivity.class
            );

            intent.putExtra("PLAYER_NAME", playerName);
            intent.putExtra("LEVEL_NUMBER", savedLevel);

            startActivity(intent);

            applyTransition();
            finish();
        };

        handler.postDelayed(
                pendingNavigation,
                START_DELAY
        );
    }

    private void openActivity(
            Class<?> destination,
            boolean finishCurrent
    ) {
        Intent intent = new Intent(this, destination);
        startActivity(intent);

        applyTransition();

        if (finishCurrent) {
            finish();
        }
    }

    private void signOut() {
        if (navigating) return;

        navigating = true;

        auth.signOut();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(
                this,
                AuthActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void applyTransition() {
        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    // -----------------------------------------
    // Audio and vibration
    // -----------------------------------------

    private void playButtonSound() {
        if (buttonSound == null) return;

        try {
            buttonSound.seekTo(0);
            buttonSound.start();
        } catch (IllegalStateException error) {
            Log.e(TAG, "Button sound failed", error);
        }
    }

    private void vibrate() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            150L,
                            VibrationEffect.DEFAULT_AMPLITUDE
                    )
            );
        } else {
            vibrator.vibrate(150L);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // -----------------------------------------
    // Lifecycle
    // -----------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

        if (menuMusic != null && !menuMusic.isPlaying()) {
            menuMusic.start();
        }

        if (!menuVisible) {
            handler.postDelayed(
                    fallbackTask,
                    MENU_FALLBACK_DELAY
            );
        }
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(fallbackTask);

        if (pendingNavigation != null) {
            handler.removeCallbacks(pendingNavigation);
            pendingNavigation = null;

            navigating = false;
            startButton.setEnabled(accountReady);
        }

        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (menuMusic != null) {
            menuMusic.release();
            menuMusic = null;
        }

        if (buttonSound != null) {
            buttonSound.release();
            buttonSound = null;
        }

        if (vibrator != null) {
            vibrator.cancel();
        }

        super.onDestroy();
    }
}
