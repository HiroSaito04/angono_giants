
package com.angonogiants.application;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class GameScoreManager {

    public interface Listener {
        void onScoreChanged(int newScore);
    }

    public static final int PENALTY = 1000;

    public static final class ScorePopup {
        public final float x;
        public float y;
        public final int points;
        public final boolean red;
        public int alpha = 255;

        private final long createdAt;

        ScorePopup(float x, float y, int points,
                   boolean red) {
            this.x = x;
            this.y = y;
            this.points = points;
            this.red = red;
            this.createdAt = SystemClock.uptimeMillis();
        }

        boolean update(long now, float frameFactor) {
            long elapsed = now - createdAt;

            if (elapsed >= 1000L) return false;

            y -= 2f * frameFactor;
            alpha = (int) (255f * (1f - elapsed / 1000f));
            return true;
        }
    }

    private final Random random = new Random();
    private final List<ScorePopup> popups =
            new ArrayList<>();

    private final Listener listener;
    private int score = 0;

    public GameScoreManager(Listener listener) {
        this.listener = listener;
    }

    public int getScore() {
        return score;
    }

    public List<ScorePopup> getPopups() {
        return popups;
    }

    public int hitEnemy(
            EnemyManager.Enemy enemy,
            float targetX,
            float targetY
    ) {
        int earned;

        if (enemy.type == 6) {
            earned = -PENALTY;
        } else {
            earned = calculateHitScore(
                    enemy.type,
                    enemy.x,
                    enemy.y,
                    targetX,
                    targetY
            );
        }

        addPoints(earned, enemy.x, enemy.y);
        return earned;
    }

    public void deductStallLife(
            float targetX,
            float targetY
    ) {
        addPoints(-PENALTY, targetX, targetY);
    }

    private void addPoints(
            int points,
            float x,
            float y
    ) {
        score += points;

        popups.add(new ScorePopup(
                x,
                y,
                points,
                points < 0
        ));

        if (listener != null) {
            listener.onScoreChanged(score);
        }
    }

    private int calculateHitScore(
            int type,
            float x,
            float y,
            float targetX,
            float targetY
    ) {
        int baseScore;

        switch (type) {
            case 0:
            case 3:
                baseScore = 100;
                break;

            case 1:
            case 4:
                baseScore = 250;
                break;

            case 2:
            case 5:
                baseScore = 400;
                break;

            default:
                return 0;
        }

        float dx = x - targetX;
        float dy = y - targetY;

        float distance =
                (float) Math.sqrt(dx * dx + dy * dy);

        float factor = 1f - Math.max(
                0f,
                Math.min(
                        1f,
                        (distance - 100f) / 700f
                )
        );

        int bonus =
                (int) (baseScore * 0.2f * factor);

        int randomOffset = random.nextInt(10) - 5;

        return baseScore + bonus + randomOffset;
    }

    public void update(long now, float frameFactor) {
        Iterator<ScorePopup> iterator =
                popups.iterator();

        while (iterator.hasNext()) {
            if (!iterator.next().update(
                    now,
                    frameFactor
            )) {
                iterator.remove();
            }
        }
    }

    public void clearPopups() {
        popups.clear();
    }
}
