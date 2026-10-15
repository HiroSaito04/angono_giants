
package com.angonogiants.application;

public class PlayerProgress {

    public int highestUnlockedLevel = 1;
    public int bestScore = 0;

    // Required for Firebase
    public PlayerProgress() {}

    public PlayerProgress(
            int highestUnlockedLevel,
            int bestScore
    ) {
        this.highestUnlockedLevel =
                highestUnlockedLevel;
        this.bestScore = bestScore;
    }
}
