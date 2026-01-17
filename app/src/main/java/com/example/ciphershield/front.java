package com.example.ciphershield;

import android.animation.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.*;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class front extends AppCompatActivity {

    private MaterialCardView cardEncrypt, cardDecrypt, cardUserManual, cardAbout;
    private ImageView imgEncrypt, imgDecrypt, imgManual, imgAbout;
    private FloatingActionButton fabInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front);

        // Hide action bar for modern look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupClickListeners();
        animateEntrance();
    }

    private void initializeViews() {
        cardEncrypt = findViewById(R.id.cardEncrypt);
        cardDecrypt = findViewById(R.id.cardDecrypt);
        cardUserManual = findViewById(R.id.cardUserManual);
        cardAbout = findViewById(R.id.cardAbout);

        imgEncrypt = findViewById(R.id.imgEncrypt);
        imgDecrypt = findViewById(R.id.imgDecrypt);
        imgManual = findViewById(R.id.imgManual);
        imgAbout = findViewById(R.id.imgAbout);

        fabInfo = findViewById(R.id.fabInfo);
    }

    private void setupClickListeners() {
        cardEncrypt.setOnClickListener(v -> {
            animateCardClick(cardEncrypt, imgEncrypt);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(front.this, ModernEncryptionActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        });

        cardDecrypt.setOnClickListener(v -> {
            animateCardClick(cardDecrypt, imgDecrypt);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(front.this, ModernDecryptionActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        });

        cardUserManual.setOnClickListener(v -> {
            animateCardClick(cardUserManual, imgManual);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(front.this, UserManual.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        });

        cardAbout.setOnClickListener(v -> {
            animateCardClick(cardAbout, imgAbout);
            showAboutDialog();
        });

        fabInfo.setOnClickListener(v -> {
            animateFab();
            showQuickTips();
        });
    }

    private void animateEntrance() {
        // Stagger card animations
        animateCardEntrance(cardEncrypt, 0);
        animateCardEntrance(cardDecrypt, 100);
        animateCardEntrance(cardUserManual, 200);
        animateCardEntrance(cardAbout, 300);

        // FAB entrance
        fabInfo.setScaleX(0f);
        fabInfo.setScaleY(0f);
        fabInfo.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(400)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void animateCardEntrance(View card, int delay) {
        card.setAlpha(0f);
        card.setTranslationX(-100f);
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);

        card.animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateCardClick(MaterialCardView card, ImageView icon) {
        // Card press animation
        card.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    card.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();

        // Icon pulse
        ObjectAnimator pulse = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.2f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(pulse, pulseY);
        set.setDuration(300);
        set.start();
    }

    private void animateFab() {
        ObjectAnimator rotate = ObjectAnimator.ofFloat(fabInfo, "rotation", 0f, 360f);
        rotate.setDuration(400);
        rotate.start();
    }

    private void showAboutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);

        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        
        dialogView.findViewById(R.id.close_button).setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();

        // Animate dialog entrance
        if (dialogView != null) {
            dialogView.setAlpha(0f);
            dialogView.setScaleX(0.8f);
            dialogView.setScaleY(0.8f);
            dialogView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    private void showQuickTips() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🔐 Quick Security Tips")
                .setMessage("✓ Always keep your encryption keys safe\n\n" +
                        "✓ Use strong passwords (8+ characters)\n\n" +
                        "✓ Files over 10MB use optimized chunked encryption\n\n" +
                        "✓ Preview files are auto-deleted for security\n\n" +
                        "✓ Green checkmark = verified file integrity")
                .setPositiveButton("Got it!", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        // Animate cards exit before closing
        cardEncrypt.animate().alpha(0f).translationX(-100f).setDuration(200).start();
        cardDecrypt.animate().alpha(0f).translationX(-100f).setDuration(200).setStartDelay(50).start();
        cardUserManual.animate().alpha(0f).translationX(-100f).setDuration(200).setStartDelay(100).start();
        cardAbout.animate().alpha(0f).translationX(-100f).setDuration(200).setStartDelay(150).start();

        new Handler().postDelayed(() -> {
            finish();
            super.onBackPressed();
        }, 400);
    }
}
