//app\src\main\java\com\angonogiants\application\GameView.java
package com.angonogiants.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import android.media.MediaPlayer;

public class GameView extends View {

    private boolean running = false;
    private Thread gameThread;
    private List<Enemy> enemies = new ArrayList<>();
    private Random random = new Random();

    private Bitmap  targetBitmap, backgroundBitmap, heartBitmap,  treeBitmap;
    private Bitmap[] targetStates, heartStates;

    private float targetX, targetY;
    private int screenWidth, screenHeight;

    private int targetLives = 3;
    private final int targetRadius = 100;

    private long lastSpawnTime = 0;
    private final int spawnInterval = 1500;
    private final int waveDelay = 1000;
    private int score = 0;
    private List<ScorePopup> scorePopups= new ArrayList<>();

    private int waveCount = 0;
    private int actualWaveCount = 0;
    private int displayedWaveCount = 1;

    private Handler handler = new Handler();
    private Paint textPaint;
    private Paint scorePopupPaint;

    private Bitmap[] enemyTopBitmaps;
    private Bitmap[] enemyBottomBitmaps;
    private int enemySize; // Size for enemies
    private final int MIN_ENEMY_DISTANCE = 150;
    private List<Enemy> enemiesToRemove = new ArrayList<>();
    private boolean isGamePaused = false;
    private boolean waveInProgress = false;
    private boolean isGameOver = false; // Track game over state
    private boolean isGameLost = false; // Track if the game was lost

    private Enemy lifeGivingEnemy = null; // Track the special enemy
    private boolean lifeGivingEnemySpawned = false; // Flag to prevent multiple spawns
    private static final long LIFE_ENEMY_RESPAWN_DELAY = 10000; // 10 seconds delay
    private long lastLifeEnemySpawnTime = 0;
    private SoundPool soundPool;
    private int soundCase0_3, soundCase1_4, soundCase2_5, soundCase6;

    private MediaPlayer growlMediaPlayer;
    private MediaPlayer waveGrowlMediaPlayer;
    private boolean hasPlayedGrowl = false; // Ensure it plays only once

    private static final int HORIZONTAL_MARGIN_DP = 40;
    private int horizontalMarginPx; // Converted margin in pixels
    private Vibrator vibrator; // Add Vibrator instance
    private Bitmap redGradientBitmap;
    private boolean showRedGradient = false;
    private long redGradientStartTime = 0;
    private final int RED_GRADIENT_DURATION = 500; // 1.5 seconds
    private boolean isRedGradientBlinking = false;
    private long lastBlinkTime = 0;
    private final int BLINK_INTERVAL = 500; // 500ms for blinking effect
    private boolean hasSpawnedFirstGiant = false; // Track if first giant has spawned

