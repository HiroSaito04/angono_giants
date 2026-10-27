import { SPRITE_SCALE, GIANT_DRAW_SIZE } from "./spriteMetrics.js";
// Port of Android GameRenderer.java: same sprite assets, layering and positions.
// Logical canvas 360x800 corresponds to 360dp Android layout. Original res/drawable
// bitmap pixel sizes are scaled to screen CSS-density, then drawn without smoothing.
export class GameRenderer {
  constructor(canvas, images) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d", { alpha: true });
    this.ctx.imageSmoothingEnabled = false;
    this.images = images;
    this.width = canvas.width;
    this.height = canvas.height;
    this.bitmapScale = SPRITE_SCALE;
    this.warningStartedAt = 0;
    this.lastLives = 3;
    this.lost = false;
  }
  reset() {
    this.lost = false;
    this.lastLives = 3;
    this.warningStartedAt = 0;
  }
  setLives(value, now) {
    if (value !== this.lastLives) {
      this.lastLives = value;
      this.warningStartedAt = now;
    }
  }
  image(name, x, y, w, h) {
    const img = this.images[name];
    if (img?.naturalWidth) this.ctx.drawImage(img, x, y, w, h);
  }
  draw(enemies = [], popups = [], lives = 3, now = performance.now()) {
    const c = this.ctx,
      w = this.width,
      h = this.height,
      s = this.bitmapScale;
    c.clearRect(0, 0, w, h);
    c.imageSmoothingEnabled = false;
    if (
      (lives === 2 && now - this.warningStartedAt < 500) ||
      (lives === 1 && Math.floor(now / 500) % 2 === 0)
    )
      this.image("red_warning", 0, 0, w, h);
    for (const e of enemies) if (e.fromTop) this.enemy(e, now);
    const stall = ["stall_destroyed", "stall03", "stall02", "stall01"][
      Math.max(0, Math.min(3, lives))
    ];
    const stallW = 200 * s,
      stallH = 200 * s;
    this.image(stall, w / 2 - stallW / 2, h / 2 - stallH / 2, stallW, stallH);
    for (const e of enemies) if (!e.fromTop) this.enemy(e, now);
    const heart = ["heart_empty", "heart_one", "heart_two", "heart_full"][
      Math.max(0, Math.min(3, lives))
    ];
    this.image(
      heart,
      w / 2 - (160 * s) / 2,
      h / 2 - stallH / 2 - 100 * s,
      160 * s,
      48 * s,
    );
    c.save();
    c.textAlign = "center";
    c.textBaseline = "middle";
    c.font = `${Math.round(60 * s)}px slkscr,monospace`;
    for (const p of popups) {
      c.globalAlpha = Math.max(0, Math.min(1, p.life / p.maxLife));
      c.fillStyle = p.red ? "red" : "white";
      const text = p.points > 0 ? `+${p.points}` : String(p.points);
      c.fillText(text, p.x, p.y);
    }
    c.restore();
    if (this.lost) {
      // Android applies saturation=0 to the GameView on defeat.
      // CSS filter on canvas achieves the same visual result without altering the scene.
      this.canvas.style.filter = "grayscale(1)";
    } else this.canvas.style.filter = "none";
  }
  enemy(e, now) {
    const img =
      this.images[`higantes${e.type + 1}${e.fromTop ? "top" : "bottom"}`];
    if (!img?.naturalWidth) return;
    const c = this.ctx,
      size = GIANT_DRAW_SIZE;
    c.save();
    c.translate(e.x, e.y);
    c.rotate((Math.sin((now - e.tiltOrigin) * e.tiltSpeed) * Math.PI) / 12);
    c.drawImage(img, -size / 2, -size / 2, size, size);
    c.restore();
  }
  clear() {
    this.ctx.clearRect(0, 0, this.width, this.height);
    this.canvas.style.filter = "none";
  }
}
