package com.example.angonogiants;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import android.media.MediaPlayer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class GameActivity extends AppCompatActivity {
    private TextView scoreCounterTextView;
    private TextView waveCounterTextView;
    private TextView timerTextView;
    private int seconds = 300;
    private boolean isTimerRunning = false;
    private Handler timerHandler = new Handler();
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private boolean gameWon = false;
    private ImageButton btnPauseResume;
    private ImageButton restartButton;
    private ImageButton menuButton;
    private ImageButton exitButton;
    private GameView gameView;
// Track when the game started to calculate timePlayed dynamically
    private long gameStartTimeMillis = -1; // Initialize to an invalid value
    private long totalPausedTimeMillis = 0; // Keep track of paused time
    private long lastPauseTimeMillis = 0; // Time when last paused
    private MediaPlayer gameOverMediaPlayer;
    private MediaPlayer buttonSoundPlayer;
    private MediaPlayer gameWonThanksMusicPlayer;
    private MediaPlayer wonFinalScoreMusicPlayer;
    private MediaPlayer lostFinalScoreMusicPlayer;
    private MediaPlayer backgroundMusicPlayer;
    private MediaPlayer lowHealthMusicPlayer;
    private MediaPlayer gifSoundPlayer;
    private boolean isLowHealthMusicPlaying = false;
    private ImageView gifOverlay;
    private Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if (isTimerRunning && !isPaused && seconds > 0) {
                seconds--;
                updateTimerUI(seconds);
                timerHandler.postDelayed(this, 1000); // Run every second
            } else if (seconds == 0 && !isGameOver) {
                stopTimer();
                checkWinCondition();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

        ImageView grazzzetView = findViewById(R.id.tilelayerView);

        Glide.with(this)
                .asGif()
                .load(R.drawable.tilelayer)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Prevent caching issues
                .into(grazzzetView);

        gameView = findViewById(R.id.gameView);

        if (gameView != null) {
            gameView.setOnLifeGivenListener(new GameView.OnLifeGivenListener() {
                @Override
                public void onLifeGiven() {
                    // When life is given, call startBackgroundMusic to check and restart if needed.
                    startBackgroundMusic2(); // No delay here, check life and restart if needed
                }
            });
        } else {
            Log.e("GameActivity", "gameView is null in onCreate!");
        }

        startBackgroundMusic(); // This will apply the 5-second delay initially.


        ImageView treelayerView = findViewById(R.id.treelayerView);

        Glide.with(this)
                .asGif()
                .load(R.drawable.treelayer)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(treelayerView);
        scoreCounterTextView = findViewById(R.id.ScoreCounter);
        waveCounterTextView = findViewById(R.id.waveCounter);
        timerTextView = findViewById(R.id.timerText);
        updateTimerUI(seconds);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        buttonSoundPlayer = MediaPlayer.create(this, R.raw.buttonsound);

        gifOverlay = findViewById(R.id.gifOverlay);

// Load GIF
        Glide.with(this)
                .asGif()
                .load(R.drawable.gametransition3)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(gifOverlay);

// Initialize MediaPlayer
        gifSoundPlayer = MediaPlayer.create(this, R.raw.gamestart);
        if (gifSoundPlayer != null) {
            gifSoundPlayer.setOnCompletionListener(mp -> {
                mp.release();
                gifSoundPlayer = null; // ✅ Prevent memory leaks
            });

            gifSoundPlayer.start();
        } else {
            Log.e("GameActivity", "Failed to create gifSoundPlayer");
        }

// Hide GIF after 1.5s (Make sure the sound isn't cut off)
        new Handler().postDelayed(() -> {
            gifOverlay.setVisibility(View.GONE);
        }, 1500);

// Initialize gameStartTimeMillis ONLY ONCE
        if (gameStartTimeMillis == -1) {
            gameStartTimeMillis = System.currentTimeMillis();
        }
        TextView playerNameText = findViewById(R.id.playerNameText);
        String playerName = getIntent().getStringExtra("PLAYER_NAME");
        if (playerName != null) {
            playerNameText.setText(playerName);
        }
// Initialize and hide the new buttons
        restartButton = findViewById(R.id.restartButton);
        menuButton = findViewById(R.id.menuButton);
        exitButton = findViewById(R.id.exitButton);
        restartButton.setVisibility(View.GONE);
        menuButton.setVisibility(View.GONE);
        exitButton.setVisibility(View.GONE);
        btnPauseResume.setOnClickListener(v -> togglePauseResume());

// Set click listeners for the new buttons
        restartButton.setOnClickListener(v -> {
            if (buttonSoundPlayer != null) {
                buttonSoundPlayer.start();
            }
            AnimationDrawable restartAnimation = (AnimationDrawable) restartButton.getBackground();
            if (restartAnimation != null) {
                restartAnimation.start();
            }
            Intent intent = new Intent(this, LoadingActivity.class);
            intent.putExtra("PLAYER_NAME", playerName);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        menuButton.setOnClickListener(v -> {
            if (buttonSoundPlayer != null) {
                buttonSoundPlayer.start();
            }
            AnimationDrawable menuAnimation = (AnimationDrawable) menuButton.getBackground();
            if (menuAnimation != null) {
                menuAnimation.start();
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        exitButton.setOnClickListener(v -> {
            if (buttonSoundPlayer != null) {
                buttonSoundPlayer.start();
            }
            AnimationDrawable exitAnimation = (AnimationDrawable) exitButton.getBackground();
            if (exitAnimation != null) {
                exitAnimation.start();
            }
            finishAffinity();
        });
// Start Background Music
        startBackgroundMusic();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }

    private void startBackgroundMusic() {
        // Apply the 5-second delay initially
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (gameView != null && gameView.getTargetLives() != 1) { // Check target lives AND gameView
                startBackgroundMusic2(); // Use the second music method
            } else {
                if (backgroundMusicPlayer == null) {
                    backgroundMusicPlayer = MediaPlayer.create(this, R.raw.bgmusic);
                    if (backgroundMusicPlayer != null) {
                        backgroundMusicPlayer.setLooping(true);
                        backgroundMusicPlayer.start();
                    } else {
                        Log.e("GameActivity", "Failed to create backgroundMusicPlayer");
                    }
                }
            }
        }, 5000); // 5 seconds delay
    }

    private void startBackgroundMusic2() {
        if (backgroundMusicPlayer == null) {
            backgroundMusicPlayer = MediaPlayer.create(this, R.raw.bgmusic); // Or a different raw file
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.setLooping(true);
                backgroundMusicPlayer.start();
            } else {
                Log.e("GameActivity", "Failed to create backgroundMusicPlayer (2)");
            }
        } else if (!backgroundMusicPlayer.isPlaying()) { // If it was playing before but stopped, restart it.
            backgroundMusicPlayer.start();
        }
    }


    public void startLowHealthMusic() {
        if (lowHealthMusicPlayer == null) {
            lowHealthMusicPlayer = MediaPlayer.create(this, R.raw.lowhealth);
            if (lowHealthMusicPlayer != null) {
                lowHealthMusicPlayer.setLooping(true);
            } else {
                Log.e("GameActivity", "Failed to create lowHealthMusicPlayer");
                return;
            }
        }

        if (!isLowHealthMusicPlaying && lowHealthMusicPlayer != null) {
            stopBackgroundMusic();
            lowHealthMusicPlayer.start();
            isLowHealthMusicPlaying = true;
        }
    }
    public void stopLowHealthMusic() {
        if (lowHealthMusicPlayer != null) {
            lowHealthMusicPlayer.stop();
            lowHealthMusicPlayer.release();
            lowHealthMusicPlayer = null;
            isLowHealthMusicPlaying = false;

            // ✅ Restart background music HERE
            startBackgroundMusic2(); // Use the non-delayed version to avoid a gap
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
//startTimer();
    }

    public void startTimerFromGameView() {
        if (!isTimerRunning) { // Ensure it starts only once
            isTimerRunning = true;
            timerHandler.postDelayed(timerRunnable, 1000);
            Log.d("GameDebug", "Timer started after first giant spawn!");
        }
    }

    private void togglePauseResume() {
        if (isPaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    private void pauseGame() {
        gameView.pauseGame(); // Pause the game logic first
        lastPauseTimeMillis = System.currentTimeMillis();

        // Pause all media players
        pauseMediaPlayer(backgroundMusicPlayer);
        pauseMediaPlayer(lowHealthMusicPlayer);
        pauseMediaPlayer(gameOverMediaPlayer);
        pauseMediaPlayer(buttonSoundPlayer);
        pauseMediaPlayer(gameWonThanksMusicPlayer);
        pauseMediaPlayer(wonFinalScoreMusicPlayer);
        pauseMediaPlayer(lostFinalScoreMusicPlayer);

        runOnUiThread(() -> {
            btnPauseResume.setImageResource(R.drawable.resume_button_animation);
            gameView.setGameTouchable(false); // Disable game interactions
            isPaused = true;
            restartButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.VISIBLE);
            exitButton.setVisibility(View.VISIBLE);
        });

        stopTimer(); // Stop the timer after the UI update
        Log.d("GameDebug", "Game paused.");

        if (buttonSoundPlayer != null) {
            buttonSoundPlayer.start();
        }
    }

    private void resumeGame() {
        gameView.resumeGame(); // Resume the game logic first
        if (lastPauseTimeMillis != 0) {
            totalPausedTimeMillis += System.currentTimeMillis() - lastPauseTimeMillis;
            lastPauseTimeMillis = 0;
        }

        // Resume all media players
        resumeMediaPlayer(backgroundMusicPlayer);
        resumeMediaPlayer(lowHealthMusicPlayer);
        resumeMediaPlayer(gameOverMediaPlayer);
        resumeMediaPlayer(buttonSoundPlayer);
        resumeMediaPlayer(gameWonThanksMusicPlayer);
        resumeMediaPlayer(wonFinalScoreMusicPlayer);
        resumeMediaPlayer(lostFinalScoreMusicPlayer);

        runOnUiThread(() -> {
            btnPauseResume.setImageResource(R.drawable.pause_button_animation);
            gameView.setGameTouchable(true);
            isPaused = false;
            restartButton.setVisibility(View.GONE);
            menuButton.setVisibility(View.GONE);
            exitButton.setVisibility(View.GONE);
        });

        startTimer(); // Start the timer after the UI update
        Log.d("GameDebug", "Game resumed.");

        if (buttonSoundPlayer != null) {
            buttonSoundPlayer.start();
        }
    }

    // Helper method to pause a MediaPlayer safely
    private void pauseMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    // Helper method to resume a MediaPlayer safely
    private void resumeMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.start();
            } catch (IllegalStateException e) {
                Log.e("GameDebug", "Error resuming media player: " + e.getMessage());
            }
        }
    }


    public void startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true;
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    public void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateTimerUI(int time) {
        int minutes = time / 60;
        int secs = time % 60;
        runOnUiThread(() -> timerTextView.setText(String.format("%02d:%02d", minutes, secs)));
    }

    public void updateScoreCounter(int count) {
        runOnUiThread(() -> scoreCounterTextView.setText("Score: " + count));
    }

    public void updateWaveCounter(int wave) {
        runOnUiThread(() -> waveCounterTextView.setText("Wave " + wave));
    }

    private void playGameOverSound() {
        if (gameOverMediaPlayer == null) {
            gameOverMediaPlayer = MediaPlayer.create(this, R.raw.gameover);
            gameOverMediaPlayer.setOnCompletionListener(mp -> stopGameOverSound());
        }
        if (gameOverMediaPlayer != null) {
            gameOverMediaPlayer.start();
        }
    }
    private void stopGameOverSound() {
        if (gameOverMediaPlayer != null) {
            gameOverMediaPlayer.stop();
            gameOverMediaPlayer.release();
            gameOverMediaPlayer = null;
        }
    }

    private void playLostFinalScoreMusic() {
        stopLostFinalScoreMusic(); // Stop any previous instance
        if (lostFinalScoreMusicPlayer == null) {
            lostFinalScoreMusicPlayer = MediaPlayer.create(this, R.raw.loztfinalzcoremuzic);
            lostFinalScoreMusicPlayer.setLooping(true); // ✅ Loop continuously
        }
        lostFinalScoreMusicPlayer.start();
    }

    private void stopLostFinalScoreMusic() {
        if (lostFinalScoreMusicPlayer != null) {
            lostFinalScoreMusicPlayer.stop();
            lostFinalScoreMusicPlayer.release();
            lostFinalScoreMusicPlayer = null;
        }
    }

    public void onGameOver() {
        if (isGameOver) return; // Prevent multiple executions
        isGameOver = true;
        stopTimer(); // Stop timer when game over

// Stop Background Music
        stopBackgroundMusic();
        stopLowHealthMusic();

// Play Game Over Sound
        playGameOverSound();
        runOnUiThread(() -> {
            View gameOverImage = findViewById(R.id.gameOverImage);
            View lostFinalScoreIV = findViewById(R.id.lostfinalscoreiv);
            View finalScoreContainer = findViewById(R.id.finalScoreContainer);

            ImageView tilelayerView = findViewById(R.id.tilelayerView);
            ImageView treelayerView = findViewById(R.id.treelayerView);

            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // Grayscale effect
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            tilelayerView.setColorFilter(filter);
            treelayerView.setColorFilter(filter);

// Hide UI elements (same as in checkWinCondition)
            timerTextView.setVisibility(View.GONE);
            scoreCounterTextView.setVisibility(View.GONE);
            waveCounterTextView.setVisibility(View.GONE);
            findViewById(R.id.zwordIcon).setVisibility(View.GONE);
            findViewById(R.id.timerIcon).setVisibility(View.GONE);
            findViewById(R.id.btnPauseResume).setVisibility(View.GONE);
            if (gameOverImage != null) {
                gameOverImage.setVisibility(View.VISIBLE);
            }
            gameView.setGameTouchable(false);
            gameView.setGameOver(true);

// If the win condition is not met, then the player lost
            if (!checkWinCondition()) {
                new Handler().postDelayed(() -> {
                    stopGameOverSound(); // Stop the game over sound when lostFinalScoreIV appears

                    if (gameOverImage != null) {
                        gameOverImage.setVisibility(View.GONE);
                    }
                    if (lostFinalScoreIV != null) {
                        lostFinalScoreIV.setVisibility(View.VISIBLE);
                    }
                    if (finalScoreContainer != null) {
                        finalScoreContainer.setVisibility(View.VISIBLE);
                    }

// ✅ Play the lost final score music
                    playLostFinalScoreMusic();
                    updateFinalScore();
                }, 5000);
            }
        });
        Log.d("GameDebug", "Game Over triggered! Score: " + gameView.getscoreCount() + ", Time Left: " + seconds);
    }

    private void playGameWonThanksMusic() {
        stopWonFinalScoreMusic(); // Stop any previous final score music
        if (gameWonThanksMusicPlayer == null) {
            gameWonThanksMusicPlayer = MediaPlayer.create(this, R.raw.gamewonthankzmuzic);
            gameWonThanksMusicPlayer.setOnCompletionListener(mp -> stopGameWonThanksMusic());
        }
        gameWonThanksMusicPlayer.start();
    }

    private void stopGameWonThanksMusic() {
        if (gameWonThanksMusicPlayer != null) {
            gameWonThanksMusicPlayer.stop();
            gameWonThanksMusicPlayer.release();
            gameWonThanksMusicPlayer = null;
        }
    }

    private void playWonFinalScoreMusic() {
        stopWonFinalScoreMusic(); // Stop any previous instance
        if (wonFinalScoreMusicPlayer == null) {
            wonFinalScoreMusicPlayer = MediaPlayer.create(this, R.raw.wonfinalzcoremuzic);
            wonFinalScoreMusicPlayer.setLooping(true); // ✅ Loop continuously
        }
        wonFinalScoreMusicPlayer.start();
    }

    private void stopWonFinalScoreMusic() {
        if (wonFinalScoreMusicPlayer != null) {
            wonFinalScoreMusicPlayer.stop();
            wonFinalScoreMusicPlayer.release();
            wonFinalScoreMusicPlayer = null;
        }
    }

    private boolean checkWinCondition() {
        if (!isGameOver && seconds == 0 && !gameView.isGameOver()) {
            gameWon = true; // Mark that the game is won
            isGameOver = true;

// Stop Background Music
            stopBackgroundMusic();
// Stop Low Health Music - check the flag
            stopLowHealthMusic();

// ✅ Clear all enemies immediately
            runOnUiThread(() -> gameView.clearAllEnemies());

            String playerName = getIntent().getStringExtra("PLAYER_NAME"); // Or "" if you have a default
            runOnUiThread(() -> {
                ImageView gameWonImage = findViewById(R.id.gameWonImage);
                ImageView gameThanksImage = findViewById(R.id.gameThanksImage);
                TextView gameThanksText = findViewById(R.id.gameThanksText);
                TextView playerNameText = findViewById(R.id.playerNameText); // If you have this TextView


// Hide UI elements
                timerTextView.setVisibility(View.GONE);
                scoreCounterTextView.setVisibility(View.GONE);
                waveCounterTextView.setVisibility(View.GONE);
                findViewById(R.id.zwordIcon).setVisibility(View.GONE);
                findViewById(R.id.timerIcon).setVisibility(View.GONE);
                findViewById(R.id.btnPauseResume).setVisibility(View.GONE);

                Glide.with(this).asGif().load(R.drawable.gamewonthanks).into(gameThanksImage);
                View finalScoreContainer = findViewById(R.id.finalScoreContainer);
                if (gameThanksImage != null) {
                    gameThanksImage.setVisibility(View.VISIBLE);

// Play game won thanks music
                    playGameWonThanksMusic();

// 2. Use the playerName variable we captured earlier:
                    gameThanksText.setText("Thank You, " + playerName + "\nfor Saving us!");
                    gameThanksText.setVisibility(View.VISIBLE);

// If you have a playerName TextView in the final score area:
                    if (playerNameText != null) {
                        playerNameText.setText(playerName); // Update the final score area as well
                    }
                } else {
                    Log.e("GameDebug", "gameThanksImage is null!");
                }
                new Handler().postDelayed(() -> {
                    if (gameWonImage != null && finalScoreContainer != null) {
                        Glide.with(this).asGif().load(R.drawable.wonfinalscore).into(gameWonImage);
                        gameThanksImage.setVisibility(View.GONE);
                        gameWonImage.setVisibility(View.VISIBLE);
                        finalScoreContainer.setVisibility(View.VISIBLE);

// Play final score music
                        playWonFinalScoreMusic();
                        updateFinalScore();
                    } else {
                        Log.e("GameDebug", "gameWonImage or finalScoreContainer is null!");
                    }
                }, 5000);
            });
            gameView.stopGame();
            Log.d("GameDebug", "Win condition met!");
            return true;
        }
        return gameWon;
    }

    private void updateFinalScore() {
        TextView finalScoreText = findViewById(R.id.finalScoreTV);
        TextView finalScoreTimer = findViewById(R.id.finalScoreTimer);
        TextView playerNameText = findViewById(R.id.playerNameText);
        TextView finalScoreWave = findViewById(R.id.finalwaveTV);
        int scoreCount = gameView.getscoreCount();

// ✅ Calculate raw time played (no "Time Played: " label)
        int initialGameDurationSeconds = 300; // Game duration
        int timePlayedSeconds = initialGameDurationSeconds - seconds;
        int minutes = timePlayedSeconds / 60;
        int secs = timePlayedSeconds % 60;
        String formattedTime = String.format("%02d:%02d", minutes, secs);

// ✅ Set raw values without labels
        finalScoreTimer.setText(formattedTime);
        finalScoreText.setText(String.valueOf(scoreCount));

// ✅ Extract raw wave count
        String waveCountStr = waveCounterTextView.getText().toString().replace("Wave ", "");
        int waveCount = 0;
        try {
            waveCount = Integer.parseInt(waveCountStr);
        } catch (NumberFormatException e) {
            Log.e("GameDebug", "Error parsing wave count: " + e.getMessage());
        }
        finalScoreWave.setText(String.valueOf(waveCount)); // ✅ Set wave count without "Wave: "

// ✅ Set raw player name (no extra text)
        String playerName = getIntent().getStringExtra("PLAYER_NAME");
        playerNameText.setText((playerName != null && !playerName.isEmpty()) ? playerName : "Player");

// ✅ Show Main Menu Button
        ImageButton mainMenuButton = findViewById(R.id.mainMenuButton);
        mainMenuButton.setVisibility(View.VISIBLE);

// ✅ Save the final score
        saveFinalScoreToFirebase(playerName, scoreCount, waveCount, gameWon);
        mainMenuButton.setOnClickListener(v -> {
            if (buttonSoundPlayer != null) {
                buttonSoundPlayer.start(); // Play button sound
            }
            AnimationDrawable animationDrawable = (AnimationDrawable) mainMenuButton.getBackground();
            if (animationDrawable != null) {
                animationDrawable.start();
            }
// ✅ Stop music before returning to main menu
            stopLostFinalScoreMusic();
            stopWonFinalScoreMusic();
// Start the animation drawable

// Redirect to MainActivity
            startActivity(new Intent(GameActivity.this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out); // Correct placement
            finish();
        });
        Log.d("GameDebug", "Final Score Updated. Score: " + scoreCount + ", Time Played: " + timePlayedSeconds + " sec.");
    }

    private void saveFinalScoreToFirebase(String playerName, int score, int waveCount, boolean gameWon) {
        String timePlayed = gameWon ? "Survived Game" : (300 - seconds) + " seconds";

        // 1. Define your actual URL here:
        String databaseUrl = "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

        // 2. Pass that URL into the getInstance() method:
        DatabaseReference database = FirebaseDatabase.getInstance(databaseUrl).getReference("leaderboard");

        PlayerScore playerScore = new PlayerScore(playerName, score, timePlayed, waveCount);

        database.push().setValue(playerScore)
                .addOnSuccessListener(aVoid -> Log.d("GameDebug", "SUCCESS: Score uploaded!"))
                .addOnFailureListener(e -> Log.e("GameDebug", "ERROR: " + e.getMessage()));
    }

//    private void saveFinalScoreToFile(String playerName, int score, int waveCount, boolean gameWon) {
//        String timePlayed = gameWon ? "Survived Game" : (300 - seconds) + " seconds"; // "Survived Game" if won
//        File file = new File(getExternalFilesDir(null), "leaderboard.txt");
//        List<String> lines = new ArrayList<>();
//        boolean playerExists = false;
//        try {
//// Read existing leaderboard file
//            if (file.exists()) {
//                BufferedReader br = new BufferedReader(new FileReader(file));
//                String line;
//                while ((line = br.readLine()) != null) {
//                    String[] parts = line.split(",");
//                    if (parts.length == 4) {
//                        String existingName = parts[0].trim();
//                        if (!existingName.equalsIgnoreCase(playerName)) {
//                            lines.add(line); // Keep other players
//                        } else {
//                            playerExists = true; // Player already exists, we will replace the entry
//                        }
//                    }
//                }
//                br.close();
//            }
//
//// ✅ Add the updated or new entry
//            lines.add(playerName + "," + score + "," + timePlayed + "," + waveCount);
//
//// ✅ Sort entries by highest score first
//            lines.sort((a, b) -> {
//                int scoreA = Integer.parseInt(a.split(",")[1].trim());
//                int scoreB = Integer.parseInt(b.split(",")[1].trim());
//                return Integer.compare(scoreB, scoreA); // Descending order
//            });
//// ✅ Write the sorted leaderboard back to the file
//            FileWriter fw = new FileWriter(file, false); // Overwrite file
//            for (String entry : lines) {
//                fw.write(entry + "\n");
//            }
//            fw.close();
//            Log.d("GameDebug", playerExists ? "Updated existing record." : "New record added.");
//        } catch (IOException e) {
//            Log.e("GameDebug", "Error saving leaderboard file: " + e.getMessage());
//        }
//    }

    private void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopGameWonThanksMusic();
        stopWonFinalScoreMusic(); // ✅ Ensure the looping music stops
        stopLostFinalScoreMusic(); // ✅ Ensure the looping music stops
        stopBackgroundMusic(); // Stop background music on destroy

        if (buttonSoundPlayer != null) {
            buttonSoundPlayer.release();
            buttonSoundPlayer = null;
        }

        if (lowHealthMusicPlayer != null) {
            lowHealthMusicPlayer.release();
            lowHealthMusicPlayer = null;
            isLowHealthMusicPlaying = false; // Reset the flag in onDestroy as well
        }

        // ✅ Stop and release gifSoundPlayer to prevent muting issues
        if (gifSoundPlayer != null) {
            if (gifSoundPlayer.isPlaying()) {
                gifSoundPlayer.stop(); // Ensure it stops before release
            }
            gifSoundPlayer.release();
            gifSoundPlayer = null;
        }

        super.onDestroy();
    }

}