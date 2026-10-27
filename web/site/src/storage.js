// Browser equivalent of Android GuestProgressStore. Guest results remain local.
const KEY = "angono_guest_progress_web_v2";
function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY) || "{}");
  } catch {
    return {};
  }
}
function save(value) {
  try {
    localStorage.setItem(KEY, JSON.stringify(value));
  } catch {}
}
export function guestProgress() {
  const d = load();
  return {
    name: d.name || "Guest",
    highestLevel: Math.max(1, d.highestLevel || 1),
    bestScore: d.bestScore || 0,
    bestWaves: d.bestWaves || 0,
    bestSeconds: d.bestSeconds || 0,
  };
}
export function setGuestName(name) {
  const d = load();
  d.name = (name || "").trim() || "Guest";
  save(d);
}
export function recordGuestResult(unlockedLevel, score, waves, seconds) {
  const d = load();
  d.highestLevel = Math.max(1, d.highestLevel || 1, unlockedLevel);
  if (score > (d.bestScore || 0)) {
    d.bestScore = score;
    d.bestWaves = waves;
    d.bestSeconds = seconds;
  }
  save(d);
}
export function guestBoard() {
  const d = load();
  return d.bestScore
    ? [
        {
          uid: "guest",
          playerName: d.name || "Guest",
          highestLevel: d.highestLevel || 1,
          score: d.bestScore || 0,
          wavesSurvived: d.bestWaves || 0,
          timePlayed: `${d.bestSeconds || 0} seconds`,
        },
      ]
    : [];
}
