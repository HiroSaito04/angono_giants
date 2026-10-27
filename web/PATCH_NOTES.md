# Angono Giants — web corrections

Install the PATCH ZIP by extracting its `site` and `tests` folders directly into `D:\AngonoGiants\web`, allowing replacement of matching files. The patch does not overwrite your Firebase config.

The full ZIP is an alternative installation and includes the previously supplied public Firebase Web configuration. Extract the full ZIP into `D:\AngonoGiants`, where it creates the `web` folder. Always back up your current `web` folder before using the full ZIP.

**Changes:** prioritize `mainmenubg01.gif` before menu controls; transparent leaderboard rows and extra 10px+ horizontal margins; full-viewport loading video; near-sprite giant hit testing; viewport-correct mobile touch coordinates and resize handling; immediate pointerdown input. No Android game, PNG, GIF, MP4, music, score formulas, enemy spawn/movement rates, or game-screen layout redesigns.

**Check locally:**

```powershell
cd D:\AngonoGiants\web
npm test
cd .\site
py -m http.server 8080
```

Open `http://localhost:8080` and refresh with Ctrl+Shift+R after patching.

**Deploy:**

```powershell
cd D:\AngonoGiants\web
firebase deploy --only hosting --project angonogiants
```

Test mobile Chrome and Safari on a real device before treating gameplay as production-ready.
