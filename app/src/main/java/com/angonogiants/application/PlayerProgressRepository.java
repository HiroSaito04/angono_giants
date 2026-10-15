
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

public final class PlayerProgressRepository {

    private static final String DATABASE_URL =
            "https://angonogiants-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final DatabaseReference progressRoot;

    public PlayerProgressRepository() {
        progressRoot = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("playerProgress");
    }

    private DatabaseReference getCurrentPlayerRef() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            throw new IllegalStateException(
                    "Player must be signed in"
            );
        }

        return progressRoot.child(user.getUid());
    }

    public Task<DataSnapshot> loadProgress() {
        try {
            return getCurrentPlayerRef().get();
        } catch (IllegalStateException error) {
            return Tasks.forException(error);
        }
    }

    public Task<Void> completeLevel(
            int completedLevel,
            int score
    ) {
        return updateProgress(
                LevelConfig.getNextLevel(completedLevel),
                score
        );
    }

    public Task<Void> recordBestScore(int score) {
        return updateProgress(1, score);
    }

    private Task<Void> updateProgress(
            int minimumUnlockedLevel,
            int score
    ) {
        TaskCompletionSource<Void> completion =
                new TaskCompletionSource<>();

        DatabaseReference reference;

        try {
            reference = getCurrentPlayerRef();
        } catch (IllegalStateException error) {
            completion.setException(error);
            return completion.getTask();
        }

        reference.runTransaction(
                new Transaction.Handler() {

                    @Override
                    public Transaction.Result doTransaction(
                            MutableData current
                    ) {
                        PlayerProgress progress =
                                current.getValue(
                                        PlayerProgress.class
                                );

                        if (progress == null) {
                            progress = new PlayerProgress();
                        }

                        progress.highestUnlockedLevel =
                                Math.max(
                                        progress.highestUnlockedLevel,
                                        minimumUnlockedLevel
                                );

                        progress.bestScore = Math.max(
                                progress.bestScore,
                                score
                        );

                        current.setValue(progress);

                        return Transaction.success(current);
                    }

                    @Override
                    public void onComplete(
                            DatabaseError error,
                            boolean committed,
                            DataSnapshot snapshot
                    ) {
                        if (error != null) {
                            completion.trySetException(
                                    error.toException()
                            );
                        } else if (!committed) {
                            completion.trySetException(
                                    new IllegalStateException(
                                            "Progress update cancelled"
                                    )
                            );
                        } else {
                            completion.trySetResult(null);
                        }
                    }
                }
        );

        return completion.getTask();
    }
}
