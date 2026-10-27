import { firebaseConfig } from "./firebase-config.js";
// Browser backend remains opt-in until real Firebase Web config + rules exist.
// The Android project remains unchanged. No public write access is granted here.
export class FirebaseService {
  constructor() {
    this.ready = false;
    this.user = null;
    this.auth = null;
    this.db = null;
    this.mods = null;
  }
  async init() {
    if (!firebaseConfig) return false;
    const ver = "11.10.0";
    const app = await import(
      `https://www.gstatic.com/firebasejs/${ver}/firebase-app.js`
    );
    const auth = await import(
      `https://www.gstatic.com/firebasejs/${ver}/firebase-auth.js`
    );
    const db = await import(
      `https://www.gstatic.com/firebasejs/${ver}/firebase-database.js`
    );
    const instance = app.initializeApp(firebaseConfig);
    this.auth = auth.getAuth(instance);
    this.db = db.getDatabase(instance);
    this.mods = { auth, db };
    await new Promise((resolve) =>
      auth.onAuthStateChanged(
        this.auth,
        (user) => {
          this.user = user;
          resolve();
        },
        () => resolve(),
      ),
    );
    this.ready = true;
    return true;
  }
  async signIn(email, password) {
    if (!this.ready)
      throw Error("Firebase Web configuration is missing. See README.md.");
    const c = await this.mods.auth.signInWithEmailAndPassword(
      this.auth,
      email,
      password,
    );
    this.user = c.user;
    return c.user;
  }
  async signUp(email, password) {
    if (!this.ready)
      throw Error("Firebase Web configuration is missing. See README.md.");
    const c = await this.mods.auth.createUserWithEmailAndPassword(
      this.auth,
      email,
      password,
    );
    this.user = c.user;
    return c.user;
  }
  async signOut() {
    if (!this.ready) return;
    await this.mods.auth.signOut(this.auth);
    this.user = null;
  }
  async resetPassword(email) {
    if (!this.ready)
      throw Error("Firebase Web configuration is missing. See README.md.");
    await this.mods.auth.sendPasswordResetEmail(this.auth, email);
  }
  async readProgress() {
    if (!this.user) return null;
    const { ref, get } = this.mods.db;
    return (await get(ref(this.db, `playerProgress/${this.user.uid}`))).val();
  }
  async readProfile() {
    if (!this.user) return null;
    const { ref, get } = this.mods.db;
    const uid = this.user.uid;
    // Android MainActivity uses /playerProfiles/<uid> profile records.
    return (await get(ref(this.db, `playerProfiles/${uid}`))).val();
  }
  async setProfile(playerName) {
    if (!this.user) return;
    const { ref, update } = this.mods.db;
    await update(ref(this.db, `playerProfiles/${this.user.uid}`), {
      uid: this.user.uid,
      playerName,
      normalizedName: playerName.toLowerCase(),
    });
  }
  async readLeaderboard() {
    if (!this.ready) return null;
    const { ref, get } = this.mods.db;
    const value = (await get(ref(this.db, "leaderboard"))).val() || {};
    return Object.values(value);
  }
  async saveResult(name, unlockedLevel, score, waves, seconds) {
    if (!this.user) return;
    const { ref, runTransaction } = this.mods.db;
    const uid = this.user.uid;
    await runTransaction(ref(this.db, `playerProgress/${uid}`), (prev) => ({
      ...prev,
      highestUnlockedLevel: Math.max(
        prev?.highestUnlockedLevel || 1,
        unlockedLevel,
      ),
      bestScore: Math.max(prev?.bestScore || 0, score),
    }));
    await runTransaction(ref(this.db, `leaderboard/${uid}`), (prev) => {
      const row = {
        uid,
        playerName: name,
        highestLevel: Math.max(prev?.highestLevel || 1, unlockedLevel),
        score: prev?.score || 0,
        wavesSurvived: prev?.wavesSurvived || 0,
        timePlayed: prev?.timePlayed || "",
      };
      if (!prev || score > row.score) {
        row.score = score;
        row.wavesSurvived = waves;
        row.timePlayed = `${seconds} seconds`;
      }
      return row;
    });
  }
}
