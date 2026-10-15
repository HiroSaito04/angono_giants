//app\src\main\java\com\angonogiants\application\SplashActivity.java

package com.angonogiants.application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Display the splash screen for 1 second
    private static final long SPLASH_DURATION = 1000L;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable openMainMenu = () -> {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Intent intent = new Intent(
                SplashActivity.this,
                MainActivity.class
        );

        startActivity(intent);

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        handler.postDelayed(
                openMainMenu,
                SPLASH_DURATION
        );
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openMainMenu);
        super.onDestroy();
    }
}
