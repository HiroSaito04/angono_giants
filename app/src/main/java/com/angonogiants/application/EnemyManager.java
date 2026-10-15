
package com.angonogiants.application;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class EnemyManager {

    public interface Listener {
        void onWaveChanged(int wave);
        void onFirstEnemySpawned();
        void onEnemyReached(Enemy enemy);
        void onGrowlNeeded(
                boolean firstWave,
                Runnable onComplete
        );
    }

    public static final class Enemy {

        public final int type;
        public final boolean fromTop;

        public final float speed;
        public final float tiltSpeed;
        public final long tiltOrigin;

        public float x;
        public float y;

        private final float dx;
        private final float dy;

        Enemy(
                float x,
                float y,
                float speed,
                int type,
                boolean fromTop,
                float targetX,
                float targetY,
                Random random
        ) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.type = type;
            this.fromTop = fromTop;

            float deltaX = targetX - x;
            float deltaY = targetY - y;

            float distance = (float) Math.sqrt(
                    deltaX * deltaX +
                    deltaY * deltaY
            );

            dx = distance > 0f
                    ? deltaX / distance
                    : 0f;

            dy = distance > 0f
                    ? deltaY / distance
                    : 0f;

            tiltSpeed = 0.01f
                    + speed * 0.002f
                    + random.nextFloat() * 0.005f;

            tiltOrigin = SystemClock.uptimeMillis();
        }

        void update(
                float frameFactor,
                int width,
                int horizontalMargin
        ) {
            x += dx * speed * frameFactor;
            y += dy * speed * frameFactor;

            int safeMargin = Math.min(
                    horizontalMargin,
                    width / 2
            );

            x = Math.max(
                    safeMargin,
                    Math.min(
                            width - safeMargin,
                            x
                    )
            );
        }
    }

    private enum WaveStage {
        READY,
        WAITING_GROWL,
        SPAWNING
    }

    private static final long FIRST_WAVE_DELAY = 1500L;
    private static final long WAVE_START_DELAY = 1000L;
    private static final long ENEMY_SPAWN_DELAY = 200L;
    private static final long HEAL_COOLDOWN = 10000L;

    private static final int MAX_ENEMIES_PER_WAVE = 9;
    private static final int MIN_ENEMY_DISTANCE = 150;

    private final Random random = new Random();
    private final List<Enemy> enemies =
            new ArrayList<>();

    private final int horizontalMargin;
    private final Listener listener;

    private WaveStage stage = WaveStage.READY;

    private int width;
    private int height;

    private float targetX;
    private float targetY;

    private int lives = 3;
    private int levelNumber = 1;
    private int waveNumber = 0;
    private int displayedWave = 1;

    private int pendingEnemies = 0;

    private long waveInterval = 1500L;
    private float speedMultiplier = 1f;

    private long nextWaveAt;
    private long nextEnemyAt;
    private long lastHealSpawnAt;
    private long pausedAt;

    private long generation = 0L;

    private boolean started = false;
    private boolean paused = false;
    private boolean stopped = false;

    private boolean firstEnemySpawned = false;
    private boolean healingEnemySpawned = false;

    public EnemyManager(
            int horizontalMargin,
            Listener listener
    ) {
        this.horizontalMargin = horizontalMargin;
        this.listener = listener;
    }

    // -----------------------------------------
    // Level configuration
    // -----------------------------------------

    public void resetForLevel(int level) {
        generation++;

        levelNumber = Math.max(1, level);

        waveInterval =
                LevelConfig.getWaveIntervalMillis(
                        levelNumber
                );

        speedMultiplier =
                LevelConfig.getSpeedMultiplier(
                        levelNumber
                );

        enemies.clear();

        lives = 3;
        waveNumber = 0;
        displayedWave = 1;
        pendingEnemies = 0;

        nextWaveAt = 0L;
        nextEnemyAt = 0L;
        lastHealSpawnAt = 0L;
        pausedAt = 0L;

        firstEnemySpawned = false;
        healingEnemySpawned = false;

        started = false;
        paused = false;
        stopped = false;

        stage = WaveStage.READY;

        if (listener != null) {
            listener.onWaveChanged(displayedWave);
        }
    }

    public void setBounds(int width, int height) {
        this.width = width;
        this.height = height;

        targetX = width / 2f;
        targetY = height / 2f;
    }

    public void setLives(int value) {
        lives = Math.max(0, Math.min(3, value));
    }

    // -----------------------------------------
    // Lifecycle
    // -----------------------------------------

    public void start() {
        if (started || stopped) return;

        started = true;

        nextWaveAt =
                SystemClock.uptimeMillis() +
                FIRST_WAVE_DELAY;
    }

    public void pause() {
        if (paused || stopped) return;

        paused = true;
        pausedAt = SystemClock.uptimeMillis();
    }

    public void resume() {
        if (!paused || stopped) return;

        long duration =
                SystemClock.uptimeMillis() - pausedAt;

        if (nextWaveAt > 0L) {
            nextWaveAt += duration;
        }

        if (nextEnemyAt > 0L) {
            nextEnemyAt += duration;
        }

        if (lastHealSpawnAt > 0L) {
            lastHealSpawnAt += duration;
        }

        paused = false;
        pausedAt = 0L;
    }

    public void stop() {
        stopped = true;
        pendingEnemies = 0;

        // Invalidate callbacks from a previous wave.
        generation++;
    }

    public void clearAll() {
        enemies.clear();
    }

    // -----------------------------------------
    // Game update
    // -----------------------------------------

    public void update(long now, float frameFactor) {
        if (!started ||
                paused ||
                stopped ||
                width <= 0 ||
                height <= 0) {
            return;
        }

        if (stage == WaveStage.READY &&
                now >= nextWaveAt) {
            beginWave();
        }

        if (stage == WaveStage.SPAWNING &&
                pendingEnemies > 0 &&
                now >= nextEnemyAt) {

            spawnEnemy(now);

            pendingEnemies--;
            nextEnemyAt = now + ENEMY_SPAWN_DELAY;

            if (pendingEnemies == 0) {
                stage = WaveStage.READY;
                nextWaveAt = now + waveInterval;
            }
        }

        Iterator<Enemy> iterator =
                enemies.iterator();

        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();

            enemy.update(
                    frameFactor,
                    width,
                    horizontalMargin
            );

            float dx = enemy.x - targetX;
            float dy = enemy.y - targetY;

            if ((dx * dx + dy * dy) <
                    (100f * 100f)) {

                iterator.remove();

                if (listener != null) {
                    listener.onEnemyReached(enemy);
                }

                if (stopped) {
                    break;
                }
            }
        }
    }

    // -----------------------------------------
    // Wave generation
    // -----------------------------------------

    private void beginWave() {
        waveNumber++;

        int newDisplayedWave =
                1 + (waveNumber - 1) / 5;

        if (newDisplayedWave != displayedWave) {
            displayedWave = newDisplayedWave;

            if (listener != null) {
                listener.onWaveChanged(
                        displayedWave
                );
            }
        }

        healingEnemySpawned = false;

        boolean firstWave = waveNumber == 1;

        boolean needsGrowl =
                firstWave ||
                (waveNumber > 1 && waveNumber % 5 == 0);

        if (needsGrowl && listener != null) {
            stage = WaveStage.WAITING_GROWL;

            final long callbackGeneration =
                    generation;

            listener.onGrowlNeeded(
                    firstWave,
                    () -> {
                        if (!stopped
                                && generation ==
                                        callbackGeneration
                                && stage ==
                                        WaveStage.WAITING_GROWL) {

                            prepareWaveSpawns();
                        }
                    }
            );
        } else {
            prepareWaveSpawns();
        }
    }

    private void prepareWaveSpawns() {
        if (stopped) return;

        pendingEnemies = Math.min(
                1 + waveNumber / 5,
                MAX_ENEMIES_PER_WAVE
        );

        nextEnemyAt =
                SystemClock.uptimeMillis() +
                WAVE_START_DELAY;

        stage = WaveStage.SPAWNING;
    }

    // -----------------------------------------
    // Enemy creation
    // -----------------------------------------

    private void spawnEnemy(long now) {
        int x = chooseX();
        boolean fromTop = random.nextBoolean();

        int y = fromTop ? 0 : height;

        for (int attempt = 0; attempt < 10; attempt++) {
            int candidate = chooseX();

            if (validPosition(candidate, y)) {
                x = candidate;
                break;
            }
        }

        int type = random.nextInt(6);

        enemies.add(
                createEnemy(x, y, type, fromTop)
        );

        if (!firstEnemySpawned) {
            firstEnemySpawned = true;

            if (listener != null) {
                listener.onFirstEnemySpawned();
            }
        }

        if (lives < 3
                && !healingEnemySpawned
                && now - lastHealSpawnAt >= HEAL_COOLDOWN
                && random.nextInt(3) == 0) {

            trySpawnHealingEnemy(now, fromTop);
        }
    }

    private void trySpawnHealingEnemy(
            long now,
            boolean fromTop
    ) {
        int y = fromTop ? 0 : height;

        for (int attempt = 0; attempt < 10; attempt++) {
            int x = chooseX();

            if (validPosition(x, y)) {
                enemies.add(
                        createEnemy(
                                x,
                                y,
                                6,
                                fromTop
                        )
                );

                healingEnemySpawned = true;
                lastHealSpawnAt = now;
                return;
            }
        }
    }

    private Enemy createEnemy(
            int x,
            int y,
            int type,
            boolean fromTop
    ) {
        return new Enemy(
                x,
                y,
                calculateSpeed(type),
                type,
                fromTop,
                targetX,
                targetY,
                random
        );
    }

    private float calculateSpeed(int type) {
        float[] baseSpeeds = {
                3f, 3.5f, 4f,
                3f, 3.5f, 4f,
                3f
        };

        float waveIncrease =
                ((waveNumber - 1) / 5) * 0.5f;

        float originalSpeed = Math.min(
                baseSpeeds[type] + waveIncrease,
                11f
        );

        return originalSpeed * speedMultiplier;
    }

    private int chooseX() {
        int margin = Math.min(50, width / 2);

        int range = Math.max(
                1,
                width - margin * 2 + 1
        );

        return margin + random.nextInt(range);
    }

    private boolean validPosition(
            float x,
            float y
    ) {
        for (Enemy enemy : enemies) {
            float dx = enemy.x - x;
            float dy = enemy.y - y;

            if (dx * dx + dy * dy <
                    MIN_ENEMY_DISTANCE *
                    MIN_ENEMY_DISTANCE) {

                return false;
            }
        }

        return true;
    }

    // -----------------------------------------
    // Touch / hit detection
    // -----------------------------------------

    public Enemy hitTest(float x, float y) {
        if (paused || stopped) {
            return null;
        }

        Enemy closestRegular = null;
        Enemy closestHealing = null;

        float regularDistance = Float.MAX_VALUE;
        float healingDistance = Float.MAX_VALUE;

        for (Enemy enemy : enemies) {
            float dx = enemy.x - x;
            float dy = enemy.y - y;

            float distance = dx * dx + dy * dy;

            if (distance >= 140f * 140f) {
                continue;
            }

            if (enemy.type == 6) {
                if (distance < healingDistance) {
                    closestHealing = enemy;
                    healingDistance = distance;
                }
            } else {
                if (distance < regularDistance) {
                    closestRegular = enemy;
                    regularDistance = distance;
                }
            }
        }

        Enemy selected = closestRegular != null
                ? closestRegular
                : closestHealing;

        if (selected != null) {
            enemies.remove(selected);
        }

        return selected;
    }

    // -----------------------------------------
    // Getters
    // -----------------------------------------

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public int getDisplayedWave() {
        return displayedWave;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }
    
    public int getActualWaveCount() {
        return waveNumber;
    }

}
