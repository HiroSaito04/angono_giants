import { LevelConfig } from "./levelConfig.js";
import { SPRITE_SCALE, giantHitRadius } from "./spriteMetrics.js";
// Faithful translation of Android EnemyManager: level-dependent wave limits,
// growl-then-1-second spawn, one enemy per 200ms, no score/reset changes.
const S = SPRITE_SCALE,
  BASE_SPEED = [3, 3.5, 4, 3, 3.5, 4, 3];
export class EnemyManager {
  constructor(width, height, callbacks = {}) {
    this.width = width;
    this.height = height;
    this.callbacks = callbacks;
    this.targetX = width / 2;
    this.targetY = height / 2;
    this.generation = 0;
    this.resetForLevel(1);
  }
  resetForLevel(level) {
    this.generation++;
    this.level = Math.max(1, level);
    this.waveInterval = LevelConfig.waveIntervalMs(level);
    this.speedMultiplier = LevelConfig.speedMultiplier(level);
    this.enemies = [];
    this.lives = 3;
    this.waveNumber = 0;
    this.displayedWave = 1;
    this.pendingEnemies = 0;
    this.nextWaveAt = 1500;
    this.nextEnemyAt = 0;
    this.lastHealSpawnAt = 0;
    this.stage = "ready";
    this.time = 0;
    this.healingEnemySpawned = false;
    this.firstEnemySpawned = false;
    this.stopped = false;
    this.callbacks.onWaveChanged?.(1);
  }
  update(delta) {
    if (this.stopped) return;
    this.time += delta;
    if (this.stage === "ready" && this.time >= this.nextWaveAt)
      this.beginWave();
    if (
      this.stage === "spawning" &&
      this.pendingEnemies > 0 &&
      this.time >= this.nextEnemyAt
    ) {
      this.spawnEnemy();
      this.pendingEnemies--;
      this.nextEnemyAt = this.time + 200;
      if (this.pendingEnemies === 0) {
        this.stage = "ready";
        this.nextWaveAt = this.time + this.waveInterval;
      }
    }
    const frameFactor = Math.max(0.25, Math.min(2.5, delta / 20));
    for (let i = this.enemies.length - 1; i >= 0; i--) {
      const e = this.enemies[i];
      e.x = Math.max(
        40 * S,
        Math.min(this.width - 40 * S, e.x + e.dx * e.speed * frameFactor * S),
      );
      e.y += e.dy * e.speed * frameFactor * S;
      if (Math.hypot(e.x - this.targetX, e.y - this.targetY) < 100 * S) {
        this.enemies.splice(i, 1);
        this.callbacks.onEnemyReached?.(e);
        if (this.stopped) break;
      }
    }
  }
  beginWave() {
    this.waveNumber++;
    const displayed = 1 + Math.floor((this.waveNumber - 1) / 5);
    if (displayed !== this.displayedWave) {
      this.displayedWave = displayed;
      this.callbacks.onWaveChanged?.(displayed);
    }
    this.healingEnemySpawned = false;
    const first = this.waveNumber === 1;
    const needsGrowl =
      first || (this.waveNumber > 1 && this.waveNumber % 5 === 0);
    if (needsGrowl) {
      this.stage = "waiting-growl";
      const gen = this.generation;
      this.callbacks.onGrowlNeeded?.(first, () => {
        if (
          !this.stopped &&
          this.generation === gen &&
          this.stage === "waiting-growl"
        )
          this.prepareWaveSpawns();
      });
    } else this.prepareWaveSpawns();
  }
  prepareWaveSpawns() {
    if (this.stopped) return;
    this.pendingEnemies = Math.min(
      1 + Math.floor(this.waveNumber / 5),
      LevelConfig.maxEnemiesPerWave(this.level),
    );
    this.nextEnemyAt = this.time + 1000;
    this.stage = "spawning";
  }
  spawnEnemy() {
    const fromTop = Math.random() < 0.5;
    const y = fromTop ? 0 : this.height;
    let x = this.randomX();
    for (let i = 0; i < 10; i++) {
      const candidate = this.randomX();
      if (this.positionValid(candidate, y)) {
        x = candidate;
        break;
      }
    }
    this.enemies.push(
      this.makeEnemy(x, y, Math.floor(Math.random() * 6), fromTop),
    );
    if (!this.firstEnemySpawned) {
      this.firstEnemySpawned = true;
      this.callbacks.onFirstEnemySpawned?.();
    }
    if (
      this.lives < 3 &&
      !this.healingEnemySpawned &&
      this.time - this.lastHealSpawnAt >= 10000 &&
      Math.floor(Math.random() * 3) === 0
    ) {
      for (let i = 0; i < 10; i++) {
        const healX = this.randomX();
        if (this.positionValid(healX, y)) {
          this.enemies.push(this.makeEnemy(healX, y, 6, fromTop));
          this.healingEnemySpawned = true;
          this.lastHealSpawnAt = this.time;
          break;
        }
      }
    }
  }
  randomX() {
    const margin = Math.min(50 * S, this.width / 2);
    return margin + Math.random() * Math.max(1, this.width - margin * 2);
  }
  positionValid(x, y) {
    return this.enemies.every(
      (e) => (e.x - x) ** 2 + (e.y - y) ** 2 >= (150 * S) ** 2,
    );
  }
  makeEnemy(x, y, type, fromTop) {
    const vx = this.targetX - x,
      vy = this.targetY - y,
      dist = Math.hypot(vx, vy) || 1;
    const speed =
      Math.min(
        BASE_SPEED[type] + Math.floor((this.waveNumber - 1) / 5) * 0.5,
        11,
      ) * this.speedMultiplier;
    return {
      x,
      y,
      type,
      fromTop,
      dx: vx / dist,
      dy: vy / dist,
      speed,
      tiltSpeed: 0.01 + speed * 0.002 + Math.random() * 0.005,
      tiltOrigin: performance.now(),
    };
  }
  hitTest(x, y, pointerType = "touch") {
    // Android used an extremely wide 140px radius; after web bitmap scaling
    // that still reached giants far outside their visible sprites.  Check
    // against the actual rendered giant size with a modest touch allowance.
    const radiusSquared = giantHitRadius(pointerType) ** 2;
    let regular = null,
      healing = null,
      regularDist = Infinity,
      healingDist = Infinity;
    for (const e of this.enemies) {
      const d = (e.x - x) ** 2 + (e.y - y) ** 2;
      if (d > radiusSquared) continue;
      if (e.type === 6) {
        if (d < healingDist) {
          healing = e;
          healingDist = d;
        }
      } else if (d < regularDist) {
        regular = e;
        regularDist = d;
      }
    }
    const target = regular ?? healing;
    if (target) this.enemies.splice(this.enemies.indexOf(target), 1);
    return target ?? null;
  }
  getActualWaveCount() {
    return this.waveNumber;
  }
  stop() {
    this.stopped = true;
    this.enemies = [];
    this.generation++;
  }
}
