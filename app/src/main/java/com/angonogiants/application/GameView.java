//app\src\main\java\com\angonogiants\application\GameView.java
package com.angonogiants.application;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    public interface OnLifeGivenListener {
        void onLifeGiven();
    }

    private GameStateManager gameState;
    private GameAudioManager audioManager;

    private final EnemyManager enemyManager;
    private final GameScoreManager scoreManager;
    private final GameRenderer renderer;

    private OnLifeGivenListener lifeGivenListener;

    private int targetLives = 3;

    private boolean attached = false;
    private boolean gamePaused = true;
    private boolean gameOver = false;
    private boolean frameScheduled = false;

    private long lastFrameAt = 0L;

    private final Runnable frameTask =
            this::runFrame;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(
            Context context,
            AttributeSet attrs
    ) {
        super(context, attrs);

        renderer = new GameRenderer(context);

        scoreManager = new GameScoreManager(
                score -> {
                    if (getContext() instanceof GameActivity) {
                        ((GameActivity) getContext())
                                .updateScoreCounter(score);
                    }
                }
        );

        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40f,
                getResources().getDisplayMetrics()
        );

        enemyManager = new EnemyManager(
                margin,
                new EnemyManager.Listener() {

                    @Override
                    public void onWaveChanged(int wave) {
                        if (getContext() instanceof GameActivity) {
                            ((GameActivity) getContext())
                                    .updateWaveCounter(wave);
                        }
                    }

                    @Override
                    public void onFirstEnemySpawned() {
                        if (getContext() instanceof GameActivity) {
                            ((GameActivity) getContext())
                                    .startTimerFromGameView();
                        }
                    }

                    @Override
                    public void onEnemyReached(
                            EnemyManager.Enemy enemy
                    ) {
                        handleEnemyReached(enemy);
                    }

                    @Override
                    public void onGrowlNeeded(
                            boolean firstWave,
                            Runnable onComplete
                    ) {
                        if (audioManager != null) {
                            audioManager.playGrowl(
                                    firstWave,
                                    onComplete
                            );
                        } else {
                            onComplete.run();
                        }
                    }
                }
        );

        setClickable(true);
        setFocusable(true);
    }

    // -----------------------------------------
    // Module integration
    // -----------------------------------------

    public void setControllers(
            GameStateManager state,
            GameAudioManager audio
    ) {
        gameState = state;
        audioManager = audio;
    }

    // Starts a new level without resetting run score.
    public void startLevel(int level) {
        if (audioManager != null) {
            audioManager.cancelGrowl();
        }

        gameOver = false;
        gamePaused = true;
        targetLives = 3;

        renderer.setLost(false);
        renderer.setLives(3);

        scoreManager.clearPopups();

        enemyManager.resetForLevel(level);
        enemyManager.setLives(targetLives);
        enemyManager.start();
        enemyManager.pause();

        lastFrameAt = 0L;

        setGameTouchable(false);
        invalidate();
    }

    // -----------------------------------------
    // Animation loop
    // -----------------------------------------

    private void scheduleFrame() {
        if (!attached ||
                frameScheduled ||
                gameOver ||
                gamePaused ||
                gameState == null ||
                !gameState.isPlaying()) {
            return;
        }

        frameScheduled = true;
        postOnAnimation(frameTask);
    }

    private void runFrame() {
        frameScheduled = false;

        if (!attached ||
                gameOver ||
                gamePaused ||
                gameState == null ||
                !gameState.isPlaying()) {
            return;
        }

        long now = SystemClock.uptimeMillis();

        float frameFactor = lastFrameAt == 0L
                ? 1f
                : Math.max(
                        0.25f,
                        Math.min(
                                2.5f,
                                (now - lastFrameAt) / 20f
                        )
                );

        lastFrameAt = now;

        enemyManager.update(now, frameFactor);
        scoreManager.update(now, frameFactor);

        invalidate();
        scheduleFrame();
    }

    // -----------------------------------------
    // Layout and drawing
    // -----------------------------------------

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight
    ) {
        super.onSizeChanged(
                width,
                height,
                oldWidth,
                oldHeight
        );

        renderer.resize(width, height);

        enemyManager.setBounds(
                width,
                height
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        renderer.draw(
                canvas,
                enemyManager.getEnemies(),
                scoreManager.getPopups(),
                SystemClock.uptimeMillis()
        );
    }

    // -----------------------------------------
    // Enemy reaching stall
    // -----------------------------------------

    private void handleEnemyReached(
            EnemyManager.Enemy enemy
    ) {
        if (gameOver || gamePaused) {
            return;
        }

        // Healing giant
        if (enemy.type == 6) {
            if (targetLives < 3) {
                targetLives++;

                renderer.setLives(targetLives);
                enemyManager.setLives(targetLives);

                if (audioManager != null) {
                    audioManager.playGainLife();
                }

                if (lifeGivenListener != null) {
                    lifeGivenListener.onLifeGiven();
                }

                notifyHealthChanged();
            }

            invalidate();
            return;
        }

        // Regular giant damages the stall.
        targetLives = Math.max(
                0,
                targetLives - 1
        );

        renderer.setLives(targetLives);
        enemyManager.setLives(targetLives);

        scoreManager.deductStallLife(
                enemyManager.getTargetX(),
                enemyManager.getTargetY()
        );

        if (audioManager != null) {
            audioManager.playLoseLife();
        }

        notifyHealthChanged();

        if (targetLives == 0) {
            gameOver = true;

            renderer.setLost(true);
            enemyManager.stop();

            if (audioManager != null) {
                audioManager.cancelGrowl();
            }

            if (getContext() instanceof GameActivity) {
                ((GameActivity) getContext())
                        .onGameOver();
            }
        }

        invalidate();
    }

    private void notifyHealthChanged() {
        if (!(getContext() instanceof GameActivity)) {
            return;
        }

        GameActivity activity =
                (GameActivity) getContext();

        if (targetLives == 1) {
            activity.startLowHealthMusic();
        } else {
            activity.stopLowHealthMusic();
        }
    }

    public void setOnLifeGivenListener(
            OnLifeGivenListener listener
    ) {
        lifeGivenListener = listener;
    }

    public void checkAndStopLowHealthMusic() {
        if (targetLives > 1) {
            notifyHealthChanged();
        }
    }

    // -----------------------------------------
    // Touch input
    // -----------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() ||
                gamePaused ||
                gameOver ||
                gameState == null ||
                !gameState.isPlaying()) {
            return false;
        }

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_POINTER_DOWN) {

            int index = event.getActionIndex();

            handleTouch(
                    event.getX(index),
                    event.getY(index)
            );
        }

        return true;
    }

    private void handleTouch(float x, float y) {
        EnemyManager.Enemy enemy =
                enemyManager.hitTest(x, y);

        if (enemy == null) {
            return;
        }

        if (audioManager != null) {
            audioManager.playEnemyHit(enemy.type);
        }

        scoreManager.hitEnemy(
                enemy,
                enemyManager.getTargetX(),
                enemyManager.getTargetY()
        );

        invalidate();
    }

    // -----------------------------------------
    // Pause / Resume
    // -----------------------------------------

    public void pauseGame() {
        if (gameOver || gamePaused) return;

        gamePaused = true;
        enemyManager.pause();

        removeCallbacks(frameTask);
        frameScheduled = false;

        invalidate();
    }

    public void resumeGame() {
        if (gameOver ||
                !gamePaused ||
                gameState == null ||
                !gameState.isPlaying()) {
            return;
        }

        gamePaused = false;
        lastFrameAt = 0L;

        enemyManager.resume();

        setGameTouchable(true);
        invalidate();
        scheduleFrame();
    }

    // -----------------------------------------
    // Game termination
    // -----------------------------------------

    public void stopGame() {
        gameOver = true;
        gamePaused = true;

        enemyManager.stop();
        renderer.setLost(false);

        removeCallbacks(frameTask);
        frameScheduled = false;

        invalidate();
    }

    public void setGameOver(boolean value) {
        gameOver = value;

        if (value) {
            gamePaused = true;

            enemyManager.stop();

            renderer.setLost(
                    targetLives == 0
            );

            removeCallbacks(frameTask);
            frameScheduled = false;
        }

        invalidate();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void clearAllEnemies() {
        enemyManager.clearAll();
        scoreManager.clearPopups();

        invalidate();
    }

    public void setGameTouchable(boolean enabled) {
        setEnabled(enabled);
        setFocusable(enabled);
        setFocusableInTouchMode(enabled);
    }

    // -----------------------------------------
    // Existing API compatibility
    // -----------------------------------------

    public int getTargetLives() {
        return targetLives;
    }

    public int getscoreCount() {
        return scoreManager.getScore();
    }

    public int getDisplayedWave() {
        return enemyManager.getDisplayedWave();
    }

    public int getActualWaveCount() {
        return enemyManager.getActualWaveCount();
    }

    // -----------------------------------------
    // View lifecycle
    // -----------------------------------------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        attached = true;
        lastFrameAt = 0L;

        scheduleFrame();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;

        removeCallbacks(frameTask);
        frameScheduled = false;

        enemyManager.stop();
        renderer.release();

        super.onDetachedFromWindow();
    }
}
