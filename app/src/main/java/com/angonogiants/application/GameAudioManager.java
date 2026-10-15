
package com.angonogiants.application;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

public final class GameAudioManager {

    private static final String TAG = "GameAudio";

    public enum MusicMode {
        NONE,
        NORMAL,
        LOW_HEALTH,
        WON_INTRO,
        LOST_INTRO,
        WON_RESULTS,
        LOST_RESULTS
    }

    private final Context context;
    private final SoundPool soundPool;

    private final int[] enemyHitSounds = new int[4];
    private final int buttonSoundId;
    private final int loseLifeSoundId;
    private final int gainLifeSoundId;

    private MediaPlayer musicPlayer;
    private MediaPlayer growlPlayer;
    private MediaPlayer introPlayer;

    private MusicMode musicMode = MusicMode.NONE;

    private boolean active = false;
    private boolean released = false;
    private boolean musicFinished = false;

    public GameAudioManager(Context context) {
        this.context = context.getApplicationContext();

        AudioAttributes attributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build();

        enemyHitSounds[0] =
                soundPool.load(context, R.raw.case0_3, 1);
        enemyHitSounds[1] =
                soundPool.load(context, R.raw.case1_4, 1);
        enemyHitSounds[2] =
                soundPool.load(context, R.raw.case2_5, 1);
        enemyHitSounds[3] =
                soundPool.load(context, R.raw.case6, 1);

        buttonSoundId =
                soundPool.load(context, R.raw.buttonsound, 1);
        loseLifeSoundId =
                soundPool.load(context, R.raw.lose_life, 1);
        gainLifeSoundId =
                soundPool.load(context, R.raw.gain_life, 1);
    }

    // Called when gameplay enters or leaves foreground.
    public void setActive(boolean enabled) {
        if (released) return;

        active = enabled;

        if (enabled) {
            if (!musicFinished) play(musicPlayer);
            play(growlPlayer);
            play(introPlayer);
        } else {
            pause(musicPlayer);
            pause(growlPlayer);
            pause(introPlayer);
        }
    }

    public void setMusic(MusicMode mode) {
        if (released) return;

        if (musicMode == mode) {
            if (active && !musicFinished) {
                play(musicPlayer);
            }
            return;
        }

        musicPlayer = dispose(musicPlayer);
        musicMode = mode;
        musicFinished = false;

        int resource = getMusicResource(mode);
        if (resource == 0) return;

        musicPlayer = MediaPlayer.create(context, resource);

        if (musicPlayer == null) {
            Log.e(TAG, "Could not create music: " + mode);
            return;
        }

        boolean looping = mode == MusicMode.NORMAL
                || mode == MusicMode.LOW_HEALTH
                || mode == MusicMode.WON_RESULTS
                || mode == MusicMode.LOST_RESULTS;

        musicPlayer.setLooping(looping);

        if (!looping) {
            musicPlayer.setOnCompletionListener(
                    player -> musicFinished = true
            );
        }

        if (active) {
            play(musicPlayer);
        }
    }

    private int getMusicResource(MusicMode mode) {
        switch (mode) {
            case NORMAL:
                return R.raw.bgmusic;
            case LOW_HEALTH:
                return R.raw.lowhealth;
            case WON_INTRO:
                return R.raw.gamewonthankzmuzic;
            case LOST_INTRO:
                return R.raw.gameover;
            case WON_RESULTS:
                return R.raw.wonfinalzcoremuzic;
            case LOST_RESULTS:
                return R.raw.loztfinalzcoremuzic;
            default:
                return 0;
        }
    }

    public void playIntro() {
        if (released || introPlayer != null) return;

        introPlayer =
                MediaPlayer.create(context, R.raw.gamestart);

        if (introPlayer == null) return;

        MediaPlayer player = introPlayer;

        player.setOnCompletionListener(completed -> {
            if (introPlayer == completed) {
                introPlayer = null;
            }
            dispose(completed);
        });

        if (active) {
            play(player);
        }
    }

    public void playGrowl(
            boolean firstWave,
            Runnable onComplete
    ) {
        if (released) return;

        cancelGrowl();

        int resource = firstWave
                ? R.raw.giantgrowl
                : R.raw.wavegrowl;

        MediaPlayer player =
                MediaPlayer.create(context, resource);

        if (player == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        growlPlayer = player;

        player.setOnCompletionListener(completed -> {
            if (growlPlayer != completed) return;

            growlPlayer = null;
            dispose(completed);

            if (onComplete != null && !released) {
                onComplete.run();
            }
        });

        if (active) {
            play(player);
        }
    }

    public void cancelGrowl() {
        growlPlayer = dispose(growlPlayer);
    }

    public void playEnemyHit(int type) {
        int index;

        switch (type) {
            case 0:
            case 3:
                index = 0;
                break;
            case 1:
            case 4:
                index = 1;
                break;
            case 2:
            case 5:
                index = 2;
                break;
            case 6:
                index = 3;
                break;
            default:
                return;
        }

        playEffect(enemyHitSounds[index]);
    }

    public void playButtonSound() {
        playEffect(buttonSoundId);
    }

    public void playLoseLife() {
        playEffect(loseLifeSoundId);
    }

    public void playGainLife() {
        playEffect(gainLifeSoundId);
    }

    private void playEffect(int soundId) {
        if (released || !active || soundId <= 0) {
            return;
        }

        soundPool.play(
                soundId,
                1f, 1f,
                0, 0, 1f
        );
    }

    private void play(MediaPlayer player) {
        if (player == null || !active) return;

        try {
            if (!player.isPlaying()) {
                player.start();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot start audio", e);
        }
    }

    private void pause(MediaPlayer player) {
        if (player == null) return;

        try {
            if (player.isPlaying()) {
                player.pause();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot pause audio", e);
        }
    }

    private MediaPlayer dispose(MediaPlayer player) {
        if (player != null) {
            try {
                player.setOnCompletionListener(null);
                player.release();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Audio cleanup failed", e);
            }
        }

        return null;
    }

    public void release() {
        if (released) return;

        setActive(false);
        released = true;

        musicPlayer = dispose(musicPlayer);
        growlPlayer = dispose(growlPlayer);
        introPlayer = dispose(introPlayer);

        soundPool.release();
    }
}
