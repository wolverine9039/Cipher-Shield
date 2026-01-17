package com.example.ciphershield;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class UserManual extends AppCompatActivity {

    private TextView manualText;
    private SearchView searchView;
    private LinearLayout faqContainer;
    private Button btnTTS;
    private TextToSpeech tts;
    private boolean isSpeaking = false;

    private final String manualContent =
            "📘 Cipher Shield - User Manual\n\n" +
                    "🔐 Encrypting:\n• Tap 'Encrypt File'.\n• Select a file.\n• Generates .cyps file & private key.\n\n" +
                    "🔓 Decrypting:\n• Tap 'Decrypt File'.\n• Select .cyps file & key.\n• File will be restored.\n\n" +
                    "📁 File Storage:\n• Encrypted: /EncryptedFiles\n• Keys: /CipherShieldKeys\n• Decrypted: /DecryptedFiles\n\n" +
                    "⚠️ Tips:\n• Don’t lose your key.\n• .cyps files work only in this app.\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);

        manualText = findViewById(R.id.manual_text);
        searchView = findViewById(R.id.searchView);

        btnTTS = findViewById(R.id.btn_tts);

        manualText.setText(manualContent);

        setupSearch();
        setupTTS();
        loadFAQFromJson();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String text) {
                manualText.setText(filterManualContent(text));
                return true;
            }
        });
    }

    private String filterManualContent(String keyword) {
        if (keyword.trim().isEmpty()) return manualContent;
        StringBuilder filtered = new StringBuilder("📘 Search Result:\n\n");
        for (String line : manualContent.split("\n")) {
            if (line.toLowerCase().contains(keyword.toLowerCase()))
                filtered.append("• ").append(line).append("\n");
        }
        return filtered.length() <= 22 ? "❌ No results found." : filtered.toString();
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {});
        btnTTS.setOnClickListener(v -> {
            if (isSpeaking) {
                tts.stop();
                isSpeaking = false;
                btnTTS.setText("🔊 Read Manual Aloud");
            } else {
                tts.speak(manualText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                isSpeaking = true;
                btnTTS.setText("⏹ Stop Reading");
            }
        });
    }

    private void loadFAQFromJson() {
        try {
            InputStream is = getAssets().open("faq.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONArray faqArray = new JSONArray(new String(buffer, StandardCharsets.UTF_8));

            for (int i = 0; i < faqArray.length(); i++) {
                JSONObject obj = faqArray.getJSONObject(i);
                addFAQEntry(
                        obj.getString("question"),
                        obj.getString("answer"),
                        obj.has("action") ? obj.getString("action") : null
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFAQEntry(String question, String answer, String action) {
        View faqItem = LayoutInflater.from(this).inflate(R.layout.faq_item, faqContainer, false);

        TextView txtQ = faqItem.findViewById(R.id.txt_faq_question);
        TextView txtA = faqItem.findViewById(R.id.txt_faq_answer);
        Button btn = faqItem.findViewById(R.id.btn_faq_action);

        txtQ.setText("❓ " + question);
        txtA.setText(answer);
        txtA.setVisibility(View.GONE);
        if (action != null) btn.setVisibility(View.VISIBLE);

        txtQ.setOnClickListener(v -> {
            txtA.setVisibility(txtA.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (btn.getVisibility() == View.VISIBLE)
                btn.setVisibility(btn.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        btn.setOnClickListener(v -> {
            if ("open_encrypt".equals(action)) {
                startActivity(new Intent(this, ModernDecryptionActivity.class));
            } else if ("open_decrypt".equals(action)) {
                startActivity(new Intent(this, ModernDecryptionActivity.class));
            }
        });

        faqContainer.addView(faqItem);
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
