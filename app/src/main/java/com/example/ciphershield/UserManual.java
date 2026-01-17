package com.example.ciphershield;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class UserManual extends AppCompatActivity {

    private SearchView searchView;
    private LinearLayout contentContainer;
    private MaterialButton btnTTS, btnBack;
    private ImageView imgHeader;
    private TextView txtTitle, txtSubtitle;
    private TextToSpeech tts;
    private boolean isSpeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupClickListeners();
        setupTTS();
        loadContent();
        animateEntrance();
    }

    private void initializeViews() {
        searchView = findViewById(R.id.searchView);
        contentContainer = findViewById(R.id.contentContainer);
        btnTTS = findViewById(R.id.btnTTS);
        btnBack = findViewById(R.id.btnBack);
        imgHeader = findViewById(R.id.imgHeader);
        txtTitle = findViewById(R.id.txtTitle);
        txtSubtitle = findViewById(R.id.txtSubtitle);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });

        btnTTS.setOnClickListener(v -> {
            animateButtonClick(v);
            toggleTTS();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterContent(newText);
                return true;
            }
        });
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(java.util.Locale.US);
            }
        });
    }

    private void toggleTTS() {
        if (isSpeaking) {
            tts.stop();
            isSpeaking = false;
            btnTTS.setText("🔊 Read Aloud");
            btnTTS.setIconResource(R.drawable.ic_volume_up);
        } else {
            String content = getAllTextContent();
            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, null);
            isSpeaking = true;
            btnTTS.setText("⏹ Stop Reading");
            btnTTS.setIconResource(R.drawable.ic_stop);
        }
    }

    private void loadContent() {
        addWelcomeSection();
        addQuickStartSection();
        addEncryptionSection();
        addDecryptionSection();
        addSecuritySection();
        addFAQSection();
        addTroubleshootingSection();
    }

    private void addWelcomeSection() {
        MaterialCardView card = createSectionCard(
                "👋 Welcome to Cipher Shield",
                "Your files deserve military-grade protection. Cipher Shield uses AES-256-GCM encryption with RSA-2048 key wrapping to ensure your sensitive data stays secure.",
                "#E8F5E9"
        );
        contentContainer.addView(card);
    }

    private void addQuickStartSection() {
        MaterialCardView card = createSectionCard(
                "🚀 Quick Start Guide",
                "Get started in 3 simple steps:\n\n" +
                        "1️⃣ Select File - Choose any file you want to protect\n\n" +
                        "2️⃣ Choose Method - Use key-based (recommended) or password protection\n\n" +
                        "3️⃣ Encrypt - Your file is now secure! Save both the encrypted file and key",
                "#E3F2FD"
        );
        contentContainer.addView(card);
    }

    private void addEncryptionSection() {
        MaterialCardView card = new MaterialCardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).setMargins(0, 0, 0, 32);
        card.setRadius(48f);
        card.setCardElevation(12f);
        card.setStrokeWidth(0);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(64, 64, 64, 64);

        // Title
        TextView title = new TextView(this);
        title.setText("🔒 How to Encrypt Files");
        title.setTextSize(22);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(title);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 4
        ));
        ((LinearLayout.LayoutParams) divider.getLayoutParams()).setMargins(0, 24, 0, 24);
        divider.setBackgroundColor(getColor(R.color.primary_light));
        container.addView(divider);

        // Steps
        addStep(container, "1", "Select File", "Tap 'Encrypt File' on the home screen, then choose the file you want to protect");
        addStep(container, "2", "Choose Encryption Method", "• Key-Based (Recommended): Generates a secure key file\n• Password: Protect with a password (8+ characters)");
        addStep(container, "3", "Encrypt", "Tap 'Encrypt File' - files over 10MB use optimized chunked encryption");
        addStep(container, "4", "Save Files", "Save both the encrypted file (.csk) and key file (.key) in secure locations");

        // Warning box
        MaterialCardView warningBox = new MaterialCardView(this);
        warningBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) warningBox.getLayoutParams()).setMargins(0, 32, 0, 0);
        warningBox.setCardBackgroundColor(getColor(R.color.warning_background));
        warningBox.setRadius(24f);
        warningBox.setCardElevation(0f);

        LinearLayout warningContent = new LinearLayout(this);
        warningContent.setOrientation(LinearLayout.HORIZONTAL);
        warningContent.setPadding(48, 32, 48, 32);

        TextView warningIcon = new TextView(this);
        warningIcon.setText("⚠️");
        warningIcon.setTextSize(32);
        warningIcon.setPadding(0, 0, 32, 0);
        warningContent.addView(warningIcon);

        TextView warningText = new TextView(this);
        warningText.setText("IMPORTANT: Keep your key file safe! Without it, encrypted files cannot be recovered.");
        warningText.setTextSize(14);
        warningText.setTextColor(getColor(R.color.warning_text));
        warningContent.addView(warningText);

        warningBox.addView(warningContent);
        container.addView(warningBox);

        card.addView(container);
        contentContainer.addView(card);
    }

    private void addDecryptionSection() {
        MaterialCardView card = new MaterialCardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).setMargins(0, 0, 0, 32);
        card.setRadius(48f);
        card.setCardElevation(12f);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("🔓 How to Decrypt Files");
        title.setTextSize(22);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(title);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 4
        ));
        ((LinearLayout.LayoutParams) divider.getLayoutParams()).setMargins(0, 24, 0, 24);
        divider.setBackgroundColor(getColor(R.color.success));
        container.addView(divider);

        addStep(container, "1", "Select Encrypted File", "Tap 'Decrypt File' and choose your .csk or .csp file");
        addStep(container, "2", "Select Key File", "Choose the corresponding .key file (or enter password if password-protected)");
        addStep(container, "3", "Decrypt", "Tap 'Decrypt File' - the app will restore your original file");
        addStep(container, "4", "Save or Preview", "Save the decrypted file or preview it directly in the app");

        // Success box
        MaterialCardView successBox = new MaterialCardView(this);
        successBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) successBox.getLayoutParams()).setMargins(0, 32, 0, 0);
        successBox.setCardBackgroundColor(getColor(R.color.success_background));
        successBox.setRadius(24f);
        successBox.setCardElevation(0f);

        LinearLayout successContent = new LinearLayout(this);
        successContent.setOrientation(LinearLayout.HORIZONTAL);
        successContent.setPadding(48, 32, 48, 32);

        TextView successIcon = new TextView(this);
        successIcon.setText("✅");
        successIcon.setTextSize(32);
        successIcon.setPadding(0, 0, 32, 0);
        successContent.addView(successIcon);

        TextView successText = new TextView(this);
        successText.setText("Green checkmark = File integrity verified! Your file is authentic and unmodified.");
        successText.setTextSize(14);
        successText.setTextColor(getColor(R.color.success));
        successContent.addView(successText);

        successBox.addView(successContent);
        container.addView(successBox);

        card.addView(container);
        contentContainer.addView(card);
    }

    private void addSecuritySection() {
        MaterialCardView card = createSectionCard(
                "🛡️ Security Features",
                "• AES-256-GCM Encryption - Military-grade security\n\n" +
                        "• RSA-2048 Key Wrapping - Secure key exchange\n\n" +
                        "• HMAC-SHA256 - File integrity verification\n\n" +
                        "• Chunked Encryption - Optimized for large files (10MB+)\n\n" +
                        "• Original Extension Preservation - Files retain their format\n\n" +
                        "• Secure Preview - Auto-deleted after 5 minutes",
                "#FFF3E0"
        );
        contentContainer.addView(card);
    }

    private void addFAQSection() {
        MaterialCardView card = new MaterialCardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).setMargins(0, 0, 0, 32);
        card.setRadius(48f);
        card.setCardElevation(12f);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("❓ Frequently Asked Questions");
        title.setTextSize(22);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(title);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 4
        ));
        ((LinearLayout.LayoutParams) divider.getLayoutParams()).setMargins(0, 24, 0, 24);
        divider.setBackgroundColor(getColor(R.color.accent));
        container.addView(divider);

        addFAQItem(container, "What file types can I encrypt?",
                "Any file type! Documents, images, videos, archives - everything is supported.");

        addFAQItem(container, "What's the maximum file size?",
                "No limit! Files over 10MB automatically use optimized chunked encryption.");

        addFAQItem(container, "What if I lose my key file?",
                "Unfortunately, encrypted files cannot be recovered without the key. Always backup your keys securely.");

        addFAQItem(container, "Is password or key-based better?",
                "Key-based is more secure. Passwords are convenient but can be guessed with brute force.");

        addFAQItem(container, "Can I share encrypted files?",
                "Yes! Share the encrypted file and key separately through different channels for maximum security.");

        card.addView(container);
        contentContainer.addView(card);
    }

    private void addTroubleshootingSection() {
        MaterialCardView card = createSectionCard(
                "🔧 Troubleshooting",
                "Decryption Failed?\n" +
                        "• Ensure you selected the correct key file\n" +
                        "• Check if password is correct (case-sensitive)\n" +
                        "• Verify the encrypted file isn't corrupted\n\n" +
                        "File Too Large Error?\n" +
                        "• Files over 256MB may require more RAM\n" +
                        "• Try closing other apps\n" +
                        "• Chunked encryption handles this automatically\n\n" +
                        "Preview Not Working?\n" +
                        "• Install an app that can open this file type\n" +
                        "• Some formats require specific viewers",
                "#FFEBEE"
        );
        contentContainer.addView(card);
    }

    private MaterialCardView createSectionCard(String title, String content, String bgColor) {
        MaterialCardView card = new MaterialCardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).setMargins(0, 0, 0, 32);
        card.setRadius(48f);
        card.setCardElevation(12f);
        card.setCardBackgroundColor(android.graphics.Color.parseColor(bgColor));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(64, 64, 64, 64);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(22);
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 32);
        container.addView(titleView);

        TextView contentView = new TextView(this);
        contentView.setText(content);
        contentView.setTextSize(16);
        contentView.setTextColor(getColor(R.color.text_secondary));
        contentView.setLineSpacing(8, 1);
        container.addView(contentView);

        card.addView(container);
        return card;
    }

    private void addStep(LinearLayout parent, String number, String title, String description) {
        LinearLayout stepContainer = new LinearLayout(this);
        stepContainer.setOrientation(LinearLayout.HORIZONTAL);
        stepContainer.setPadding(0, 0, 0, 48);

        // Number circle
        TextView numberView = new TextView(this);
        numberView.setText(number);
        numberView.setTextSize(20);
        numberView.setTextColor(getColor(android.R.color.white));
        numberView.setTypeface(null, android.graphics.Typeface.BOLD);
        numberView.setGravity(android.view.Gravity.CENTER);
        numberView.setBackground(getDrawable(R.drawable.circle_primary));
        numberView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        stepContainer.addView(numberView);

        // Content
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        contentParams.setMargins(32, 0, 0, 0);
        contentLayout.setLayoutParams(contentParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        contentLayout.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextSize(14);
        descView.setTextColor(getColor(R.color.text_secondary));
        descView.setLineSpacing(6, 1);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, 16, 0, 0);
        descView.setLayoutParams(descParams);
        contentLayout.addView(descView);

        stepContainer.addView(contentLayout);
        parent.addView(stepContainer);
    }

    private void addFAQItem(LinearLayout parent, String question, String answer) {
        LinearLayout faqItem = new LinearLayout(this);
        faqItem.setOrientation(LinearLayout.VERTICAL);
        faqItem.setPadding(0, 0, 0, 48);

        TextView questionView = new TextView(this);
        questionView.setText("Q: " + question);
        questionView.setTextSize(16);
        questionView.setTextColor(getColor(R.color.text_primary));
        questionView.setTypeface(null, android.graphics.Typeface.BOLD);
        faqItem.addView(questionView);

        TextView answerView = new TextView(this);
        answerView.setText("A: " + answer);
        answerView.setTextSize(14);
        answerView.setTextColor(getColor(R.color.text_secondary));
        answerView.setLineSpacing(6, 1);
        LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        answerParams.setMargins(0, 16, 0, 0);
        answerView.setLayoutParams(answerParams);
        faqItem.addView(answerView);

        parent.addView(faqItem);
    }

    private void animateEntrance() {
        // Header animation
        imgHeader.setAlpha(0f);
        imgHeader.setScaleX(0.5f);
        imgHeader.setScaleY(0.5f);
        imgHeader.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Title animation
        txtTitle.setAlpha(0f);
        txtTitle.setTranslationY(50f);
        txtTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();

        // Content animation
        for (int i = 0; i < contentContainer.getChildCount(); i++) {
            View child = contentContainer.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(30f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(300 + (i * 100))
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void animateButtonClick(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> button.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    private void filterContent(String query) {
        if (query.trim().isEmpty()) {
            // Show all content
            for (int i = 0; i < contentContainer.getChildCount(); i++) {
                contentContainer.getChildAt(i).setVisibility(View.VISIBLE);
            }
        } else {
            // Filter content
            String lowerQuery = query.toLowerCase();
            for (int i = 0; i < contentContainer.getChildCount(); i++) {
                View child = contentContainer.getChildAt(i);
                String text = getTextFromView(child).toLowerCase();
                child.setVisibility(text.contains(lowerQuery) ? View.VISIBLE : View.GONE);
            }
        }
    }

    private String getTextFromView(View view) {
        StringBuilder text = new StringBuilder();
        if (view instanceof TextView) {
            text.append(((TextView) view).getText());
        } else if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            for (int i = 0; i < layout.getChildCount(); i++) {
                text.append(getTextFromView(layout.getChildAt(i))).append(" ");
            }
        }
        return text.toString();
    }

    private String getAllTextContent() {
        return "Cipher Shield User Manual. " +
                "Welcome to Cipher Shield. Your files deserve military grade protection. " +
                "Quick Start Guide. Select File, Choose Method, and Encrypt. " +
                "For more details, please read the manual on screen.";
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}