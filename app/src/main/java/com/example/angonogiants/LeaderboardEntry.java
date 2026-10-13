package com.example.angonogiants;

public class LeaderboardEntry {
    public String playerName;
    public int score;
    public String timePlayed;
    public int wavesSurvived;

    // MANDATORY: Empty constructor for Firebase
    public LeaderboardEntry() {}

    public LeaderboardEntry(String playerName, int score, String timePlayed, int wavesSurvived) {
        this.playerName = playerName;
        this.score = score;
        this.timePlayed = timePlayed;
        this.wavesSurvived = wavesSurvived;
    }
}