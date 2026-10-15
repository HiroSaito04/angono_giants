
package com.angonogiants.application;

public class PlayerScore {

    public String uid;
    public String playerName;

    public int highestLevel;
    public int score;
    public int wavesSurvived;

    public String timePlayed;

    // Required by Firebase.
    public PlayerScore() {
    }

    public PlayerScore(
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