    private int loseLifeSound, gainLifeSound; // Declare sound IDs
    private OnLifeGivenListener lifeGivenListener;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void clearAllEnemies() {
        enemies.clear(); //  Remove all enemies
        postInvalidate(); // Force UI refresh
        Log.d("GameDebug", "All enemies removed after winning.");
    }


    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
        postInvalidate(); // Redraw the screen
    }

    public void pauseGame() {
        isGamePaused = true;
        handler.removeCallbacksAndMessages(null); // Cancel all scheduled tasks
        waveInProgress = false; // Allow new waves to be scheduled after resuming
    }


    public void resumeGame() {
        isGamePaused = false;

        // Ensure a new wave can start if needed
        if (!waveInProgress) {
            waveInProgress = true;
            handler.postDelayed(() -> {
                if (!isGamePaused) {
                    spawnWave();
                    lastSpawnTime = System.currentTimeMillis();
                    waveInProgress = false;
                }
            }, waveDelay);
        }

        invalidate(); // Force a redraw
    }



    public void stopGame() {
        running = false;
        isGameOver = true;
        isGameLost = false; // Ensure no grayscale effect if the player wins
        postInvalidate();
    }
    public boolean isGameOver() {
        return isGameOver;
    }

    private void init(Context context) {
        initSounds(context);
       vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE); // Initialize Vibrator
        enemySize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
        redGradientBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.red_warning);

        // Convert 40dp to pixels
        horizontalMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, HORIZONTAL_MARGIN_DP, getResources().getDisplayMetrics());

        enemyTopBitmaps = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes1top),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes2top),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes3top),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes4top),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes5top),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes6top),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes7top) // Add the new top bitmap
        };
        enemyBottomBitmaps = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes1bottom),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes2bottom),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes3bottom),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes4bottom),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes5bottom),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes6bottom),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.higantes7bottom) // Add the new bottom bitmap
        };

        heartStates = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.heart_full),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.heart_two),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.heart_one),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.heart_empty)
        };


        targetStates = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.stall01),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.stall02),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.stall03),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.stall_destroyed)
        };

        targetBitmap = targetStates[0];
        heartBitmap = heartStates[0];
        try {
        Typeface typeface = ResourcesCompat.getFont(context, R.font.slkscr);

        textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(60);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(typeface);
        scorePopupPaint = new Paint();
        scorePopupPaint.setColor(0xFFFFFFFF);
        scorePopupPaint.setTextSize(60);
        scorePopupPaint.setTypeface(typeface);
        } catch (Exception e) {
            Log.e("GameView", "Error loading typeface", e);
        }

    }

    private void initSounds(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        soundCase0_3 = soundPool.load(context, R.raw.case0_3, 1);
        soundCase1_4 = soundPool.load(context, R.raw.case1_4, 1);
        soundCase2_5 = soundPool.load(context, R.raw.case2_5, 1);
        soundCase6 = soundPool.load(context, R.raw.case6, 1);
        //  Load new sounds
        loseLifeSound = soundPool.load(context, R.raw.lose_life, 1);
        gainLifeSound = soundPool.load(context, R.raw.gain_life, 1);
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        targetX = w / 2f;
        targetY = h / 2f;


        if (backgroundBitmap != null) {
            backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, w, h, true);
            treeBitmap = Bitmap.createScaledBitmap(treeBitmap, w, h, true);
        }
        //  Scale red_warning.png to fit the screen
        if (redGradientBitmap != null) {
            redGradientBitmap = Bitmap.createScaledBitmap(redGradientBitmap, w, h, true);
        }

        for (int i = 0; i < enemyTopBitmaps.length; i++) {
            enemyTopBitmaps[i] = Bitmap.createScaledBitmap(enemyTopBitmaps[i], enemySize, enemySize, true);
            enemyBottomBitmaps[i] = Bitmap.createScaledBitmap(enemyBottomBitmaps[i], enemySize, enemySize, true);
        }



        if (!running) {
            running = true;
            gameThread = new Thread(this::gameLoop);
            gameThread.start();
        }

    }


    private void gameLoop() {
        while (running) {
            update();
            postInvalidate();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (isGamePaused) return; // Prevent updates when paused
        long currentTime = System.currentTimeMillis();

        // Handle red gradient visibility
        if (showRedGradient && targetLives == 2) {
            if (currentTime - redGradientStartTime > RED_GRADIENT_DURATION) {
                showRedGradient = false;
            }
        }

        // Prevent new waves from being spawned while paused
        if (!isGamePaused && !waveInProgress && screenWidth > 0 && screenHeight > 0 && currentTime - lastSpawnTime > spawnInterval) {
            waveInProgress = true;
            handler.postDelayed(() -> {
                if (!isGamePaused) { // Only spawn if the game is still running
                    spawnWave();
                    lastSpawnTime = System.currentTimeMillis();
                    waveInProgress = false;
                    actualWaveCount++;

                    if (actualWaveCount % 5 == 0) {
                        displayedWaveCount++;
                        updateWaveCounterUI(displayedWaveCount);
                        playWaveGrowlSound(() -> {}); // Play the sound here
                    }
                }
            }, waveDelay);
        }

        processEnemies(currentTime);
        removeEnemies();
        updateScorePopups();
    }


    private void processEnemies(long currentTime) {
        if (isGamePaused) return; // Prevent processing when paused
        List<Enemy> enemiesCopy = new ArrayList<>(enemies);

        for (Enemy enemy : enemiesCopy) {
            enemy.update();

            float distanceToTarget = (float) Math.sqrt(Math.pow(enemy.x - targetX, 2) + Math.pow(enemy.y - targetY, 2));

            if (distanceToTarget < targetRadius) {
                enemies.remove(enemy);

                if (enemy.enemyType == 6) { // Life-Giving Enemy Reached Target
                    handleLifeEnemyReachedTarget();
                } else {
                    reduceTargetLife();
                }
                break;
            }
        }
    }

    private void handleLifeEnemyReachedTarget() {
        if (targetLives < 3) {
            targetLives++;
            targetBitmap = targetStates[3 - targetLives];
            heartBitmap = heartStates[3 - targetLives];

            checkAndStopLowHealthMusic();

            if (soundPool != null) {
                soundPool.play(gainLifeSound, 1, 1, 0, 0, 1);
            }
        }
        lifeGivingEnemySpawned = false;
        lifeGivingEnemy = null;
        lastLifeEnemySpawnTime = System.currentTimeMillis();
    }

    private void removeEnemies() {
        if (!enemiesToRemove.isEmpty()) {
            enemies.removeAll(enemiesToRemove);
            enemiesToRemove.clear();
        }
    }

    private void updateScorePopups() {
        if (isGamePaused) return; // Prevent updates when paused
        Iterator<ScorePopup> popupIterator = scorePopups.iterator();
        while (popupIterator.hasNext()) {
            ScorePopup popup = popupIterator.next();
            if (popup != null && !popup.update()) {
                popupIterator.remove();
            }
        }
    }

    private void spawnWave() {
        if (isGamePaused) return; // Prevent wave spawning when paused

        if (waveCount == 0 && !hasPlayedGrowl) {
            hasPlayedGrowl = true;
            waveInProgress = true;

            playGiantGrowlSound(() -> {
                Log.d("GameView", "Giant Growl finished. Now spawning first wave...");

                handler.postDelayed(() -> {
                    if (!isGamePaused) { // Ensure we are not spawning while paused
                        waveCount++;
                        startWaveSequence();
                        waveInProgress = false;
                    }
                }, 1000);
            });
        } else {
            waveCount++;
            startWaveSequence();
        }
    }


    private void startWaveSequence() {
        if (waveCount == 1 && !hasSpawnedFirstGiant) {
            // Start Timer on First Giant Spawn only if it is the first wave
            hasSpawnedFirstGiant = true;
            ((GameActivity) getContext()).startTimerFromGameView();
        }

        if (waveCount > 1 && waveCount % 5 == 0) {
            playWaveGrowlSound(() -> {
                actuallySpawnWave();
            });
        } else {
            actuallySpawnWave();
        }
    }

    private void playGiantGrowlSound(Runnable onComplete) {
        if (growlMediaPlayer == null) {
            growlMediaPlayer = MediaPlayer.create(getContext(), R.raw.giantgrowl);
            if (growlMediaPlayer == null) {
                Log.e("GameView", "Failed to create growlMediaPlayer");
                if (onComplete != null) {
                    onComplete.run(); // Call onComplete even if MediaPlayer creation fails
                }
                return;
            }

            growlMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                growlMediaPlayer = null;
                if (onComplete != null) {
                    onComplete.run(); // Call onComplete after growl finishes
                }
            });
        }

        if (growlMediaPlayer != null) {
            growlMediaPlayer.start();
            growlMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                growlMediaPlayer = null;
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        } else {
            if (onComplete != null) {
                onComplete.run(); // If sound fails, call onComplete immediately
            }
        }
    }

    private void playWaveGrowlSound(Runnable onComplete) {
        if (waveGrowlMediaPlayer == null) {
            waveGrowlMediaPlayer = MediaPlayer.create(getContext(), R.raw.wavegrowl);
            if (waveGrowlMediaPlayer == null) {
                Log.e("GameView", "Failed to create waveGrowlMediaPlayer");
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            waveGrowlMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                waveGrowlMediaPlayer = null;
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        }

        if (waveGrowlMediaPlayer != null) {
            waveGrowlMediaPlayer.start();
            waveGrowlMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                waveGrowlMediaPlayer = null;
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        } else {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private float calculateEnemySpeed(int wave, int enemyType) {
        float[] baseSpeeds = {3f, 3.5f, 4f, 3f, 3.5f, 4f, 3f}; // Added base speed for life enemy
        int displayWave = (wave - 1) / 5; // Calculate the displayed wave count
        float speedIncrease = displayWave * 0.5f;
        float maxSpeed = 11f;

        if (enemyType >= 0 && enemyType < baseSpeeds.length) {
            return Math.min(baseSpeeds[enemyType] + speedIncrease, maxSpeed);
        } else {
            Log.e("GameView", "Invalid enemyType in calculateEnemySpeed: " + enemyType);
            return 3f;
        }
    }
    private void actuallySpawnWave(){
        int baseWaveSize = 1;
        int additionalEnemies = waveCount / 5;
        int waveSize = Math.min(baseWaveSize + additionalEnemies, 9);

        lifeGivingEnemySpawned = false;
        lifeGivingEnemy = null;

        Handler waveHandler = new Handler();
        long spawnDelay = 200;

        for (int i = 0; i < waveSize; i++) {
            final int index = i;
            waveHandler.postDelayed(() -> {
                spawnEnemy(waveCount);
            }, index * spawnDelay);
        }
    }

    private void spawnEnemy(int waveCount) {
        int attempts = 0;
        int x, y;
        boolean spawnFromTop;

        while (attempts < 10) {
            x = random.nextInt(screenWidth - 100) + 50;
            spawnFromTop = random.nextBoolean();
            y = spawnFromTop ? 0 : screenHeight;

            boolean validPosition = true;
            for (Enemy existingEnemy : enemies) {
                float distance = (float) Math.sqrt(Math.pow(x - existingEnemy.x, 2) + Math.pow(y - existingEnemy.y, 2));
                if (distance < MIN_ENEMY_DISTANCE) {
                    validPosition = false;
                    break;
                }
            }

            if (validPosition) {
                int enemyType = random.nextInt(6); // Regular enemy type
                float speed = calculateEnemySpeed(waveCount, enemyType);
                int score = getScoreForEnemyType(enemyType, x, y, targetX, targetY);
                enemies.add(new Enemy(x, y, speed, spawnFromTop, enemyType, score));

                // Life-giving enemy logic - Only spawn ONE per wave, if lives < 3, and after delay
                long currentTime = System.currentTimeMillis();
                if (!lifeGivingEnemySpawned && targetLives < 3 && currentTime - lastLifeEnemySpawnTime > LIFE_ENEMY_RESPAWN_DELAY && random.nextInt(3) == 0) {
                    spawnLifeEnemy(waveCount, x, y, spawnFromTop, currentTime);
                }
                return; // Exit the loop after spawning an enemy
            }
            attempts++;
        }

        // Spawn enemy if no valid position found after 10 attempts
        x = random.nextInt(screenWidth - 100) + 50;
        spawnFromTop = random.nextBoolean();
        y = spawnFromTop ? 0 : screenHeight;
        int enemyType = random.nextInt(6);
        float speed = calculateEnemySpeed(waveCount, enemyType);
        int score = getScoreForEnemyType(enemyType, x, y, targetX, targetY);
        enemies.add(new Enemy(x, y, speed, spawnFromTop, enemyType, score));
    }

    private void spawnLifeEnemy(int waveCount, int x, int y, boolean spawnFromTop, long currentTime) {
        int lifeEnemyX = x; // Try to spawn life enemy near regular enemy
        int lifeEnemyY = y;
        boolean lifeEnemyValidPos = true;
        for (Enemy existingEnemy : enemies) {
            float distance = (float) Math.sqrt(Math.pow(lifeEnemyX - existingEnemy.x, 2) + Math.pow(lifeEnemyY - existingEnemy.y, 2));
            if (distance < MIN_ENEMY_DISTANCE) {
                lifeEnemyValidPos = false;
                break;
            }
        }
        if (!lifeEnemyValidPos) {
            lifeEnemyX = random.nextInt(screenWidth - 100) + 50;
            lifeEnemyY = spawnFromTop ? 0 : screenHeight;
        }
        int lifeEnemyType = 6;
        float lifeEnemySpeed = calculateEnemySpeed(waveCount, lifeEnemyType);
        int lifeEnemyScore = getScoreForEnemyType(lifeEnemyType, lifeEnemyX, lifeEnemyY, targetX, targetY);
        Enemy newLifeGivingEnemy = new Enemy(lifeEnemyX, lifeEnemyY, lifeEnemySpeed, spawnFromTop, lifeEnemyType, lifeEnemyScore);
        enemies.add(newLifeGivingEnemy);
        lifeGivingEnemy = newLifeGivingEnemy;
        lifeGivingEnemySpawned = true;
        lastLifeEnemySpawnTime = currentTime;
    }
    private int getScoreForEnemyType(int enemyType, float enemyX, float enemyY, float targetX, float targetY) {
        int baseScore;

        switch (enemyType) {
            case 0:
            case 3:
                baseScore = 100;
                break;
            case 1:
            case 4:
                baseScore = 250;
                break;
            case 2:
            case 5:
                baseScore = 400;
                break;
            case 6:
                return -1000; // Life enemy always -1000
            default:
                return 0;
        }

        // Calculate distance from enemy to target
        float distance = (float) Math.sqrt(Math.pow(enemyX - targetX, 2) + Math.pow(enemyY - targetY, 2));

        // Define min and max distance
        float minDistance = 100f;
        float maxDistance = 800f;

        // Ensure distanceFactor stays between 0 and 1
        float distanceFactor = 1 - Math.max(0, Math.min(1, (distance - minDistance) / (maxDistance - minDistance)));

        // Cap the bonus to 120% of baseScore
        int maxBonus = (int) (baseScore * 0.2f);
        int bonus = (int) (maxBonus * distanceFactor);

        // Add slight random offset to break patterns
        Random rand = new Random();
        int randomOffset = rand.nextInt(10) - 5; // Random number between -5 and +5

        return baseScore + bonus + randomOffset;
    }


    public interface OnLifeGivenListener {
        void onLifeGiven();
    }
    public void setOnLifeGivenListener(OnLifeGivenListener listener) {
        this.lifeGivenListener = listener;
    }

    public int getTargetLives() {
        return targetLives;
    }

    private void reduceTargetLife() {
        if (targetLives > 0) {
            targetLives--;

            //  Play lose life sound
            if (soundPool != null) {
                soundPool.play(loseLifeSound, 1, 1, 0, 0, 1);
            }

            //  Deduct a fixed 200 points (no more doubling)
            int deduction = 1000; // Always subtract 200 points
            score -= deduction;  // Allow negative scores

            //  Show the negative score popup
            scorePopups.add(new ScorePopup(targetX, targetY, -deduction, true)); // Red popup for deduction

            // Update target/heart visuals
            targetBitmap = targetStates[3 - targetLives];
            heartBitmap = heartStates[3 - targetLives];

            // Handle UI updates and effects
            if (targetLives == 2) {
                vibrate();
                showRedGradient = true;
                redGradientStartTime = System.currentTimeMillis();
                postInvalidate();
                Context context = getContext();
                if (context instanceof GameActivity) {
                    ((GameActivity) context).stopLowHealthMusic();
                }
            } else if (targetLives == 1) {
                vibrate();
                showRedGradient = true;
                isRedGradientBlinking = true;
                postInvalidate();

                if (lifeGivenListener != null && lifeGivingEnemy != null) {
                    lifeGivenListener.onLifeGiven();
                }

                Context context = getContext();
                if (context instanceof GameActivity) {
                    ((GameActivity) context).startLowHealthMusic();
                }
            } else if (targetLives == 0) {
                Log.d("GameView", "Target life reached 0, stopping low health music");
                isRedGradientBlinking = false;

                Context context = getContext();
                if (context instanceof GameActivity) {
                    ((GameActivity) context).stopLowHealthMusic();
                }

                vibrate();
                isGameOver = true;
                gameOver();
            }

            //  Update UI with the new score
            ((GameActivity) getContext()).updateScoreCounter(score);
        }
    }

    public void checkAndStopLowHealthMusic() {
        if (targetLives > 1) { // If lives are greater than 1, stop low health music
            Context context = getContext();
            if (context instanceof GameActivity) {
                ((GameActivity) context).stopLowHealthMusic();
            }
        }
    }

    private void vibrate() {
        if (vibrator != null) {
            // Vibrate for 200 milliseconds
            vibrator.vibrate(300);  // You can adjust the duration
        }
    }

    private void gameOver() {
        running = false;

        if (targetLives == 0) {
            isGameLost = true; // Player lost
        }

        handler.post(() -> {
        });            ((GameActivity) getContext()).onGameOver();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean layerSaved = false; // Track if a layer was saved
        if (!isGamePaused) {
            // Normal game logic runs when not paused
            update();
        }
        super.onDraw(canvas);

        if (isGameLost) { // Only apply grayscale if the game was lost
            Paint grayscalePaint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            grayscalePaint.setColorFilter(new ColorMatrixColorFilter(cm));
            canvas.saveLayer(null, grayscalePaint);
            layerSaved = true;
        }

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
            canvas.drawBitmap(treeBitmap, 0, 0, null);
        }

        if (showRedGradient) {
            // Ensure background is cleared before drawing gradient
            canvas.drawColor(Color.TRANSPARENT);

            if (targetLives == 2) {
                canvas.drawBitmap(redGradientBitmap, 0, 0, null);
            } else if (targetLives == 1) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBlinkTime > BLINK_INTERVAL) {
                    lastBlinkTime = currentTime;
                    isRedGradientBlinking = !isRedGradientBlinking;
                    postInvalidate(); //  Force redraw to avoid lingering overlays
                }

                if (isRedGradientBlinking) {
                    canvas.drawBitmap(redGradientBitmap, 0, 0, null);
                }
            }
        }

        long currentTime = System.currentTimeMillis();
         // Draw the enemies that spawn from the TOP first (behind the stall)
        // TOP enemies
        for (Enemy enemy : enemies) {
            if (enemy.isTopEnemy) {
                float tiltAngle = (float) (Math.sin((currentTime - enemy.lastTiltTime) * enemy.tiltSpeed) * 15);
                drawRotatedBitmap(canvas, enemy.bitmap, enemy.x, enemy.y, tiltAngle);
            }
        }

        // STALL
        if (targetBitmap != null) {
            float stallX = targetX - targetBitmap.getWidth() / 2;
            float stallY = targetY - targetBitmap.getHeight() / 2;
            canvas.drawBitmap(targetBitmap, stallX, stallY, null);
        }


        // BOTTOM enemies
        for (Enemy enemy : enemies) {
            if (!enemy.isTopEnemy) {
                float tiltAngle = (float) (Math.sin((currentTime - enemy.lastTiltTime) * enemy.tiltSpeed) * 15);
                drawRotatedBitmap(canvas, enemy.bitmap, enemy.x, enemy.y, tiltAngle);
            }
        }


        if (heartBitmap != null) {
            float heartX = targetX - heartBitmap.getWidth() / 2;
            float heartY = targetY - targetBitmap.getHeight() / 2 - 100;
            canvas.drawBitmap(heartBitmap, heartX, heartY, null);
        }


        // Draw score popups
        for (ScorePopup popup : scorePopups) {
            popup.draw(canvas, scorePopupPaint);
        }

        // Only restore the canvas if we saved a layer
        if (layerSaved) {
            canvas.restore();
        }

    }
    public void setGameTouchable(boolean enabled) {
        setEnabled(enabled);
        setFocusable(enabled);
        setFocusableInTouchMode(enabled);
    }
    private void drawRotatedBitmap(Canvas canvas, Bitmap bitmap, float x, float y, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
        matrix.postTranslate(x - bitmap.getWidth() / 2f, y - bitmap.getHeight() / 2f);
        canvas.drawBitmap(bitmap, matrix, null);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGamePaused) {
            return false;
        }

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleTouch(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // Optional: Handle release events if needed
                break;
        }
        return true;
    }

    private void handleTouch(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<Enemy> enemiesCopy = new ArrayList<>(enemies);
        boolean regularEnemyHit = false;

        for (Enemy enemy : enemiesCopy) {
            if (enemy != lifeGivingEnemy && Math.sqrt(Math.pow(x - enemy.x, 2) + Math.pow(y - enemy.y, 2)) < 140) {
                enemiesToRemove.add(enemy);
                playEnemyHitSound(enemy.enemyType);

                //  Calculate the score based on distance
                int earnedScore = getScoreForEnemyType(enemy.enemyType, enemy.x, enemy.y, targetX, targetY);
                score += earnedScore;

                scorePopups.add(new ScorePopup(enemy.x, enemy.y, earnedScore, false)); // Regular enemies: white popup
                ((GameActivity) getContext()).updateScoreCounter(score);
                regularEnemyHit = true;
            }
        }

        if (!regularEnemyHit && lifeGivingEnemy != null && Math.sqrt(Math.pow(x - lifeGivingEnemy.x, 2) + Math.pow(y - lifeGivingEnemy.y, 2)) < 140) {
            enemiesToRemove.add(lifeGivingEnemy);
            playEnemyHitSound(lifeGivingEnemy.enemyType);

            score -= 1000;
            lifeGivingEnemy = null;
            lifeGivingEnemySpawned = true;
            lastLifeEnemySpawnTime = System.currentTimeMillis();

            scorePopups.add(new ScorePopup(x, y, -1000, true)); // Life-giving enemy: red popup
            ((GameActivity) getContext()).updateScoreCounter(score);
        }
    }


    private void playEnemyHitSound(int enemyType) {
        if (soundPool == null) return; // Prevent crashes if soundPool is null

        int soundId = -1; // Default invalid ID
        switch (enemyType) {
            case 0:
            case 3:
                soundId = soundCase0_3;
                break;
            case 1:
            case 4:
                soundId = soundCase1_4;
                break;
            case 2:
            case 5:
                soundId = soundCase2_5;
                break;
            case 6:
                soundId = soundCase6;
                break;
        }

        // Only play if a valid sound ID exists
        if (soundId > 0) {
            soundPool.play(soundId, 1, 1, 0, 0, 1);
        } else {
            Log.e("GameView", "Invalid sound ID for enemyType: " + enemyType);
        }
    }

    private void updateWaveCounterUI(int wave) {
        ((GameActivity) getContext()).updateWaveCounter(wave);
    }

    public int getscoreCount() {
        return score;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        running = false; // Stop the game loop

        // Stop the game thread safely
        if (gameThread != null) {
            try {
                gameThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                e.printStackTrace(); // Log the exception
            }
        }

        // Release sound resources safely
        if (soundPool != null) {
            soundPool.release(); // Release the SoundPool resources
            soundPool = null; // Set soundPool to null to prevent further use
        }

        // Cancel any ongoing vibrations
        if (vibrator != null) {
            vibrator.cancel(); // Stop any currently running vibrations
        }

        // Release MediaPlayer resources (if you are using it)
        if (growlMediaPlayer != null) {
            growlMediaPlayer.release();
            growlMediaPlayer = null;
        }
    }

    private class ScorePopup {
        float x, y;
        int score;
        int alpha = 255; // Start fully visible
        long startTime;
        final int duration = 1000; // 1 second fade-out duration
        boolean isRed;

        public ScorePopup(float x, float y, int score, boolean isRed) {
            this.x = x;
            this.y = y;
            this.score = score;
            this.startTime = System.currentTimeMillis();
            this.isRed = isRed;
        }

        public boolean update() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > duration) {
                return false; // Remove the popup when time is up
            }

            float progress = (float) elapsed / duration; // Value between 0 and 1
            y -= 2; // Move upward
            alpha = (int) (255 * (1 - progress)); // Opacity reduces gradually

            return true;
        }

        public void draw(Canvas canvas, Paint scorePopupPaint) {
            if (alpha <= 0) return; // Don't draw if fully transparent

            scorePopupPaint.setAlpha(alpha); // Set opacity

            if (isRed) {
                scorePopupPaint.setColor(Color.RED);
            } else {
                scorePopupPaint.setColor(Color.WHITE);
            }

            // Draw text with fade-out effect
            canvas.drawText((score > 0 ? "+" : "") + score, x, y, scorePopupPaint);

            // Reset alpha to prevent affecting other elements
            scorePopupPaint.setAlpha(255);
        }
    }


    // -------------------------------
    // Inner class for Enemy
    // -------------------------------
    private class Enemy {
        Bitmap bitmap;
        float x, y;
        float speed;
        float dx, dy;
        boolean isTopEnemy;
        float tiltSpeed;
        long lastTiltTime;
        int enemyType;
        int scoreValue;

        public Enemy(float x, float y, float speed, boolean isTopEnemy, int enemyType, int scoreValue) {
            this.x = x;
            this.y = y;
            this.speed = speed;

            this.isTopEnemy = isTopEnemy;
            this.enemyType = enemyType;
            this.scoreValue = scoreValue;
            this.bitmap = isTopEnemy ? enemyTopBitmaps[enemyType] : enemyBottomBitmaps[enemyType];

            float deltaX = targetX - x;
            float deltaY = targetY - y;
            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (distance > 0) {
                dx = (deltaX / distance);
                dy = (deltaY / distance);
            }

            this.tiltSpeed = 0.01f + (speed * 0.002f) + (random.nextFloat() * 0.005f);
            this.lastTiltTime = System.currentTimeMillis();
        }

        public void update() {
            x += dx * speed;
            y += dy * speed;

            // Ensure the enemy doesn't move inside the horizontal margins
            if (x < horizontalMarginPx) {
                x = horizontalMarginPx;
            } else if (x > screenWidth - horizontalMarginPx) {
                x = screenWidth - horizontalMarginPx;
            }
        }

    }
}

