package com.angonogiants.application;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aboutus);

        ImageButton backButton = findViewById(R.id.backToMenuButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }
}
