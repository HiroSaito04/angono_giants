package com.example.angonogiants;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aboutus); // Make sure your XML is named exactly this

        ImageButton backButton = findViewById(R.id.backToMenuButton);

        backButton.setOnClickListener(v -> {
            AnimationDrawable animation = (AnimationDrawable) backButton.getBackground();
            if (animation != null) {
                animation.start();
            }

            // Return to Menu
            Intent intent = new Intent(AboutUsActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }
}