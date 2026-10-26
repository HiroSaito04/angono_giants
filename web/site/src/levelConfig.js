/** Same progression formulas as Android LevelConfig.java. */
export const LevelConfig = {
  durationSeconds(level) { return Math.min(600, 30 + (Math.max(1, level) - 1) * 15); },
  waveIntervalMs(level) { return Math.max(450, 3200 - (Math.max(1, level) - 1) * 120); },
  speedMultiplier(level) {
    const safe = Math.max(1, level);
    return safe <= 10 ? 0.60 + (safe - 1) * 0.04 : 0.96 + 0.09 * Math.log1p(safe - 10);
  },
  maxEnemiesPerWave(level) { return Math.min(9, 2 + Math.floor((Math.max(1, level) - 1) / 2)); },
  nextLevel(level) { return Math.min(2147483647, Math.max(1, level + 1)); },
};
