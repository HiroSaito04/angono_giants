//app\src\main\java\com\angonogiants\application\GameActivity.java
package com.angonogiants.application;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private static final long MUSIC_DELAY = 5000L;
    private static final long TRANSITION_DELAY = 5000L;

    // -----------------------------------------
    // GAME MODULES
    // -----------------------------------------

    private GameStateManager gameState;
    private GameTimer gameTimer;
    private GameAudioManager audioManager;

    private PlayerProgressRepository progressRepository;
    private LeaderboardRepository leaderboardRepository;

    private GameView gameView;

    // -----------------------------------------
    // PLAYER INFORMATION
    // -----------------------------------------

    private String playerName = "Guest";
    private boolean isGuestRun = false;

    private int levelNumber = 1;
    private int currentWave = 1;

    // Cumulative statistics across completed levels.
    private long completedSeconds = 0L;
    private long completedWaves = 0L;

    // Frozen final-result values.
    private int resultLevel = 1;
    private int resultScore = 0;
    private int resultWaves = 0;
    private long resultSeconds = 0L;

    // -----------------------------------------
    // ACTIVITY STATE
    // -----------------------------------------

    private boolean foreground = false;
    private boolean navigating = false;
    private boolean resultsVisible = false;
    private boolean finalResultSaved = false;

    // -----------------------------------------
    // INTRO STATE
    // -----------------------------------------

    private boolean introRequested = false;
    private boolean introFinished = false;
    private boolean introHalfReached = false;
    private boolean introSoundPlayed = false;

    private int backgroundsRemaining = 2;

    private GifDrawable introGifDrawable;

    private Animatable2Compat.AnimationCallback
            introAnimationCallback;

    private View introCurtain;
    private ImageView gifOverlay;

    // -----------------------------------------
    // HUD
    // -----------------------------------------

    private TextView timerText;
    private TextView scoreText;
    private TextView waveText;

    // -----------------------------------------
    // BUTTONS
    // -----------------------------------------

    private ImageButton pauseButton;
    private ImageButton restartButton;
    private ImageButton menuButton;
    private ImageButton exitButton;

    private ImageButton finalMenuButton;
    private Button defendAgainButton;

    // -----------------------------------------
    // RESULT SCREENS
    // -----------------------------------------

    private ImageView gameThanksImage;
    private TextView gameThanksText;

    private ImageView gameWonImage;
    private ImageView gameOverImage;
    private ImageView lostFinalScoreImage;

    private View finalScoreContainer;
    private View finalActionRow;

    private TextView resultPlayerName;
    private TextView finalLevelText;
    private TextView resultSubtitle;
    private TextView finalScoreText;
    private TextView finalTimeText;
    private TextView finalWavesText;

    // -----------------------------------------
    // HANDLER
    // -----------------------------------------

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private long musicReadyAt;

    private long transitionRemaining =
            TRANSITION_DELAY;

    private long transitionDueAt = 0L;

    private final Runnable musicTask =
            this::refreshGameplayMusic;

    private final Runnable resultTask =
            this::showTransitionResult;

    // -----------------------------------------
    // HALF-GIF FRAME CHECK
    // -----------------------------------------

    private final Runnable halfwayTask =
            new Runnable() {

                @Override
                public void run() {

                    if (introFinished
                            || introHalfReached
                            || isFinishing()
                            || isDestroyed()) {
                        return;
                    }

                    if (!foreground) {
                        return;
                    }

                    if (introGifDrawable == null) {
                        handler.postDelayed(
                                this,
                                16L
                        );
                        return;
                    }

                    int frameCount =
                            introGifDrawable
                                    .getFrameCount();

                    int frameIndex =
                            introGifDrawable
                                    .getFrameIndex();

                    int halfFrame =
                            Math.max(
                                    1,
                                    frameCount / 2
                            );

                    if (frameIndex >= halfFrame) {
                        revealGameUnderGif();
                        return;
                    }

                    handler.postDelayed(
                            this,
                            16L
                    );
                }
            };

    // -----------------------------------------
    // ON CREATE
    // -----------------------------------------

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_game
        );

        readPlayerData();
        initializeViews();

        // Protect the screen before gameplay setup.
        introCurtain.setVisibility(
                View.VISIBLE
        );

        gifOverlay.setVisibility(
                View.VISIBLE
        );

        introCurtain.bringToFront();
        gifOverlay.bringToFront();

        // GameView is already constructed during
        // XML inflation, so GameRenderer has loaded
        // the stall, hearts and giant bitmaps.
        initializeModules();

        setupGameView();

        // Keep all gameplay mechanics inactive
        // while the introduction runs.
        gameView.pauseGame();

        gameView.setGameTouchable(
                false
        );

        initializeButtons();
        setupBackNavigation();

        updateTimerText(
                LevelConfig.getDurationSeconds(
                        levelNumber
                )
        );

        updateScoreCounter(
                gameView.getscoreCount()
        );

        updateWaveCounter(1);

        musicReadyAt =
                SystemClock.uptimeMillis()
                        + MUSIC_DELAY;

        // Start intro only after background GIFs
        // have finished loading.
        loadBackgroundsThenIntro();
    }

    // -----------------------------------------
    // PLAYER DATA
    // -----------------------------------------

    private void readPlayerData() {
        Intent intent = getIntent();

        String name =
                intent.getStringExtra(
                        "PLAYER_NAME"
                );

        playerName =
                name == null || name.trim().isEmpty()
                        ? "Guest"
                        : name.trim();

        levelNumber = Math.max(
                1,
                intent.getIntExtra(
                        "LEVEL_NUMBER",
                        1
                )
        );

        isGuestRun = intent.getBooleanExtra(
                "IS_GUEST",
                FirebaseAuth.getInstance()
                        .getCurrentUser() == null
        );
    }

    // -----------------------------------------
    // FIND VIEWS
    // -----------------------------------------

    private void initializeViews() {

        gameView =
                findViewById(R.id.gameView);

        timerText =
                findViewById(R.id.timerText);

        scoreText =
                findViewById(R.id.ScoreCounter);

        waveText =
                findViewById(R.id.waveCounter);

        pauseButton =
                findViewById(R.id.btnPauseResume);

        restartButton =
                findViewById(R.id.restartButton);

        menuButton =
                findViewById(R.id.menuButton);

        exitButton =
                findViewById(R.id.exitButton);

        finalMenuButton =
                findViewById(R.id.mainMenuButton);

        defendAgainButton =
                findViewById(R.id.defendAgainButton);

        introCurtain =
                findViewById(R.id.introCurtain);

        gifOverlay =
                findViewById(R.id.gifOverlay);

        gameThanksImage =
                findViewById(R.id.gameThanksImage);

        gameThanksText =
                findViewById(R.id.gameThanksText);

        gameWonImage =
                findViewById(R.id.gameWonImage);

        gameOverImage =
                findViewById(R.id.gameOverImage);

        lostFinalScoreImage =
                findViewById(R.id.lostfinalscoreiv);

        finalScoreContainer =
                findViewById(R.id.finalScoreContainer);

        finalActionRow =
                findViewById(R.id.finalActionRow);

        resultPlayerName =
                findViewById(R.id.playerNameText);

        finalLevelText =
                findViewById(R.id.finalLevelTV);

        resultSubtitle =
                findViewById(R.id.resultSubtitle);

        finalScoreText =
                findViewById(R.id.finalScoreTV);

        finalTimeText =
                findViewById(R.id.finalScoreTimer);

        finalWavesText =
                findViewById(R.id.finalwaveTV);

        showPauseButtons(false);
        hideResultScreens();

        resultPlayerName.setText(
                playerName
        );
    }

    // -----------------------------------------
    // GAME MODULES
    // -----------------------------------------

    private void initializeModules() {

        gameState =
                new GameStateManager();

        audioManager =
                new GameAudioManager(this);

        progressRepository =
                new PlayerProgressRepository();

        leaderboardRepository =
                new LeaderboardRepository();

        gameTimer =
                createLevelTimer(levelNumber);

        Log.d(
                TAG,
                "Level " + levelNumber
                        + " prepared. Duration: "
                        + LevelConfig.getDurationSeconds(
                                levelNumber
                        )
        );
    }

    private GameTimer createLevelTimer(
            int level
    ) {
        return new GameTimer(
                LevelConfig.getDurationSeconds(
                        level
                ),
                new GameTimer.Listener() {

                    @Override
                    public void onTick(
                            int remainingSeconds
                    ) {
                        updateTimerText(
                                remainingSeconds
                        );
                    }

                    @Override
                    public void onFinished() {
                        handleLevelCompleted();
                    }
                }
        );
    }

    private void setupGameView() {

        gameView.setControllers(
                gameState,
                audioManager
        );

        gameView.setOnLifeGivenListener(
                this::refreshGameplayMusic
        );

        gameView.startLevel(
                levelNumber
        );

        gameView.pauseGame();

        gameView.setGameTouchable(
                false
        );

        // A paused GameView still draws its
        // stall, hearts and sprites on invalidate().
        gameView.invalidate();
    }

    // -----------------------------------------
    // PRELOAD BOTH BACKGROUND GIFS
    // -----------------------------------------

    private void loadBackgroundsThenIntro() {

        backgroundsRemaining = 2;

        ImageView tiles =
                findViewById(
                        R.id.tilelayerView
                );

        ImageView trees =
                findViewById(
                        R.id.treelayerView
                );

        Glide.with(this)
                .asGif()
                .load(R.drawable.tile_layer)
                .listener(backgroundLoadListener(
                        "Tile layer"
                ))
                .into(tiles);

        Glide.with(this)
                .asGif()
                .load(R.drawable.tree_layer)
                .listener(backgroundLoadListener(
                        "Tree layer"
                ))
                .into(trees);
    }

    private RequestListener<GifDrawable>
    backgroundLoadListener(String label) {

        return new RequestListener<GifDrawable>() {

            @Override
            public boolean onLoadFailed(
                    @Nullable GlideException error,
                    Object model,
                    Target<GifDrawable> target,
                    boolean isFirstResource
            ) {
                Log.e(
                        TAG,
                        label + " loading failed",
                        error
                );

                // Continue rather than hanging
                // forever if an asset is missing.
                handler.post(
                        () -> onBackgroundReady(label)
                );

                return false;
            }

            @Override
            public boolean onResourceReady(
                    @NonNull GifDrawable drawable,
                    @NonNull Object model,
                    Target<GifDrawable> target,
                    @NonNull DataSource dataSource,
                    boolean isFirstResource
            ) {
                Log.d(
                        TAG,
                        label + " loaded"
                );

                // Post so Glide attaches the drawable.
                handler.post(
                        () -> onBackgroundReady(label)
                );

                return false;
            }
        };
    }

    private void onBackgroundReady(
            String label
    ) {

        if (introRequested
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        backgroundsRemaining--;

        Log.d(
                TAG,
                label + " ready. Remaining: "
                        + backgroundsRemaining
        );

        if (backgroundsRemaining <= 0) {
            waitForGameLayout();
        }
    }

    // -----------------------------------------
    // ENSURE GAMEVIEW HAS BEEN LAID OUT
    // -----------------------------------------

    private void waitForGameLayout() {

        if (introRequested
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        if (gameView.getWidth() > 0
                && gameView.getHeight() > 0) {

            // GameRenderer.resize() was called
            // by GameView.onSizeChanged().
            gameView.invalidate();

            // Let Android process the prepared scene.
            gameView.post(
                    this::startIntroAnimation
            );

            return;
        }

        gameView.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {

                    @Override
                    public void onLayoutChange(
                            View view,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom
                    ) {
                        if (right <= left
                                || bottom <= top) {
                            return;
                        }

                        gameView.removeOnLayoutChangeListener(
                                this
                        );

                        if (introRequested
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        gameView.invalidate();

                        gameView.post(
                                GameActivity.this
                                        ::startIntroAnimation
                        );
                    }
                }
        );
    }

    // -----------------------------------------
    // PLAY INTRO GIF
    // -----------------------------------------

    private void startIntroAnimation() {

        if (introRequested
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        introRequested = true;
        introFinished = false;
        introHalfReached = false;

        introCurtain.setVisibility(
                View.VISIBLE
        );

        gifOverlay.setVisibility(
                View.VISIBLE
        );

        // Correct z-order:
        // game < curtain < transparent GIF.
        introCurtain.bringToFront();
        gifOverlay.bringToFront();

        gifOverlay.setClickable(true);

        gameView.pauseGame();

        gameView.setGameTouchable(
                false
        );

        Log.d(
                TAG,
                "Starting gametransition3 GIF"
        );

        Glide.with(this)
                .asGif()
                .load(
                        R.drawable.gametransition3
                )
                .listener(
                        new RequestListener<GifDrawable>() {

                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException error,
                                    Object model,
                                    Target<GifDrawable> target,
                                    boolean isFirstResource
                            ) {
                                Log.e(
                                        TAG,
                                        "Intro GIF failed",
                                        error
                                );

                                gifOverlay.post(
                                        GameActivity.this
                                                ::finishIntroAnimation
                                );

                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    @NonNull GifDrawable drawable,
                                    @NonNull Object model,
                                    Target<GifDrawable> target,
                                    @NonNull DataSource dataSource,
                                    boolean isFirstResource
                            ) {

                                introGifDrawable =
                                        drawable;

                                // Exactly one animation loop.
                                drawable.setLoopCount(1);

                                introAnimationCallback =
                                        new Animatable2Compat
                                                .AnimationCallback() {

                                            @Override
                                            public void onAnimationEnd(
                                                    Drawable finished
                                            ) {
                                                runOnUiThread(
                                                        GameActivity.this
                                                                ::finishIntroAnimation
                                                );
                                            }
                                        };

                                drawable.registerAnimationCallback(
                                        introAnimationCallback
                                );

                                gifOverlay.post(() -> {

                                    if (introFinished
                                            || isFinishing()
                                            || isDestroyed()) {
                                        return;
                                    }

                                    Log.d(
                                            TAG,
                                            "Intro loaded: "
                                                    + drawable
                                                            .getFrameCount()
                                                    + " frames"
                                    );

                                    playIntroSoundIfNeeded();

                                    handler.removeCallbacks(
                                            halfwayTask
                                    );

                                    if (foreground) {
                                        handler.post(
                                                halfwayTask
                                        );
                                    }
                                });

                                return false;
                            }
                        }
                )
                .into(gifOverlay);
    }

    // -----------------------------------------
    // INTRO SOUND
    // -----------------------------------------

    private void playIntroSoundIfNeeded() {

        if (introSoundPlayed
                || !foreground
                || introFinished
                || introGifDrawable == null) {
            return;
        }

        introSoundPlayed = true;

        audioManager.setActive(
                true
        );

        audioManager.playIntro();
    }

    // -----------------------------------------
    // MIDPOINT: REVEAL THE GAME
    // -----------------------------------------

    private void revealGameUnderGif() {

        if (introHalfReached
                || introFinished) {
            return;
        }

        introHalfReached = true;

        // Force a static frame of the prepared
        // renderer to be drawn.
        gameView.invalidate();

        // Remove the opaque cover ONLY.
        introCurtain.setVisibility(
                View.GONE
        );

        // Keep gametransition3 on top.
        gifOverlay.setVisibility(
                View.VISIBLE
        );

        gifOverlay.bringToFront();

        // Enemies, touches and timer still paused.
        gameView.pauseGame();

        gameView.setGameTouchable(
                false
        );

        Log.d(
                TAG,
                "GIF halfway: game visible underneath"
        );
    }

    // -----------------------------------------
    // GIF FINISHED: ACTIVATE GAME
    // -----------------------------------------

    private void finishIntroAnimation() {

        if (introFinished
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        introFinished = true;

        handler.removeCallbacks(
                halfwayTask
        );

        introHalfReached = true;

        introCurtain.setVisibility(
                View.GONE
        );

        if (introGifDrawable != null
                && introAnimationCallback != null) {

            introGifDrawable
                    .unregisterAnimationCallback(
                            introAnimationCallback
                    );
        }

        introAnimationCallback = null;
        introGifDrawable = null;

        Glide.with(this).clear(
                gifOverlay
        );

        gifOverlay.setVisibility(
                View.GONE
        );

        gifOverlay.setClickable(
                false
        );

        gameView.invalidate();

        Log.d(
                TAG,
                "Intro finished; activating gameplay"
        );

        if (foreground
                && gameState.isPlaying()) {

            beginGameplayAfterIntro();
        }
    }

    // -----------------------------------------
    // ACTUAL GAMEPLAY START
    // -----------------------------------------

    private void beginGameplayAfterIntro() {

        if (!introFinished
                || !foreground
                || !gameState.isPlaying()) {
            return;
        }

        gameView.resumeGame();

        gameView.setGameTouchable(
                true
        );

        audioManager.setActive(
                true
        );

        // IMPORTANT:
        // Do NOT start gameTimer here.
        //
        // GameView reports onFirstEnemySpawned()
        // through its EnemyManager listener.
        //
        // That callback invokes
        // startTimerFromGameView().
        //
        // Therefore the countdown starts only
        // when the first giant actually spawns.

        musicReadyAt =
                SystemClock.uptimeMillis()
                        + MUSIC_DELAY;

        refreshGameplayMusic();
    }

    // -----------------------------------------
    // TIMER AND HUD
    // -----------------------------------------

    private void updateTimerText(
            int seconds
    ) {
        timerText.setText(
                String.format(
                        Locale.US,
                        "%02d:%02d",
                        seconds / 60,
                        seconds % 60
                )
        );
    }

    public void updateScoreCounter(
            int score
    ) {
        runOnUiThread(() ->
                scoreText.setText(
                        String.format(
                                Locale.US,
                                "Score: %,d",
                                score
                        )
                )
        );
    }

    public void updateWaveCounter(
            int wave
    ) {
        runOnUiThread(() -> {

            currentWave =
                    Math.max(1, wave);

            waveText.setText(
                    String.format(
                            Locale.US,
                            "Level %d - Wave %d",
                            levelNumber,
                            currentWave
                    )
            );
        });
    }

    public void startTimerFromGameView() {

        runOnUiThread(() -> {

            if (!introFinished
                    || !foreground
                    || !gameState.isPlaying()) {
                return;
            }

            if (!gameTimer.isStarted()) {
                gameTimer.start();

                Log.d(
                        TAG,
                        "First enemy spawned: timer started"
                );
            }
        });
    }

    public void startTimer() {
        startTimerFromGameView();
    }

    public void stopTimer() {

        runOnUiThread(() -> {
            gameTimer.pause();
            gameTimer.stop();
        });
    }

    // -----------------------------------------
    // BUTTONS
    // -----------------------------------------


    private void initializeButtons() {

        // PAUSE / RESUME
        pauseButton.setOnClickListener(v -> {

                if (!introFinished) {
                return;
                }

                if (gameState.isPlaying()) {

                audioManager.playButtonSound();
                pauseGame();

                } else if (gameState.isPaused()) {

                audioManager.playButtonSound();
                resumeGame();
                }
        });

        // RESTART FROM PAUSE MENU
        restartButton.setOnClickListener(v -> {

                if (navigating) return;

                audioManager.playButtonSound();
                playButtonAnimation(restartButton);

                restartGame();
        });

        // PAUSE MENU -> MAIN MENU
        menuButton.setOnClickListener(v -> {

                if (navigating) return;

                audioManager.playButtonSound();
                playButtonAnimation(menuButton);

                openMainMenu();
        });

        // EXIT GAME
        exitButton.setOnClickListener(v -> {

                if (navigating) return;

                navigating = true;
                audioManager.playButtonSound();

                finishAffinity();
        });

        // FINAL SCORE -> MAIN MENU
        // Works after victory AND defeat.
        finalMenuButton.setOnClickListener(v -> {

                if (navigating) return;

                audioManager.playButtonSound();

                Log.d(
                        TAG,
                        "Final score Main Menu clicked"
                );

                openMainMenu();
        });

        // FINAL SCORE -> DEFEND AGAIN
        defendAgainButton.setOnClickListener(v -> {

                if (navigating || !resultsVisible) {
                return;
                }

                audioManager.playButtonSound();

                if (gameState.isLevelComplete()) {

                // VICTORY:
                // Move to the next unlocked level.
                Log.d(
                        TAG,
                        "Victory: starting next level"
                );

                startNextLevel();

                } else if (
                        gameState.getState()
                                == GameStateManager.State.RESULTS
                        && !gameState.didWin()
                ) {

                // DEFEAT:
                // Restart the same level.
                // LoadingActivity creates a fresh game.
                Log.d(
                        TAG,
                        "Defeat: retrying level "
                                + levelNumber
                );

                restartGame();
                }
        });
    }

    private void playButtonAnimation(
            ImageButton button
    ) {
        Drawable background =
                button.getBackground();

        if (background
                instanceof AnimationDrawable) {

            AnimationDrawable animation =
                    (AnimationDrawable) background;

            animation.stop();
            animation.start();
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {
                                // Disabled during game.
                            }
                        }
                );
    }

    // -----------------------------------------
    // PAUSE / RESUME
    // -----------------------------------------

    private void pauseGame() {

        if (!gameState.pause()) {
            return;
        }

        gameView.pauseGame();

        gameView.setGameTouchable(
                false
        );

        gameTimer.pause();

        handler.removeCallbacks(
                musicTask
        );

        audioManager.setActive(
                false
        );

        pauseButton.setImageResource(
                R.drawable.resume_button_animation
        );

        showPauseButtons(true);
    }

    private void resumeGame() {

        if (!introFinished
                || !gameState.resume()) {
            return;
        }

        gameView.resumeGame();

        gameView.setGameTouchable(
                true
        );

        // If first giant hasn't spawned yet,
        // resume() won't start the timer.
        gameTimer.resume();

        pauseButton.setImageResource(
                R.drawable.pause_button_animation
        );

        showPauseButtons(false);

        audioManager.setActive(
                foreground
        );

        refreshGameplayMusic();
    }

    private void showPauseButtons(
            boolean visible
    ) {

        int visibility =
                visible
                        ? View.VISIBLE
                        : View.GONE;

        restartButton.setVisibility(
                visibility
        );

        menuButton.setVisibility(
                visibility
        );

        exitButton.setVisibility(
                visibility
        );
    }

    // -----------------------------------------
    // MUSIC
    // -----------------------------------------

    private void refreshGameplayMusic() {

        handler.removeCallbacks(
                musicTask
        );

        if (!introFinished
                || !foreground
                || !gameState.isPlaying()
                || gameView == null) {
            return;
        }

        int lives =
                gameView.getTargetLives();

        if (lives <= 0) {
            return;
        }

        if (lives == 1) {

            audioManager.setMusic(
                    GameAudioManager.MusicMode
                            .LOW_HEALTH
            );

            return;
        }

        long remaining =
                musicReadyAt
                        - SystemClock.uptimeMillis();

        if (remaining > 0L) {

            audioManager.setMusic(
                    GameAudioManager.MusicMode.NONE
            );

            handler.postDelayed(
                    musicTask,
                    remaining
            );

            return;
        }

        audioManager.setMusic(
                GameAudioManager.MusicMode.NORMAL
        );
    }

    public void startLowHealthMusic() {
        runOnUiThread(
                this::refreshGameplayMusic
        );
    }

    public void stopLowHealthMusic() {
        runOnUiThread(
                this::refreshGameplayMusic
        );
    }

    // -----------------------------------------
    // CAPTURE RESULT
    // -----------------------------------------

    private void captureResult(
            boolean completed
    ) {

        resultLevel =
                levelNumber;

        resultScore =
                gameView.getscoreCount();

        if (completed) {

            resultSeconds =
                    completedSeconds;

            resultWaves =
                    safeWaveCount(
                            completedWaves
                    );

        } else {

            resultSeconds =
                    getTotalTimePlayed();

            resultWaves =
                    getTotalWaves();
        }

        Log.d(
                TAG,
                "Result: level=" + resultLevel
                        + ", score=" + resultScore
                        + ", waves=" + resultWaves
                        + ", time=" + resultSeconds
        );
    }

    // -----------------------------------------
    // LEVEL COMPLETED
    // -----------------------------------------

    private void handleLevelCompleted() {

        if (!gameState.completeLevel()) {
            return;
        }

        gameTimer.stop();

        completedSeconds +=
                gameTimer.getElapsedSeconds();

        completedWaves +=
                gameView.getActualWaveCount();

        gameView.pauseGame();

        gameView.setGameTouchable(
                false
        );

        gameView.clearAllEnemies();

        handler.removeCallbacks(
                musicTask
        );

        audioManager.cancelGrowl();

        captureResult(true);

        hideGameplayControls();

        showLevelCompleteScreen();

        saveCompletedLevel();

        transitionRemaining =
                TRANSITION_DELAY;

        resultsVisible = false;

        scheduleTransition();
    }

    private void showLevelCompleteScreen() {

        hideResultScreens();

        Glide.with(this)
                .asGif()
                .load(
                        R.drawable.gamewonthanks_background
                )
                .into(
                        gameThanksImage
                );

        gameThanksImage.setVisibility(
                View.VISIBLE
        );

        int next =
                LevelConfig.getNextLevel(
                        levelNumber
                );

        gameThanksText.setText(
                "THANKS FOR SAVING OUR VILLAGE!\n\n"
                        + "LEVEL "
                        + levelNumber
                        + " IS COMPLETE!\n"
                        + "LEVEL "
                        + next
                        + " UNLOCKED!\n\n"
                        + "GIANTS WON'T ATTACK\n "
                        + "FOR THE MEANTIME."
        );

        gameThanksText.setVisibility(
                View.VISIBLE
        );

        audioManager.setMusic(
                GameAudioManager.MusicMode.WON_INTRO
        );
    }

    // -----------------------------------------
    // SAVE SUCCESSFUL LEVEL
    // -----------------------------------------

    private void saveCompletedLevel() {

        int unlockedLevel =
                LevelConfig.getNextLevel(
                        resultLevel
                );

        if (isGuestRun) {

            GuestProgressStore.recordResult(
                    this,
                    unlockedLevel,
                    resultScore,
                    resultWaves,
                    resultSeconds
            );

            return;
        }

        progressRepository.completeLevel(
                resultLevel,
                resultScore
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Cloud level save failed",
                        error
                )
        );

        leaderboardRepository.saveScore(
                playerName,
                unlockedLevel,
                resultScore,
                resultWaves,
                resultSeconds + " seconds"
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Leaderboard save failed",
                        error
                )
        );
    }

    // -----------------------------------------
    // RESULT TRANSITION
    // -----------------------------------------

    private void scheduleTransition() {

        if (!foreground
                || resultsVisible) {
            return;
        }

        boolean levelComplete =
                gameState.isLevelComplete();

        boolean lostIntro =
                gameState.getState()
                        == GameStateManager.State.LOST_INTRO;

        if (!levelComplete && !lostIntro) {
            return;
        }

        handler.removeCallbacks(
                resultTask
        );

        transitionDueAt =
                SystemClock.uptimeMillis()
                        + transitionRemaining;

        handler.postDelayed(
                resultTask,
                transitionRemaining
        );
    }

    private void pauseTransition() {

        if (transitionDueAt == 0L) {
            return;
        }

        transitionRemaining =
                Math.max(
                        0L,
                        transitionDueAt
                                - SystemClock.uptimeMillis()
                );

        transitionDueAt = 0L;

        handler.removeCallbacks(
                resultTask
        );
    }

    private void showTransitionResult() {

        transitionDueAt = 0L;

        if (resultsVisible) {
            return;
        }

        if (gameState.isLevelComplete()) {

            showLevelCompleteResults();

        } else if (gameState.getState()
                == GameStateManager.State.LOST_INTRO) {

            showFinalScore();
        }
    }

    // -----------------------------------------
    // SUCCESS FINAL SCORE
    // -----------------------------------------

    private void showLevelCompleteResults() {

        if (!gameState.isLevelComplete()) {
                return;
        }

        resultsVisible = true;
        transitionDueAt = 0L;

        introCurtain.setVisibility(View.GONE);
        gifOverlay.setVisibility(View.GONE);

        gameThanksImage.setVisibility(View.GONE);
        gameThanksText.setVisibility(View.GONE);

        gameOverImage.setVisibility(View.GONE);
        lostFinalScoreImage.setVisibility(View.GONE);

        Glide.with(this)
                .asGif()
                .load(R.drawable.wonfinalscore)
                .into(gameWonImage);

        gameWonImage.setVisibility(View.VISIBLE);

        updateFinalScore(true);

        finalScoreContainer.setVisibility(View.VISIBLE);

        defendAgainButton.setVisibility(View.VISIBLE);
        defendAgainButton.setEnabled(true);

        finalMenuButton.setVisibility(View.VISIBLE);
        finalMenuButton.setEnabled(true);

        finalActionRow.setVisibility(View.VISIBLE);
        finalActionRow.setEnabled(true);

        finalScoreContainer.bringToFront();
        finalActionRow.bringToFront();

        audioManager.setMusic(
                GameAudioManager.MusicMode.NONE
        );

        Log.d(
                TAG,
                "Victory results displayed: "
                        + "Next level and Main Menu available"
        );
        }

    // -----------------------------------------
    // DEFEND AGAIN
    // -----------------------------------------

    private void startNextLevel() {

        if (!resultsVisible
                || !gameState.isLevelComplete()) {
            return;
        }

        if (!gameState.startNextLevel()) {
            return;
        }

        handler.removeCallbacks(
                resultTask
        );

        transitionDueAt = 0L;

        transitionRemaining =
                TRANSITION_DELAY;

        resultsVisible = false;

        levelNumber =
                LevelConfig.getNextLevel(
                        levelNumber
                );

        currentWave = 1;

        gameTimer.release();

        gameTimer =
                createLevelTimer(
                        levelNumber
                );

        hideResultScreens();
        removeGrayscale();

        // GameView resets lives and enemies but
        // preserves the accumulated run score.
        gameView.startLevel(
                levelNumber
        );

        restoreGameplayControls();

        updateTimerText(
                LevelConfig.getDurationSeconds(
                        levelNumber
                )
        );

        updateScoreCounter(
                gameView.getscoreCount()
        );

        updateWaveCounter(1);

        pauseButton.setImageResource(
                R.drawable.pause_button_animation
        );

        gameView.resumeGame();

        gameView.setGameTouchable(
                true
        );

        audioManager.setMusic(
                GameAudioManager.MusicMode.NONE
        );

        musicReadyAt =
                SystemClock.uptimeMillis()
                        + MUSIC_DELAY;

        // Timer will start when the first
        // enemy of this new level spawns.
        refreshGameplayMusic();
    }

    // -----------------------------------------
    // DEFEAT
    // -----------------------------------------

    public void onGameOver() {
        runOnUiThread(
                this::handleGameLost
        );
    }

    private void handleGameLost() {

        if (!gameState.lose()) {
            return;
        }

        gameTimer.pause();
        gameTimer.stop();

        gameView.setGameOver(
                true
        );

        gameView.setGameTouchable(
                false
        );

        handler.removeCallbacks(
                musicTask
        );

        audioManager.cancelGrowl();

        captureResult(false);

        hideGameplayControls();
        applyGrayscale();

        gameOverImage.setVisibility(
                View.VISIBLE
        );

        audioManager.setMusic(
                GameAudioManager.MusicMode.LOST_INTRO
        );

        saveFinalResults();

        transitionRemaining =
                TRANSITION_DELAY;

        resultsVisible = false;

        scheduleTransition();
    }


     private void showFinalScore() {

        if (!gameState.showResults()) {
                return;
        }

        resultsVisible = true;
        transitionDueAt = 0L;

        // Make sure the intro is not blocking touches.
        introCurtain.setVisibility(View.GONE);
        gifOverlay.setVisibility(View.GONE);

        // Hide other screens.
        gameOverImage.setVisibility(View.GONE);

        gameThanksImage.setVisibility(View.GONE);
        gameThanksText.setVisibility(View.GONE);

        gameWonImage.setVisibility(View.GONE);

        // Show defeat final score background.
        lostFinalScoreImage.setVisibility(View.VISIBLE);

        // Display the captured result.
        updateFinalScore(false);

        finalScoreContainer.setVisibility(View.VISIBLE);

        defendAgainButton.setVisibility(View.VISIBLE);
        defendAgainButton.setEnabled(true);

        finalMenuButton.setVisibility(View.VISIBLE);
        finalMenuButton.setEnabled(true);

        finalActionRow.setVisibility(View.VISIBLE);
        finalActionRow.setEnabled(true);

        // Keep results and buttons in front of
        // the defeat background.
        finalScoreContainer.bringToFront();
        finalActionRow.bringToFront();

        audioManager.setMusic(
                GameAudioManager.MusicMode.LOST_RESULTS
        );

        Log.d(
                TAG,
                "Defeat results displayed: "
                        + "Defend Again and Main Menu enabled"
        );
     }


    // -----------------------------------------
    // FINAL SCORE VALUES
    // -----------------------------------------


    private void updateFinalScore(boolean victory) {

        resultPlayerName.setText(playerName);

        if (victory) {

                finalLevelText.setText(
                        "LEVEL " + resultLevel + " COMPLETE!"
                );

                int nextLevel =
                        LevelConfig.getNextLevel(resultLevel);

                resultSubtitle.setText(
                        "LEVEL " + nextLevel + " UNLOCKED"
                );

        } else {

                finalLevelText.setText(
                        "LEVEL " + resultLevel + " REACHED"
                );

                resultSubtitle.setText(
                        "THE VILLAGE NEEDS YOUR HELP!"
                );
        }

        // ACTUAL SCORE
        finalScoreText.setText(
                String.format(
                        Locale.US,
                        "%,d",
                        resultScore
                )
        );

        // ACTUAL TIME
        finalTimeText.setText(
                String.format(
                        Locale.US,
                        "%02d:%02d",
                        resultSeconds / 60,
                        resultSeconds % 60
                )
        );

        // ACTUAL WAVES
        finalWavesText.setText(
                String.format(
                        Locale.US,
                        "%,d",
                        resultWaves
                )
        );
    }


    private long getTotalTimePlayed() {

        return completedSeconds
                + gameTimer.getElapsedSeconds();
    }

    private int getTotalWaves() {

        return safeWaveCount(
                completedWaves
                        + gameView.getActualWaveCount()
        );
    }

    private int safeWaveCount(
            long waves
    ) {

        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(
                        0L,
                        waves
                )
        );
    }

    // -----------------------------------------
    // SAVE DEFEAT RESULT
    // -----------------------------------------

    private void saveFinalResults() {

        if (finalResultSaved) {
            return;
        }

        finalResultSaved = true;

        if (isGuestRun) {

            GuestProgressStore.recordResult(
                    this,
                    resultLevel,
                    resultScore,
                    resultWaves,
                    resultSeconds
            );

            return;
        }

        leaderboardRepository.saveScore(
                playerName,
                resultLevel,
                resultScore,
                resultWaves,
                resultSeconds + " seconds"
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Final leaderboard save failed",
                        error
                )
        );

        progressRepository.recordBestScore(
                resultScore
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Best score save failed",
                        error
                )
        );
    }

    // -----------------------------------------
    // GRAYSCALE
    // -----------------------------------------

    private void applyGrayscale() {

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);

        ColorMatrixColorFilter filter =
                new ColorMatrixColorFilter(matrix);

        ImageView tiles =
                findViewById(R.id.tilelayerView);

        ImageView trees =
                findViewById(R.id.treelayerView);

        if (tiles != null) {
                tiles.setColorFilter(filter);
        }

        if (trees != null) {
                trees.setColorFilter(filter);
        }
        }


        private void removeGrayscale() {

        ImageView tiles =
                findViewById(R.id.tilelayerView);

        ImageView trees =
                findViewById(R.id.treelayerView);

        if (tiles != null) {
                tiles.clearColorFilter();
        }

        if (trees != null) {
                trees.clearColorFilter();
        }
        }

    // -----------------------------------------
    // VISIBILITY
    // -----------------------------------------

    private void hideGameplayControls() {

        timerText.setVisibility(
                View.GONE
        );

        scoreText.setVisibility(
                View.GONE
        );

        waveText.setVisibility(
                View.GONE
        );

        findViewById(
                R.id.timerIcon
        ).setVisibility(
                View.GONE
        );

        findViewById(
                R.id.swordIcon
        ).setVisibility(
                View.GONE
        );

        pauseButton.setVisibility(
                View.GONE
        );

        gifOverlay.setVisibility(
                View.GONE
        );

        showPauseButtons(false);
    }

    private void restoreGameplayControls() {

        timerText.setVisibility(
                View.VISIBLE
        );

        scoreText.setVisibility(
                View.VISIBLE
        );

        waveText.setVisibility(
                View.VISIBLE
        );

        findViewById(
                R.id.timerIcon
        ).setVisibility(
                View.VISIBLE
        );

        findViewById(
                R.id.swordIcon
        ).setVisibility(
                View.VISIBLE
        );

        pauseButton.setVisibility(
                View.VISIBLE
        );

        showPauseButtons(false);
    }

    private void hideResultScreens() {

        gameThanksImage.setVisibility(
                View.GONE
        );

        gameThanksText.setVisibility(
                View.GONE
        );

        gameWonImage.setVisibility(
                View.GONE
        );

        gameOverImage.setVisibility(
                View.GONE
        );

        lostFinalScoreImage.setVisibility(
                View.GONE
        );

        finalScoreContainer.setVisibility(
                View.GONE
        );

        defendAgainButton.setVisibility(
                View.GONE
        );

        finalMenuButton.setVisibility(
                View.GONE
        );

        finalActionRow.setVisibility(
                View.GONE
        );
    }

    // -----------------------------------------
    // NAVIGATION
    // -----------------------------------------

    private void restartGame() {

        if (navigating) {
            return;
        }

        navigating = true;

        Intent intent =
                new Intent(
                        this,
                        LoadingActivity.class
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
                isGuestRun
        );

        startActivity(intent);

        applyTransition();
        finish();
    }


    private void openMainMenu() {

        if (navigating) {
                return;
        }

        navigating = true;

        Log.d(TAG, "Returning to Main Menu");

        Intent intent = new Intent(
                GameActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        applyTransition();

        finish();
    }


    private void applyTransition() {

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    // -----------------------------------------
    // ON START
    // -----------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

        if (gameState == null
                || audioManager == null
                || gameView == null) {
            return;
        }

        foreground = true;

        if (!introFinished) {

            gameView.pauseGame();

            gameView.setGameTouchable(
                    false
            );

            if (introGifDrawable != null) {

                playIntroSoundIfNeeded();

                handler.removeCallbacks(
                        halfwayTask
                );

                handler.post(
                        halfwayTask
                );
            }

            return;
        }

        if (gameState.isPaused()) {

            audioManager.setActive(
                    false
            );

            return;
        }

        audioManager.setActive(
                true
        );

        if (gameState.isPlaying()) {

            gameView.resumeGame();

            gameView.setGameTouchable(
                    true
            );

            // Restore only an already-started timer.
            // Do not start before the first giant.
            gameTimer.resume();

            refreshGameplayMusic();

        } else if (!resultsVisible
                && (gameState.isLevelComplete()
                || gameState.getState()
                        == GameStateManager.State.LOST_INTRO)) {

            scheduleTransition();
        }
    }

    // -----------------------------------------
    // ON STOP
    // -----------------------------------------

    @Override
    protected void onStop() {

        foreground = false;

        handler.removeCallbacks(
                halfwayTask
        );

        if (gameState != null) {

            if (introFinished
                    && gameState.isPlaying()) {

                // Backgrounding the app should not
                // turn it into a manually paused state.
                // This allows automatic continuation.
                gameView.pauseGame();

                gameView.setGameTouchable(
                        false
                );

                gameTimer.pause();
            }

            if (gameState.isLevelComplete()
                    || gameState.getState()
                            == GameStateManager.State.LOST_INTRO) {

                pauseTransition();
            }
        }

        handler.removeCallbacks(
                musicTask
        );

        if (audioManager != null) {
            audioManager.setActive(
                    false
            );
        }

        super.onStop();
    }

    // -----------------------------------------
    // ON DESTROY
    // -----------------------------------------

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );

        if (introGifDrawable != null
                && introAnimationCallback != null) {

            introGifDrawable
                    .unregisterAnimationCallback(
                            introAnimationCallback
                    );
        }

        introAnimationCallback = null;
        introGifDrawable = null;

        if (gameTimer != null) {
            gameTimer.release();
        }

        if (audioManager != null) {
            audioManager.release();
        }

        super.onDestroy();
    }
}