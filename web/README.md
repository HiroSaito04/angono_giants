# Angono Giants — Android-faithful browser port

This is a **separate HTML5/JavaScript web port of the Android project supplied in `src.zip`**, not an Android Studio HTML source listing. It was rebuilt from the original Java and XML rules. Your Android project is **not** modified. Unzip this project as `D:\AngonoGiants\web` (or another folder outside `app`).

## Exactly what is preserved

- **Splash** (`splashart.png`, 1-second delay), then **original MainActivity menu** (`mainmenubg01.gif`), Sign In button, leaderboard icon, About Us icon, original guest-name input and Start button positions.
- **Original loading video** (`loadingpagevideo.mp4`) with the Skip control, before entering GameActivity.
- **Original game layering**: animated `tile_layer.gif`; the center-aligned Android stall, giants, hearts and score popups; animated `tree_layer.gif`; timer/score/wave/pause HUD; victory/defeat overlays; **transparent** `gametransition3.gif` as the uppermost layer.
- The intro's original 25 frames take **1,510 ms**. The black curtain is hidden at **1,070 ms** (the midpoint frame boundary); the sprites are prepared behind it, and gameplay starts only when the GIF finishes.
- **Game rules** ported from `EnemyManager.java`, `GameRenderer.java`, `GameScoreManager.java`, `LevelConfig.java`, `GameTimer.java`, and `GameView.java`: waves, growls, min spawn spacing, regular/healing giant hit priority, speed/difficulty, stall life damage and -1,000 penalty, regular hit score with location bonus/random modifier, health repair giant, countdown starting **with the first spawned giant**, pause/resume and level continuation.
- **Same results flow**: five-second thank-you/defeat intro, aligned player/level/time/score/waves, original victory/defeat graphic, image-based **DEFEND AGAIN** / **MAIN MENU** buttons. Victory carries the score into the next unlocked level; defeat retries the same level through the loading screen.
- Guest name, highest unlocked level and best run stored in browser localStorage, matching Android GuestProgressStore best-score policy.
- About Us page with text from `aboutus.xml`, and leaderboard screen with the original background, headline, medal images and layout.
- Source Java/XML copies under `reference/` for review (not executed by the browser).

## Setup and run on Windows

1. Extract the ZIP so that `D:\AngonoGiants\web\site\index.html` exists.
2. Ensure Python 3 is installed.
3. Open PowerShell:

   ```powershell
   cd D:\AngonoGiants\web
   .\START-WINDOWS.ps1
   ```

   Or:

   ```powershell
   cd D:\AngonoGiants\web\site
   py -m http.server 8080
   ```

4. Open http://localhost:8080/ in a browser. **Do not open `site/index.html` directly via `file://`**: JavaScript modules require a local HTTP server.
5. Click START, let the original loading video complete (or press Skip), then play normally.

## Android / web structure

```text
D:\AngonoGiants\
├── app\                          original Android game (UNCHANGED)
└── web\                          extracted project from this ZIP
    ├── README.md
    ├── START-WINDOWS.ps1
    ├── package.json
    ├── firebase.json
    ├── reference\                copied original Android Java/XML for parity
    ├── tests\                    automated rule/asset tests
    └── site\                     site root for browser and hosting
        ├── index.html            original Activity screens and view IDs
        ├── styles.css            Android XML-inspired placement and layering
        ├── assets\
        │   ├── images\          UNMODIFIED original PNG/GIF files
        │   ├── audio\           same source sound effects/music, browser MP3
        │   ├── video\           UNMODIFIED original loadingpagevideo.mp4
        │   ├── start.ttf
        │   └── slkscr.ttf
        └── src\
            ├── main.js           screen flow / wiring
            ├── renderer.js       original sprites and draw order
            ├── enemyManager.js   waves, spawn, hit priority and movement
            ├── scoreManager.js   scoring and popup values
            ├── gameTimer.js      30-second level 1, first-spawn start
            ├── levelConfig.js    same level difficulty/time formulas
            ├── audioManager.js   Android sound map
            ├── storage.js        browser guest progress
            ├── firebase.js       optional existing Android cloud backend
            └── firebase-config.js
```

## Firebase Web authentication and global leaderboard

**Firebase authentication, remote progress and the shared online leaderboard require one manual configuration step.** The user's Android Firebase Web configuration (`apiKey`, `authDomain`, `projectId`, `appId`) was not present in the uploaded `src.zip`, so it is not possible to connect the backend automatically. The game works as a **guest** without Firebase; signed-in cloud functionality becomes available only after configuration.

1. Open Firebase console → your **existing Angono Giants Firebase project** → Project settings → Your apps → Add a **Web** app (if one does not exist).
2. Copy the JavaScript Firebase `firebaseConfig` object into `site/src/firebase-config.js`, replacing `null`. Preserve its existing `databaseURL` field, which should be `https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app`.
3. Enable/configure Email/Password Authentication and add your Hosting/domain to **authorized domains**.
4. Check Firebase Realtime Database security rules; **do not make the entire `playerProgress`, `playerProfiles`, `usernames` or `leaderboard` directories publicly writable.** Client-side score manipulation is possible in any browser game, so use a trusted backend for competitive leaderboards. The included code attempts user-scoped RTDB transactions and has no rule bypass.
5. Test existing sign-in, registration/username reservations, leaderboard read, and cloud-save permissions with a test account. The Android app's database paths are `playerProfiles`, `playerProgress`, `usernames`, and `leaderboard`.

Without that configuration, Sign In displays a configuration error and the leaderboard shows the original empty-state message; **it does not pretend guest scores are in the online leaderboard**.

## Deploying to Firebase Hosting

From `web/` after `firebase login` and project selection:

```powershell
firebase deploy --only hosting --project YOUR_FIREBASE_PROJECT_ID
```

The `firebase.json` public directory is `site/`.

## Automated validation

With Node.js 18+ installed:

```powershell
cd D:\AngonoGiants\web
npm test
```

The tests validate level formulas, timer start behavior, first wave timings, regular-vs-healing hit priority, score penalties, original art presence, and original game-view IDs. The JavaScript source also passes `node --check`.

## Honest browser limitations

- This is a **manual port**, not a byte-for-byte execution of Android's Java classes. The original PNG/GIF artwork, screen assets, layout IDs, level settings and principal mechanics are retained. Rendering scale, text rasterization, animated GIF scheduling and audio playback can vary slightly by browser, screen density and device. Original Android source references are bundled to facilitate side-by-side checks.
- Original WAV recordings are encoded as MP3 for web file size/browser compatibility. **The original Android files are not changed.** PNG, GIF and loading MP4 files are copied unchanged.
- A browser ordinarily cannot close an arbitrary tab when the original Android EXIT button is used. The web version tries to close the tab and otherwise returns to the original main menu.
- Audio/video autoplay and background-tab suspension are subject to browser policies. The browser pauses play when the tab loses focus.
- Real-world mobile testing and complete Firebase integration are still required before production. These are not claimed to have been completed. No cloud accounts or data were altered during packaging.
