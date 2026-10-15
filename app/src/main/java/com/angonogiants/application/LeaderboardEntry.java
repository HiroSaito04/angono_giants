
package com.angonogiants.application;

public class LeaderboardEntry extends PlayerScore {

    // Required for Firebase compatibility.
    public LeaderboardEntry() {
        super();
    }

    public LeaderboardEntry(PlayerScore score) {
        super(
                score.uid,
                score.playerName,
                score.highestLevel,
                score.score,
                score.wavesSurvived,
                score.timePlayed
        );
    }
}
