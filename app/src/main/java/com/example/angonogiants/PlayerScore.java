package com.example.angonogiants;

// This class acts as the blueprint for the data sent to Firebase
public class PlayerScore {
    public String playerName;
    public int score;
    public String timePlayed;
    public int wavesSurvived;

    // Required empty constructor for Firebase DataSnapshot.getValue()
    public PlayerScore() {}

    public PlayerScore(String playerName, int score, String timePlayed, int wavesSurvived) {
        this.playerName = playerName;
        this.score = score;
        this.timePlayed = timePlayed;
        this.wavesSurvived = wavesSurvived;
    }
}