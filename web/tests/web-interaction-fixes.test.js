import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { EnemyManager } from "../site/src/enemyManager.js";
import { canvasPoint } from "../site/src/inputCoordinates.js";
import { GIANT_DRAW_SIZE, giantHitRadius } from "../site/src/spriteMetrics.js";

test("far-away taps cannot hit small visible giants", () => {
  const manager = new EnemyManager(360, 800);
  manager.enemies = [{ x: 180, y: 300, type: 1 }];
  assert.equal(manager.hitTest(265, 300, "touch"), null); // 85px away: previously hittable
  assert.equal(manager.enemies.length, 1);
});
test("tap in sprite/touch margin registers immediately", () => {
  const manager = new EnemyManager(360, 800);
  const giant = { x: 180, y: 300, type: 1 };
  manager.enemies = [giant];
  assert.ok(giantHitRadius("touch") > GIANT_DRAW_SIZE / 2);
  assert.ok(giantHitRadius("touch") < 50);
  assert.equal(
    manager.hitTest(180 + GIANT_DRAW_SIZE / 2 + 5, 300, "touch"),
    giant,
  );
  assert.equal(manager.enemies.length, 0);
});
test("regular giant retains priority over overlapping healing giant", () => {
  const manager = new EnemyManager(360, 800);
  const healing = { x: 180, y: 300, type: 6 },
    regular = { x: 182, y: 300, type: 0 };
  manager.enemies = [healing, regular];
  assert.equal(manager.hitTest(180, 300, "touch"), regular);
});
test("touch/mouse tolerance is proportional to the drawn giant", () => {
  assert.ok(giantHitRadius("mouse") < giantHitRadius("touch"));
  const manager = new EnemyManager(360, 800),
    giant = { x: 100, y: 100, type: 0 };
  manager.enemies = [giant];
  assert.equal(manager.hitTest(142, 100, "mouse"), null);
  assert.equal(manager.hitTest(142, 100, "touch"), giant);
});
test("mobile viewport maps screen point using real canvas height (not 800)", () => {
  const rectangle = { left: 20, top: 15, width: 390, height: 844 };
  const hit = canvasPoint(215, 437, rectangle, 360, 779);
  assert.equal(hit.x, 180);
  assert.ok(Math.abs(hit.y - 389.5) < 0.01);
  assert.equal(
    canvasPoint(100, 100, { left: 0, top: 0, width: 0, height: 50 }, 360, 800),
    null,
  );
});
test("main menu prioritizes GIF and leaderboard rows have transparent backgrounds", () => {
  const css = readFileSync("site/styles.css", "utf8");
  const html = readFileSync("site/index.html", "utf8");
  assert.match(html, /rel="preload"[^>]*mainmenubg01\.gif/);
  assert.match(html, /id="menu-screen-bg"[^>]*fetchpriority="high"/);
  assert.match(css, /#leaderboardListView\{margin:0 40px/);
  assert.match(css, /\.leaderboard-entry\{[^}]*background:transparent/);
  assert.match(css, /#loading-video\{[^}]*object-fit:cover/);
});
