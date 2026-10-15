
package com.angonogiants.application;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public final class LeaderboardRepository {

    private static final String DATABASE_URL =
            "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final DatabaseReference leaderboardReference;

    public LeaderboardRepository() {
        leaderboardReference = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("leaderboard");
    }

    public DatabaseReference getLeaderboardReference() {
        return leaderboardReference;
    }

    public Task<Void> saveScore(
            String playerName,
            int highestLevel,
            int score,
            int wavesSurvived,
            String timePlayed
    ) {
        FirebaseUser user = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (user == null) {
            return Tasks.forException(
                    new IllegalStateException(
                            "Player is not signed in."
                    )
            );
        }

        if (playerName == null
                || playerName.trim().isEmpty()) {
            return Tasks.forException(
                    new IllegalArgumentException(
                            "Player name is required."
                    )
            );
        }

        String uid = user.getUid();

        PlayerScore incoming = new PlayerScore(
                uid,
                playerName.trim(),
                Math.max(1, highestLevel),
                score,
                Math.max(0, wavesSurvived),
                timePlayed == null ? "" : timePlayed
        );

        TaskCompletionSource<Void> result =
                new TaskCompletionSource<>();

        leaderboardReference
                .child(uid)
                .runTransaction(
                    new Transaction.Handler() {

                        @Override
                        public Transaction.Result doTransaction(
                                MutableData current
                        ) {
                            PlayerScore previous =
                                    current.getValue(
                                            PlayerScore.class
                                    );

                            if (previous == null) {
                                current.setValue(incoming);

                                return Transaction.success(
                                        current
                                );
                            }

                            // Update highest unlocked level.
                            previous.highestLevel = Math.max(
                                    Math.max(
                                            1,
                                            previous.highestLevel
                                    ),
                                    incoming.highestLevel
                            );

                            // Only replace the best-score run
                            // if the new score is higher.
                            if (incoming.score > previous.score) {
                                previous.score = incoming.score;

                                previous.wavesSurvived =
                                        incoming.wavesSurvived;

                                previous.timePlayed =
                                        incoming.timePlayed;
                            }

                            previous.uid = uid;
                            previous.playerName =
                                    incoming.playerName;

                            current.setValue(previous);

                            return Transaction.success(current);
                        }

                        @Override
                        public void onComplete(
                                DatabaseError error,
                                boolean committed,
                                DataSnapshot snapshot
                        ) {
                            if (error != null) {
                                result.trySetException(
                                        error.toException()
                                );
                            } else if (!committed) {
                                result.trySetException(
                                        new IllegalStateException(
                                                "Score update cancelled."
                                        )
                                );
                            } else {
                                result.trySetResult(null);
                            }
                        }
                    },
                    false
                );

        return result.getTask();
    }
}
