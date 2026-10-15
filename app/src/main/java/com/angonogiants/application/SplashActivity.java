
package com.angonogiants.application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 1000L;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean navigated = false;

    private final Runnable navigationTask =
            this::openNextScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.removeCallbacks(navigationTask);

        if (!navigated) {
            handler.postDelayed(
                    navigationTask,
                    SPLASH_DURATION
            );
        }
    }

    private void openNextScreen() {
        if (navigated || isFinishing() || isDestroyed()) {
            return;
        }

        navigated = true;

        Class<?> destination =
                FirebaseAuth.getInstance()
                        .getCurrentUser() != null
                        ? MainActivity.class
                        : AuthActivity.class;

        Intent intent = new Intent(this, destination);
        startActivity(intent);

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(navigationTask);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigationTask);
        super.onDestroy();
    }
}
