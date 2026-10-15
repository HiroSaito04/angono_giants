
package com.angonogiants.application;

public class PlayerProfile {

    public String uid;
    public String playerName;
    public String normalizedName;

    // Required for Firebase
    public PlayerProfile() {}

    public PlayerProfile(
            String uid,
            String playerName,
            String normalizedName
    ) {
        this.uid = uid;
        this.playerName = playerName;
        this.normalizedName = normalizedName;
    }
}
