/** Deadline-free simulation time: paused menus and inactive tabs don't drain time. */
export class GameTimer {
  constructor(durationSeconds, onTick, onFinished) {
    this.durationMs = durationSeconds * 1000;
    this.remainingMs = this.durationMs;
    this.onTick = onTick;
    this.onFinished = onFinished;
    this.started = false;
    this.finished = false;
    this.lastSeconds = -1;
    this.notify();
  }
  start() { if (!this.started && !this.finished) {this.started=true;this.notify();} }
  update(deltaMs) {
    if (!this.started || this.finished) return;
    this.remainingMs = Math.max(0, this.remainingMs - deltaMs);
    this.notify();
    if (this.remainingMs <= 0) {this.finished=true;this.onFinished?.();}
  }
  notify() {
    const s = Math.ceil(this.remainingMs/1000);
    if (s !== this.lastSeconds) {this.lastSeconds=s; this.onTick?.(s);}
  }
  elapsedSeconds() {return Math.floor((this.durationMs - this.remainingMs) / 1000);}
}
export function formatSeconds(seconds) {
  const s=Math.max(0,Math.floor(seconds));
  return `${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
}
