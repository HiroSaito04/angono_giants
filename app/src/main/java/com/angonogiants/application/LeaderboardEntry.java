//app\src\main\java\com\angonogiants\application\LeaderboardEntry.java
package com.angonogiants.application;

public class LeaderboardEntry {

    public String uid;
    public String playerName;

    public int highestLevel;
    public int score;
    public int wavesSurvived;

    public String timePlayed;

    // Required by Firebase.
    public LeaderboardEntry() {
    }

    public LeaderboardEntry(
            String uid,
            String playerName,
            int highestLevel,
            int score,
            int wavesSurvived,
            String timePlayed
    ) {
        this.uid = uid;
        this.playerName = playerName;
        this.highestLevel = highestLevel;
        this.score = score;
        this.wavesSurvived = wavesSurvived;
        this.timePlayed = timePlayed;
    }
}
