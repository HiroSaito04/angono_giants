
package com.angonogiants.application;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    // UI Components
    private ImageButton backToMenuButton;

    // Navigation State
    private boolean isNavigating = false;

    // --------------------------------------------------
    // Activity Lifecycle
    // --------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeLayout();
        initializeViews();
        setupClickListeners();
    }

    // --------------------------------------------------
    // UI Initialization
    // --------------------------------------------------

    private void initializeLayout() {
        setContentView(R.layout.aboutus);
    }

    private void initializeViews() {
        backToMenuButton =
                findViewById(R.id.backToMenuButton);
    }

    // --------------------------------------------------
    // Button Listeners
    // --------------------------------------------------

    private void setupClickListeners() {
        backToMenuButton.setOnClickListener(
                v -> returnToMainMenu()
        );
    }

    // --------------------------------------------------
    // Navigation
    // --------------------------------------------------

    private void returnToMainMenu() {
        if (isNavigating || isFinishing()) {
            return;
        }

        isNavigating = true;
        backToMenuButton.setEnabled(false);

        finish();
        applyExitTransition();
    }

    // --------------------------------------------------
    // Screen Transitions
    // --------------------------------------------------

    private void applyExitTransition() {
        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }
}
