//app\src\main\java\com\angonogiants\application\GuestProgressStore.java
package com.angonogiants.application;

import android.content.Context;
import android.content.SharedPreferences;

public final class GuestProgressStore {

    private static final String PREFS =
            "angono_guest_progress";

    private static final String KEY_NAME =
            "guest_name";

    private static final String KEY_LEVEL =
            "highest_unlocked_level";

    private static final String KEY_SCORE =
            "best_score";

    private static final String KEY_WAVES =
            "best_run_waves";

    private static final String KEY_SECONDS =
            "best_run_seconds";

    private GuestProgressStore() {
    }

    private static SharedPreferences prefs(
            Context context
    ) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );
    }

    public static String getGuestName(
            Context context
    ) {
        return prefs(context).getString(
                KEY_NAME,
                "Guest"
        );
    }

    public static void setGuestName(
            Context context,
            String name
    ) {
        String safeName = name == null
                ? ""
                : name.trim();

        if (safeName.isEmpty()) {
            safeName = "Guest";
        }

        prefs(context).edit()
                .putString(KEY_NAME, safeName)
                .apply();
    }

    public static int getHighestUnlockedLevel(
            Context context
    ) {
        return Math.max(
                1,
                prefs(context).getInt(KEY_LEVEL, 1)
        );
    }

    public static int getBestScore(
            Context context
    ) {
        return prefs(context).getInt(KEY_SCORE, 0);
    }

    public static int getBestRunWaves(
            Context context
    ) {
        return prefs(context).getInt(KEY_WAVES, 0);
    }

    public static long getBestRunSeconds(
            Context context
    ) {
        return prefs(context).getLong(
                KEY_SECONDS,
                0L
        );
    }

    public static void recordResult(
            Context context,
            int highestUnlockedLevel,
            int score,
            int waves,
            long seconds
    ) {
        SharedPreferences preferences = prefs(context);

        int currentLevel =
                getHighestUnlockedLevel(context);

        int currentBestScore =
                getBestScore(context);

        SharedPreferences.Editor editor =
                preferences.edit();

        editor.putInt(
                KEY_LEVEL,
                Math.max(
                        currentLevel,
                        Math.max(1, highestUnlockedLevel)
                )
        );

        if (score > currentBestScore) {
            editor.putInt(
                    KEY_SCORE,
                    score
            );

            editor.putInt(
                    KEY_WAVES,
                    Math.max(0, waves)
            );

            editor.putLong(
                    KEY_SECONDS,
                    Math.max(0L, seconds)
            );
        }

        editor.apply();
    }
}
