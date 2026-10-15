
package com.angonogiants.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;

import androidx.core.content.res.ResourcesCompat;

import java.util.List;

public final class GameRenderer {

    private final Context context;
    private final Paint bitmapPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint scorePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint grayscalePaint = new Paint();
    private final Matrix matrix = new Matrix();

    private final Bitmap[] topSprites = new Bitmap[7];
    private final Bitmap[] bottomSprites = new Bitmap[7];

    private final Bitmap[] stallStates = new Bitmap[4];
    private final Bitmap[] heartStates = new Bitmap[4];

    private Bitmap warningBitmap;

    private int screenWidth;
    private int screenHeight;
    private int lives = 3;

    private long warningStartedAt;
    private boolean lost = false;

    public GameRenderer(Context context) {
        this.context = context;

        int[] topIds = {
                R.drawable.higantes1top,
                R.drawable.higantes2top,
                R.drawable.higantes3top,
                R.drawable.higantes4top,
                R.drawable.higantes5top,
                R.drawable.higantes6top,
                R.drawable.higantes7top
        };

        int[] bottomIds = {
                R.drawable.higantes1bottom,
                R.drawable.higantes2bottom,
                R.drawable.higantes3bottom,
                R.drawable.higantes4bottom,
                R.drawable.higantes5bottom,
                R.drawable.higantes6bottom,
                R.drawable.higantes7bottom
        };

        for (int i = 0; i < 7; i++) {
            topSprites[i] = loadScaled(topIds[i], 90);
            bottomSprites[i] = loadScaled(bottomIds[i], 90);
        }

        int[] stallIds = {
                R.drawable.stall01,
                R.drawable.stall02,
                R.drawable.stall03,
                R.drawable.stall_destroyed
        };

        int[] heartIds = {
                R.drawable.heart_full,
                R.drawable.heart_two,
                R.drawable.heart_one,
                R.drawable.heart_empty
        };

        for (int i = 0; i < 4; i++) {
            stallStates[i] =
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            stallIds[i]
                    );

            heartStates[i] =
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            heartIds[i]
                    );
        }

        Typeface font = ResourcesCompat.getFont(
                context,
                R.font.slkscr
        );

        scorePaint.setTypeface(
                font != null ? font : Typeface.DEFAULT_BOLD
        );
        scorePaint.setTextSize(60f);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);

        grayscalePaint.setColorFilter(
                new ColorMatrixColorFilter(colorMatrix)
        );
    }

    private Bitmap loadScaled(int resId, int sizeDp) {
        Bitmap original = BitmapFactory.decodeResource(
                context.getResources(),
                resId
        );

        if (original == null) return null;

        float density =
                context.getResources()
                        .getDisplayMetrics().density;

        int pixels = Math.max(
                1,
                Math.round(sizeDp * density)
        );

        Bitmap scaled = Bitmap.createScaledBitmap(
                original, pixels, pixels, true
        );

        if (scaled != original) {
            original.recycle();
        }

        return scaled;
    }

    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;

        if (warningBitmap != null) {
            warningBitmap.recycle();
            warningBitmap = null;
        }

        if (width <= 0 || height <= 0) return;

        Bitmap original = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.red_warning
        );

        if (original != null) {
            warningBitmap = Bitmap.createScaledBitmap(
                    original, width, height, true
            );

            if (warningBitmap != original) {
                original.recycle();
            }
        }
    }

    public void setLives(int newLives) {
        if (lives != newLives) {
            lives = Math.max(0, Math.min(3, newLives));

            warningStartedAt = SystemClock.uptimeMillis();
        }
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }

    public void draw(
            Canvas canvas,
            List<EnemyManager.Enemy> enemies,
            List<GameScoreManager.ScorePopup> popups,
            long now
    ) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        int checkpoint = -1;

        if (lost) {
            checkpoint = canvas.saveLayer(
                    null,
                    grayscalePaint
            );
        }

        drawWarning(canvas, now);

        for (EnemyManager.Enemy enemy : enemies) {
            if (enemy.fromTop) {
                drawEnemy(canvas, enemy, now);
            }
        }

        drawStall(canvas);

        for (EnemyManager.Enemy enemy : enemies) {
            if (!enemy.fromTop) {
                drawEnemy(canvas, enemy, now);
            }
        }

        drawHearts(canvas);
        drawScorePopups(canvas, popups);

        if (checkpoint != -1) {
            canvas.restoreToCount(checkpoint);
        }
    }

    private void drawEnemy(
            Canvas canvas,
            EnemyManager.Enemy enemy,
            long now
    ) {
        Bitmap bitmap = enemy.fromTop
                ? topSprites[enemy.type]
                : bottomSprites[enemy.type];

        if (bitmap == null) return;

        float angle = (float) (
                Math.sin(
                        (now - enemy.tiltOrigin)
                                * enemy.tiltSpeed
                ) * 15d
        );

        matrix.reset();

        matrix.postRotate(
                angle,
                bitmap.getWidth() / 2f,
                bitmap.getHeight() / 2f
        );

        matrix.postTranslate(
                enemy.x - bitmap.getWidth() / 2f,
                enemy.y - bitmap.getHeight() / 2f
        );

        canvas.drawBitmap(bitmap, matrix, bitmapPaint);
    }

    private void drawStall(Canvas canvas) {
        Bitmap bitmap = stallStates[3 - lives];

        if (bitmap == null) return;

        canvas.drawBitmap(
                bitmap,
                screenWidth / 2f - bitmap.getWidth() / 2f,
                screenHeight / 2f - bitmap.getHeight() / 2f,
                bitmapPaint
        );
    }

    private void drawHearts(Canvas canvas) {
        Bitmap heart = heartStates[3 - lives];
        Bitmap stall = stallStates[3 - lives];

        if (heart == null || stall == null) return;

        canvas.drawBitmap(
                heart,
                screenWidth / 2f - heart.getWidth() / 2f,
                screenHeight / 2f - stall.getHeight() / 2f
                        - 100f,
                bitmapPaint
        );
    }

    private void drawWarning(Canvas canvas, long now) {
        if (warningBitmap == null) return;

        if (lives == 2) {
            if (now - warningStartedAt < 500L) {
                canvas.drawBitmap(
                        warningBitmap, 0f, 0f, bitmapPaint
                );
            }
        } else if (lives == 1) {
            if ((now / 500L) % 2 == 0) {
                canvas.drawBitmap(
                        warningBitmap, 0f, 0f, bitmapPaint
                );
            }
        }
    }

    private void drawScorePopups(
            Canvas canvas,
            List<GameScoreManager.ScorePopup> popups
    ) {
        for (GameScoreManager.ScorePopup popup : popups) {
            scorePaint.setColor(
                    popup.red ? Color.RED : Color.WHITE
            );
            scorePaint.setAlpha(popup.alpha);

            String text = popup.points > 0
                    ? "+" + popup.points
                    : String.valueOf(popup.points);

            canvas.drawText(
                    text,
                    popup.x,
                    popup.y,
                    scorePaint
            );
        }

        scorePaint.setAlpha(255);
    }

    public void release() {
        for (int i = 0; i < 7; i++) {
            recycle(topSprites[i]);
            recycle(bottomSprites[i]);

            topSprites[i] = null;
            bottomSprites[i] = null;
        }

        for (int i = 0; i < 4; i++) {
            recycle(stallStates[i]);
            recycle(heartStates[i]);

            stallStates[i] = null;
            heartStates[i] = null;
        }

        recycle(warningBitmap);
        warningBitmap = null;
    }

    private void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
