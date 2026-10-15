
package com.angonogiants.application;

public final class LevelConfig {

    private static final int BASE_SECONDS = 300;
    private static final int MAX_SECONDS = 600;

    private LevelConfig() {}

    public static int getDurationSeconds(int level) {
        long extra = Math.max(0L, (long) level - 1L) * 15L;

        return (int) Math.min(
                MAX_SECONDS,
                BASE_SECONDS + extra
        );
    }

    public static long getWaveIntervalMillis(int level) {
        long reduction =
                Math.max(0L, (long) level - 1L) * 30L;

        return Math.max(
                350L,
                1500L - reduction
        );
    }

    public static float getSpeedMultiplier(int level) {
        int safeLevel = Math.max(1, level);

        double logarithmicGrowth =
                0.08 * (Math.log(safeLevel) / Math.log(2.0));

        double advancedGrowth =
                0.04 * Math.log1p(
                        Math.max(0, safeLevel - 3)
                );

        return (float) (
                1.0 + logarithmicGrowth + advancedGrowth
        );
    }

    public static int getNextLevel(int currentLevel) {
        // Prevent integer overflow.
        return currentLevel == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : Math.max(1, currentLevel + 1);
    }
}
