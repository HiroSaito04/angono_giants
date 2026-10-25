//app\src\main\java\com\angonogiants\application\LevelConfig.java
package com.angonogiants.application;

public final class LevelConfig {

    private static final int BASE_DURATION_SECONDS = 30;
    private static final int MAX_DURATION_SECONDS = 600;

    private static final long BASE_WAVE_INTERVAL = 3200L;
    private static final long MIN_WAVE_INTERVAL = 450L;

    private LevelConfig() {
        // Utility class
    }

    // -------------------------------------
    // Level duration
    // -------------------------------------

    public static int getDurationSeconds(int level) {
        int safeLevel = Math.max(1, level);

        long seconds = BASE_DURATION_SECONDS
                + ((long) safeLevel - 1L) * 15L;

        return (int) Math.min(
                MAX_DURATION_SECONDS,
                seconds
        );
    }

    // -------------------------------------
    // Time between waves
    // -------------------------------------

    public static long getWaveIntervalMillis(int level) {
        int safeLevel = Math.max(1, level);

        long reduction =
                ((long) safeLevel - 1L) * 120L;

        return Math.max(
                MIN_WAVE_INTERVAL,
                BASE_WAVE_INTERVAL - reduction
        );
    }

    // -------------------------------------
    // Enemy speed
    // -------------------------------------

    public static float getSpeedMultiplier(int level) {
        int safeLevel = Math.max(1, level);

        // Beginner levels: noticeably slower.
        if (safeLevel <= 10) {
            return 0.60f
                    + (safeLevel - 1) * 0.04f;
        }

        // Advanced levels: difficulty grows
        // gradually without a fixed level limit.
        double extraSpeed =
                0.09 * Math.log1p(safeLevel - 10);

        return (float) (0.96 + extraSpeed);
    }

    // -------------------------------------
    // Maximum giants in one wave
    // -------------------------------------

    public static int getMaxEnemiesPerWave(int level) {
        int safeLevel = Math.max(1, level);

        return Math.min(
                9,
                2 + (safeLevel - 1) / 2
        );
    }

    // -------------------------------------
    // Infinite-level progression
    // -------------------------------------

    public static int getNextLevel(int currentLevel) {
        if (currentLevel == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return Math.max(1, currentLevel + 1);
    }
}
