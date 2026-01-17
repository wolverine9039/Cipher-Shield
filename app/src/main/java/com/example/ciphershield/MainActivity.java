package com.example.ciphershield;

import android.animation.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private TextView txtAppName, txtTagline;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        imgLogo = findViewById(R.id.imgLogo);
        txtAppName = findViewById(R.id.txtAppName);
        txtTagline = findViewById(R.id.txtTagline);
        progressBar = findViewById(R.id.progressBar);

        // Start animations
        animateSplashScreen();

        // Navigate to front after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, front.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3000);
    }

    private void animateSplashScreen() {
        // Logo animation - scale and fade in
        imgLogo.setAlpha(0f);
        imgLogo.setScaleX(0.3f);
        imgLogo.setScaleY(0.3f);

        imgLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // Rotate animation for logo
        ObjectAnimator rotate = ObjectAnimator.ofFloat(imgLogo, "rotation", 0f, 360f);
        rotate.setDuration(1500);
        rotate.setStartDelay(200);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.start();

        // App name animation - slide from bottom
        txtAppName.setAlpha(0f);
        txtAppName.setTranslationY(100f);

        txtAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Tagline animation - fade in
        txtTagline.setAlpha(0f);
        txtTagline.setTranslationY(50f);

        txtTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(700)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Progress bar fade in
        progressBar.setAlpha(0f);
        progressBar.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(1000)
                .start();

        // Pulse animation for logo
        ObjectAnimator pulse = ObjectAnimator.ofFloat(imgLogo, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(imgLogo, "scaleY", 1f, 1.05f, 1f);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulse, pulseY);
        pulseSet.setDuration(1500);
        pulseSet.setStartDelay(1200);
        pulseSet.setDuration(1);
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.start();
    }
}