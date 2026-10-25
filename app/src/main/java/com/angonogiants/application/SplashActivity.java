//app\src\main\java\com\angonogiants\application\SplashActivity.java
package com.angonogiants.application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1000L;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable openMain = () -> {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Intent intent = new Intent(
                this,
                MainActivity.class
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );

        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        handler.postDelayed(
                openMain,
                SPLASH_DELAY
        );
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openMain);
        super.onDestroy();
    }
}
