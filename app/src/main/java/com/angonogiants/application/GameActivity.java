
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private static final String EXTRA_PLAYER_NAME =
            "PLAYER_NAME";

    private static final String EXTRA_LEVEL_NUMBER =
            "LEVEL_NUMBER";

    private static final long MUSIC_DELAY = 5000L;
    private static final long INTRO_DELAY = 1500L;
    private static final long TRANSITION_DELAY = 5000L;

    // Modules
    private GameStateManager gameState;
    private GameTimer gameTimer;
    private GameAudioManager audioManager;

    private PlayerProgressRepository progressRepository;
    private LeaderboardRepository leaderboardRepository;

    // Run data
    private String playerName = "Player";
    private int levelNumber = 1;
    private int currentWave = 1;

    private long completedSeconds = 0L;
    private long completedWaves = 0L;

    private boolean foreground = false;
    private boolean introPlayed = false;
    private boolean navigating = false;
    private boolean finalResultSaved = false;

    // Views
    private GameView gameView;

    private TextView timerText;
    private TextView scoreText;
    private TextView waveText;

    private ImageButton pauseButton;
    private ImageButton restartButton;
    private ImageButton menuButton;
    private ImageButton exitButton;
    private ImageButton finalMenuButton;

    private ImageView gifOverlay;

    // Scheduling
    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private long musicReadyAt;

    private long transitionRemaining =
            TRANSITION_DELAY;

    private long transitionDueAt = 0L;

    private final Runnable musicTask =
            this::refreshGameplayMusic;

    private final Runnable nextLevelTask =
            this::startNextLevel;

    private final Runnable resultTask =
            this::showFinalScore;

    private final Runnable hideIntroTask = () -> {
        if (gifOverlay != null) {
            gifOverlay.setVisibility(View.GONE);
        }
    };

    // -----------------------------------------
    // Activity setup
    // -----------------------------------------

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

        setContentView(R.layout.activity_game);

        readPlayerData();
        initializeViews();
        initializeModules();

        setupGameView();
        setupBackgrounds();
        setupIntroAnimation();
        setupButtons();
        setupBackNavigation();

        updateTimerText(
                LevelConfig.getDurationSeconds(levelNumber)
        );

        updateScoreCounter(0);
        updateWaveCounter(1);

        musicReadyAt =
                SystemClock.uptimeMillis() + MUSIC_DELAY;
    }

    private void readPlayerData() {
        Intent intent = getIntent();

        String name =
                intent.getStringExtra(EXTRA_PLAYER_NAME);

        if (name != null && !name.trim().isEmpty()) {
            playerName = name.trim();
        }

        levelNumber = Math.max(
                1,
                intent.getIntExtra(EXTRA_LEVEL_NUMBER, 1)
        );
    }

    private void initializeViews() {
        gameView = findViewById(R.id.gameView);

        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.ScoreCounter);
        waveText = findViewById(R.id.waveCounter);

        pauseButton = findViewById(R.id.btnPauseResume);
        restartButton = findViewById(R.id.restartButton);
        menuButton = findViewById(R.id.menuButton);
        exitButton = findViewById(R.id.exitButton);
        finalMenuButton = findViewById(R.id.mainMenuButton);

        gifOverlay = findViewById(R.id.gifOverlay);

        showPauseButtons(false);
        finalMenuButton.setVisibility(View.GONE);

        TextView nameText =
                findViewById(R.id.playerNameText);

        nameText.setText(playerName);
    }

    private void initializeModules() {
        gameState = new GameStateManager();
        audioManager = new GameAudioManager(this);

        progressRepository =
                new PlayerProgressRepository();

        leaderboardRepository =
                new LeaderboardRepository();

        gameTimer = createLevelTimer(levelNumber);
    }

    private GameTimer createLevelTimer(int level) {
        return new GameTimer(
                LevelConfig.getDurationSeconds(level),
                new GameTimer.Listener() {

                    @Override
                    public void onTick(int remainingSeconds) {
                        updateTimerText(remainingSeconds);
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

        gameView.startLevel(levelNumber);
    }

    // -----------------------------------------
    // Graphics
    // -----------------------------------------

    private void setupBackgrounds() {
        ImageView tiles =
                findViewById(R.id.tilelayerView);

        ImageView trees =
                findViewById(R.id.treelayerView);

        Glide.with(this)
                .asGif()
                .load(R.drawable.tile_layer)
                .into(tiles);

        Glide.with(this)
                .asGif()
                .load(R.drawable.tree_layer)
                .into(trees);
    }

    private void setupIntroAnimation() {
        Glide.with(this)
                .asGif()
                .load(R.drawable.gametransition3)
                .into(gifOverlay);

        gifOverlay.setVisibility(View.VISIBLE);

        handler.postDelayed(
                hideIntroTask,
                INTRO_DELAY
        );
    }

    private void applyGrayscale() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);

        ColorMatrixColorFilter filter =
                new ColorMatrixColorFilter(matrix);

        ImageView tiles =
                findViewById(R.id.tilelayerView);

        ImageView trees =
                findViewById(R.id.treelayerView);

        tiles.setColorFilter(filter);
        trees.setColorFilter(filter);
    }

    // -----------------------------------------
    // HUD updates
    // -----------------------------------------

    private void updateTimerText(int seconds) {
        timerText.setText(
                String.format(
                        Locale.US,
                        "%02d:%02d",
                        seconds / 60,
                        seconds % 60
                )
        );
    }

    public void updateScoreCounter(int score) {
        runOnUiThread(() ->
                scoreText.setText("Score: " + score)
        );
    }

    public void updateWaveCounter(int wave) {
        runOnUiThread(() -> {
            currentWave = Math.max(1, wave);

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

    // -----------------------------------------
    // Timer integration
    // -----------------------------------------

    public void startTimerFromGameView() {
        runOnUiThread(() -> {
            if (gameState.isPlaying()
                    && !gameTimer.isStarted()) {
                gameTimer.start();
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
    // Buttons
    // -----------------------------------------

    private void setupButtons() {
        pauseButton.setOnClickListener(v -> {
            if (gameState.isPlaying()) {
                audioManager.playButtonSound();
                pauseGame();
            } else if (gameState.isPaused()) {
                resumeGame();
                audioManager.playButtonSound();
            }
        });

        restartButton.setOnClickListener(v -> {
            if (navigating) return;

            audioManager.playButtonSound();
            playButtonAnimation(restartButton);
            restartGame();
        });

        menuButton.setOnClickListener(v -> {
            if (navigating) return;

            audioManager.playButtonSound();
            playButtonAnimation(menuButton);
            openMainMenu();
        });

        exitButton.setOnClickListener(v -> {
            if (navigating) return;

            navigating = true;
            audioManager.playButtonSound();

            finishAffinity();
        });

        finalMenuButton.setOnClickListener(v -> {
            if (navigating) return;

            audioManager.playButtonSound();
            playButtonAnimation(finalMenuButton);
            openMainMenu();
        });
    }

    private void playButtonAnimation(ImageButton button) {
        Drawable background = button.getBackground();

        if (background instanceof AnimationDrawable) {
            AnimationDrawable animation =
                    (AnimationDrawable) background;

            animation.stop();
            animation.start();
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Back intentionally disabled during game.
                    }
                }
        );
    }

    // -----------------------------------------
    // Pause and resume
    // -----------------------------------------

    private void pauseGame() {
        if (!gameState.pause()) return;

        gameView.pauseGame();
        gameView.setGameTouchable(false);

        gameTimer.pause();

        handler.removeCallbacks(musicTask);
        audioManager.setActive(false);

        pauseButton.setImageResource(
                R.drawable.resume_button_animation
        );

        showPauseButtons(true);
    }

    private void resumeGame() {
        if (!gameState.resume()) return;

        gameView.resumeGame();
        gameView.setGameTouchable(true);

        gameTimer.resume();

        pauseButton.setImageResource(
                R.drawable.pause_button_animation
        );

        showPauseButtons(false);

        audioManager.setActive(foreground);
        refreshGameplayMusic();
    }

    private void showPauseButtons(boolean show) {
        int visibility =
                show ? View.VISIBLE : View.GONE;

        restartButton.setVisibility(visibility);
        menuButton.setVisibility(visibility);
        exitButton.setVisibility(visibility);
    }

    // -----------------------------------------
    // Gameplay music
    // -----------------------------------------

    private void refreshGameplayMusic() {
        handler.removeCallbacks(musicTask);

        if (!foreground
                || !gameState.isPlaying()
                || gameView == null) {
            return;
        }

        int lives = gameView.getTargetLives();

        if (lives <= 0) return;

        if (lives == 1) {
            audioManager.setMusic(
                    GameAudioManager.MusicMode.LOW_HEALTH
            );
            return;
        }

        long remaining =
                musicReadyAt - SystemClock.uptimeMillis();

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
        runOnUiThread(this::refreshGameplayMusic);
    }

    public void stopLowHealthMusic() {
        runOnUiThread(this::refreshGameplayMusic);
    }

    // -----------------------------------------
    // Level completion
    // -----------------------------------------

    private void handleLevelCompleted() {
        if (!gameState.completeLevel()) return;

        gameTimer.stop();

        completedSeconds +=
                gameTimer.getElapsedSeconds();

        completedWaves +=
                gameView.getActualWaveCount();

        gameView.pauseGame();
        gameView.setGameTouchable(false);
        gameView.clearAllEnemies();

        handler.removeCallbacks(musicTask);
        audioManager.cancelGrowl();

        hideGameplayControls();
        showLevelCompleteScreen();

        saveCompletedLevel();

        transitionRemaining = TRANSITION_DELAY;
        scheduleTransition();
    }

    private void showLevelCompleteScreen() {
        ImageView thanksImage =
                findViewById(R.id.gameThanksImage);

        TextView thanksText =
                findViewById(R.id.gameThanksText);

        Glide.with(this)
                .asGif()
                .load(R.drawable.gamewonthanks_background)
                .into(thanksImage);

        thanksImage.setVisibility(View.VISIBLE);

        int next = LevelConfig.getNextLevel(
                levelNumber
        );

        thanksText.setText(
                "LEVEL " + levelNumber
                        + " COMPLETE!\n"
                        + "LEVEL " + next
                        + " UNLOCKED!"
        );

        thanksText.setVisibility(View.VISIBLE);

        audioManager.setMusic(
                GameAudioManager.MusicMode.WON_INTRO
        );
    }

    private void saveCompletedLevel() {
        int completedLevel = levelNumber;
        int unlockedLevel = LevelConfig.getNextLevel(
                completedLevel
        );

        int score = gameView.getscoreCount();

        progressRepository.completeLevel(
                completedLevel,
                score
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Level unlock save failed",
                        error
                )
        );

        leaderboardRepository.saveScore(
                playerName,
                unlockedLevel,
                score,
                safeWaveCount(completedWaves),
                completedSeconds + " seconds"
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Leaderboard update failed",
                        error
                )
        );
    }

    // -----------------------------------------
    // Transition handling
    // -----------------------------------------

    private void scheduleTransition() {
        if (!foreground) return;

        boolean levelComplete =
                gameState.isLevelComplete();

        boolean showingDefeat =
                gameState.getState()
                        == GameStateManager.State.LOST_INTRO;

        if (!levelComplete && !showingDefeat) {
            return;
        }

        handler.removeCallbacks(nextLevelTask);
        handler.removeCallbacks(resultTask);

        transitionDueAt =
                SystemClock.uptimeMillis()
                        + transitionRemaining;

        handler.postDelayed(
                levelComplete ? nextLevelTask : resultTask,
                transitionRemaining
        );
    }

    private void pauseTransition() {
        if (transitionDueAt == 0L) return;

        transitionRemaining = Math.max(
                0L,
                transitionDueAt - SystemClock.uptimeMillis()
        );

        transitionDueAt = 0L;

        handler.removeCallbacks(nextLevelTask);
        handler.removeCallbacks(resultTask);
    }

    // -----------------------------------------
    // Start next level
    // -----------------------------------------

    private void startNextLevel() {
        if (!gameState.startNextLevel()) return;

        transitionDueAt = 0L;
        transitionRemaining = TRANSITION_DELAY;

        levelNumber = LevelConfig.getNextLevel(
                levelNumber
        );

        currentWave = 1;

        gameTimer.release();
        gameTimer = createLevelTimer(levelNumber);

        gameView.startLevel(levelNumber);

        ImageView thanksImage =
                findViewById(R.id.gameThanksImage);

        Glide.with(this).clear(thanksImage);

        thanksImage.setVisibility(View.GONE);

        findViewById(R.id.gameThanksText)
                .setVisibility(View.GONE);

        restoreGameplayControls();

        updateTimerText(
                LevelConfig.getDurationSeconds(levelNumber)
        );

        updateScoreCounter(
                gameView.getscoreCount()
        );

        updateWaveCounter(1);

        pauseButton.setImageResource(
                R.drawable.pause_button_animation
        );

        gameView.resumeGame();
        gameView.setGameTouchable(true);

        audioManager.setMusic(
                GameAudioManager.MusicMode.NONE
        );

        musicReadyAt =
                SystemClock.uptimeMillis() + MUSIC_DELAY;

        refreshGameplayMusic();
    }

    // -----------------------------------------
    // Defeat
    // -----------------------------------------

    public void onGameOver() {
        runOnUiThread(this::handleGameLost);
    }

    private void handleGameLost() {
        if (!gameState.lose()) return;

        gameTimer.pause();
        gameTimer.stop();

        gameView.setGameOver(true);
        gameView.setGameTouchable(false);

        handler.removeCallbacks(musicTask);
        audioManager.cancelGrowl();

        hideGameplayControls();
        applyGrayscale();

        findViewById(R.id.gameOverImage)
                .setVisibility(View.VISIBLE);

        audioManager.setMusic(
                GameAudioManager.MusicMode.LOST_INTRO
        );

        // Start cloud saving before the defeat animation.
        saveFinalResults();

        transitionRemaining = TRANSITION_DELAY;
        scheduleTransition();
    }

    // -----------------------------------------
    // Result display
    // -----------------------------------------

    private void showFinalScore() {
        if (!gameState.showResults()) return;

        transitionDueAt = 0L;

        findViewById(R.id.gameOverImage)
                .setVisibility(View.GONE);

        findViewById(R.id.lostfinalscoreiv)
                .setVisibility(View.VISIBLE);

        findViewById(R.id.finalScoreContainer)
                .setVisibility(View.VISIBLE);

        finalMenuButton.setVisibility(View.VISIBLE);

        audioManager.setMusic(
                GameAudioManager.MusicMode.LOST_RESULTS
        );

        updateFinalScore();
    }

    private void updateFinalScore() {
        int score = gameView.getscoreCount();

        long totalSeconds = getTotalTimePlayed();
        int totalWaves = getTotalWaves();

        String formattedTime = String.format(
                Locale.US,
                "%02d:%02d",
                totalSeconds / 60,
                totalSeconds % 60
        );

        TextView player =
                findViewById(R.id.playerNameText);

        TextView level =
                findViewById(R.id.finalLevelTV);

        TextView finalScore =
                findViewById(R.id.finalScoreTV);

        TextView finalTime =
                findViewById(R.id.finalScoreTimer);

        TextView finalWaves =
                findViewById(R.id.finalwaveTV);

        player.setText(playerName);
        level.setText("Level " + levelNumber);

        finalScore.setText(String.valueOf(score));
        finalTime.setText(formattedTime);
        finalWaves.setText(String.valueOf(totalWaves));
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

    private int safeWaveCount(long waves) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(0L, waves)
        );
    }

    // -----------------------------------------
    // Final cloud save
    // -----------------------------------------

    private void saveFinalResults() {
        if (finalResultSaved) return;

        finalResultSaved = true;

        int score = gameView.getscoreCount();
        long totalTime = getTotalTimePlayed();

        leaderboardRepository.saveScore(
                playerName,
                levelNumber,
                score,
                getTotalWaves(),
                totalTime + " seconds"
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Final leaderboard save failed",
                        error
                )
        );

        progressRepository.recordBestScore(
                score
        ).addOnFailureListener(error ->
                Log.e(
                        TAG,
                        "Best score save failed",
                        error
                )
        );
    }

    // -----------------------------------------
    // Show / hide game controls
    // -----------------------------------------

    private void hideGameplayControls() {
        timerText.setVisibility(View.GONE);
        scoreText.setVisibility(View.GONE);
        waveText.setVisibility(View.GONE);

        findViewById(R.id.timerIcon)
                .setVisibility(View.GONE);

        findViewById(R.id.swordIcon)
                .setVisibility(View.GONE);

        pauseButton.setVisibility(View.GONE);
        gifOverlay.setVisibility(View.GONE);

        showPauseButtons(false);
    }

    private void restoreGameplayControls() {
        timerText.setVisibility(View.VISIBLE);
        scoreText.setVisibility(View.VISIBLE);
        waveText.setVisibility(View.VISIBLE);

        findViewById(R.id.timerIcon)
                .setVisibility(View.VISIBLE);

        findViewById(R.id.swordIcon)
                .setVisibility(View.VISIBLE);

        pauseButton.setVisibility(View.VISIBLE);

        showPauseButtons(false);
    }

    // -----------------------------------------
    // Navigation
    // -----------------------------------------

    private void restartGame() {
        if (navigating) return;

        navigating = true;

        Intent intent = new Intent(
                this,
                LoadingActivity.class
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
        applyTransition();
        finish();
    }

    private void openMainMenu() {
        if (navigating) return;

        navigating = true;

        startActivity(
                new Intent(this, MainActivity.class)
        );

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
    // Lifecycle
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

        if (gameState.isPaused()) {
            audioManager.setActive(false);
            return;
        }

        audioManager.setActive(true);

        if (gameState.isPlaying()) {
            gameView.resumeGame();
            gameTimer.resume();

            if (!introPlayed) {
                introPlayed = true;
                audioManager.playIntro();
            }

            refreshGameplayMusic();

        } else if (gameState.isLevelComplete()
                || gameState.getState()
                        == GameStateManager.State.LOST_INTRO) {

            scheduleTransition();
        }
    }

    @Override
    protected void onStop() {
        foreground = false;

        if (gameState != null) {
            if (gameState.isPlaying()) {
                pauseGame();
            }

            if (gameState.isLevelComplete()
                    || gameState.getState()
                            == GameStateManager.State.LOST_INTRO) {

                pauseTransition();
            }
        }

        handler.removeCallbacks(musicTask);

        if (audioManager != null) {
            audioManager.setActive(false);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (gameTimer != null) {
            gameTimer.release();
        }

        if (audioManager != null) {
            audioManager.release();
        }

        super.onDestroy();
    }
}
