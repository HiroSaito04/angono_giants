
package com.angonogiants.application;

public final class GameStateManager {

    public enum State {
        PLAYING,
        PAUSED,
        LEVEL_COMPLETE,
        WON_INTRO,
        LOST_INTRO,
        RESULTS
    }

    private State state = State.PLAYING;
    private boolean won = false;

    public State getState() {
        return state;
    }

    public boolean isPlaying() {
        return state == State.PLAYING;
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public boolean isLevelComplete() {
        return state == State.LEVEL_COMPLETE;
    }

    public boolean isGameOver() {
        return state == State.LEVEL_COMPLETE
                || state == State.WON_INTRO
                || state == State.LOST_INTRO
                || state == State.RESULTS;
    }

    public boolean didWin() {
        return won;
    }

    public boolean pause() {
        if (state != State.PLAYING) {
            return false;
        }

        state = State.PAUSED;
        return true;
    }

    public boolean resume() {
        if (state != State.PAUSED) {
            return false;
        }

        state = State.PLAYING;
        return true;
    }

    public boolean completeLevel() {
        if (state != State.PLAYING) {
            return false;
        }

        state = State.LEVEL_COMPLETE;
        return true;
    }

    public boolean startNextLevel() {
        if (state != State.LEVEL_COMPLETE) {
            return false;
        }

        state = State.PLAYING;
        return true;
    }

    public boolean lose() {
        if (state != State.PLAYING
                && state != State.PAUSED) {
            return false;
        }

        won = false;
        state = State.LOST_INTRO;

        return true;
    }

    // Reserved for a future final-boss or campaign ending.
    public boolean win() {
        if (state != State.PLAYING) {
            return false;
        }

        won = true;
        state = State.WON_INTRO;

        return true;
    }

    public boolean showResults() {
        if (state != State.WON_INTRO
                && state != State.LOST_INTRO) {
            return false;
        }

        state = State.RESULTS;
        return true;
    }
}
