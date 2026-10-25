//app\src\main\java\com\angonogiants\application\GameTimer.java
package com.angonogiants.application;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public final class GameTimer {

    public interface Listener {
        void onTick(int remainingSeconds);
        void onFinished();
    }

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Listener listener;
    private final long durationMillis;

    private long remainingMillis;
    private long deadlineMillis;

    private boolean started = false;
    private boolean running = false;
    private boolean finished = false;

    private int lastDisplayedSeconds = -1;

    private final Runnable tickTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            remainingMillis = Math.max(
                    0L,
                    deadlineMillis
                            - SystemClock.elapsedRealtime()
            );

            notifyTick();

            if (remainingMillis == 0L) {
                running = false;
                finished = true;
                listener.onFinished();
            } else {
                handler.postDelayed(this, 100L);
            }
        }
    };

    public GameTimer(int durationSeconds, Listener listener) {
        this.listener = listener;
        this.durationMillis = durationSeconds * 1000L;
        this.remainingMillis = durationMillis;
    }

    public void start() {
        if (started || finished) return;

        started = true;
        running = true;

        deadlineMillis =
                SystemClock.elapsedRealtime()
                        + remainingMillis;

        notifyTick();
        handler.post(tickTask);
    }

    public void pause() {
        if (!running) return;

        remainingMillis = Math.max(
                0L,
                deadlineMillis - SystemClock.elapsedRealtime()
        );

        running = false;
        handler.removeCallbacks(tickTask);
        notifyTick();
    }

    public void resume() {
        if (!started || running || finished) return;

        if (remainingMillis <= 0L) {
            finished = true;
            listener.onFinished();
            return;
        }

        running = true;

        deadlineMillis =
                SystemClock.elapsedRealtime()
                        + remainingMillis;

        handler.post(tickTask);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tickTask);
    }

    public int getRemainingSeconds() {
        long millis = running
                ? Math.max(
                        0L,
                        deadlineMillis - SystemClock.elapsedRealtime()
                )
                : remainingMillis;

        return (int) ((millis + 999L) / 1000L);
    }

    public int getElapsedSeconds() {
        return (int) (
                (durationMillis
                        - Math.min(
                                durationMillis,
                                getRemainingMillis()
                        )) / 1000L
        );
    }

    public boolean isStarted() {
        return started;
    }

    private long getRemainingMillis() {
        return running
                ? Math.max(
                        0L,
                        deadlineMillis - SystemClock.elapsedRealtime()
                )
                : remainingMillis;
    }

    private void notifyTick() {
        int seconds = getRemainingSeconds();

        if (seconds != lastDisplayedSeconds) {
            lastDisplayedSeconds = seconds;
            listener.onTick(seconds);
        }
    }

    public void release() {
        stop();
    }
}
