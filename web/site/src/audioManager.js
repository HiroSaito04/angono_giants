// Original GameAudioManager sound map; browser uses compressed MP3 copies of the WAVs.
const AUDIO = "./assets/audio/";
const FX = {
  click: "buttonsound",
  hit0: "case0_3",
  hit1: "case1_4",
  hit2: "case2_5",
  hit6: "case6",
  hurt: "lose_life",
  heal: "gain_life",
  intro: "gamestart",
};
const MUSIC = {
  menu: "mainmenubg",
  leaderboard: "leaderboardmuzic",
  normal: "bgmusic",
  low: "lowhealth",
  winIntro: "gamewonthankzmuzic",
  winResult: "wonfinalzcoremuzic",
  lostIntro: "gameover",
  lostResult: "loztfinalzcoremuzic",
};
export class AudioManager {
  constructor() {
    this.musicName = null;
    this.music = null;
    this.pending = new Set();
    this.active = true;
    this.soundEnabled = true;
  }
  play(name, volume = 1) {
    if (!this.soundEnabled || !this.active || !FX[name]) return;
    const s = new Audio(`${AUDIO}${FX[name]}.mp3`);
    s.volume = volume;
    s.play().catch(() => {});
    return s;
  }
  hit(type) {
    this.play(
      type === 6
        ? "hit6"
        : type === 0 || type === 3
          ? "hit0"
          : type === 1 || type === 4
            ? "hit1"
            : "hit2",
    );
  }
  playGrowl(first, done) {
    const s = new Audio(`${AUDIO}${first ? "giantgrowl" : "wavegrowl"}.mp3`);
    let called = false;
    const end = () => {
      if (called) return;
      called = true;
      this.pending.delete(s);
      done();
    };
    this.pending.add(s);
    s.addEventListener("ended", end, { once: true });
    s.addEventListener("error", end, { once: true });
    if (this.active) s.play().catch(end);
    else end();
    return s;
  }
  musicSet(name) {
    if (this.musicName === name) {
      if (this.active && this.music?.paused) this.music.play().catch(() => {});
      return;
    }
    if (this.music) {
      this.music.pause();
      this.music.currentTime = 0;
    }
    this.musicName = name;
    this.music = null;
    if (!name || !MUSIC[name] || !this.soundEnabled) return;
    const s = new Audio(`${AUDIO}${MUSIC[name]}.mp3`);
    s.volume = 0.65;
    s.loop = [
      "menu",
      "leaderboard",
      "normal",
      "low",
      "winResult",
      "lostResult",
    ].includes(name);
    this.music = s;
    if (this.active) s.play().catch(() => {});
  }
  pause() {
    this.active = false;
    this.music?.pause();
    for (const s of this.pending) s.pause();
  }
  resume() {
    this.active = true;
    if (this.music) this.music.play().catch(() => {});
    for (const s of this.pending) s.play().catch(() => {});
  }
  cancelGrowl() {
    for (const s of this.pending) {
      s.pause();
      s.dispatchEvent(new Event("error"));
    }
    this.pending.clear();
  }
}
