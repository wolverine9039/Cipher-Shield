package com.example.ciphershield;

import android.animation.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.ciphershield.security.SecureEncryptionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.io.*;

public class ModernDecryptionActivity extends AppCompatActivity {

    private MaterialCardView cardEncryptedFile, cardKeyFile, cardActions;
    private TextView txtSelectedEncrypted, txtSelectedKey, txtStatus, txtVerificationStatus;
    private MaterialButton btnSelectEncrypted, btnSelectKey, btnDecrypt, btnSaveDecrypted, btnPreview;
    private Chip chipVerified, chipWarning;
    private CircularProgressIndicator progressBar;
    private ImageView imgUnlockAnimation;

    private byte[] encryptedData, keyBytes, decryptedData;
    private String originalExtension = "";
    private boolean isPasswordProtected = false;
    private boolean isVerified = false;

    private final ActivityResultLauncher<Intent> encryptedFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleFileSelection(result.getData().getData(), true);
                }
            });

    private final ActivityResultLauncher<Intent> keyFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleFileSelection(result.getData().getData(), false);
                }
            });

    private final ActivityResultLauncher<Intent> saveDecryptedLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    saveDecryptedFile(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_decryption);

        initializeViews();
        setupClickListeners();
        animateInitialEntry();
    }

    private void initializeViews() {
        cardEncryptedFile = findViewById(R.id.cardEncryptedFile);
        cardKeyFile = findViewById(R.id.cardKeyFile);
        cardActions = findViewById(R.id.cardActions);

        txtSelectedEncrypted = findViewById(R.id.txtSelectedEncrypted);
        txtSelectedKey = findViewById(R.id.txtSelectedKey);
        txtStatus = findViewById(R.id.txtStatus);
        txtVerificationStatus = findViewById(R.id.txtVerificationStatus);

        btnSelectEncrypted = findViewById(R.id.btnSelectEncrypted);
        btnSelectKey = findViewById(R.id.btnSelectKey);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        btnSaveDecrypted = findViewById(R.id.btnSaveDecrypted);
        btnPreview = findViewById(R.id.btnPreview);

        chipVerified = findViewById(R.id.chipVerified);
        chipWarning = findViewById(R.id.chipWarning);

        progressBar = findViewById(R.id.progressBar);
        imgUnlockAnimation = findViewById(R.id.imgUnlockAnimation);

        cardActions.setVisibility(View.GONE);
        cardActions.setAlpha(0f);
    }

    private void setupClickListeners() {
        btnSelectEncrypted.setOnClickListener(v -> {
            animateButtonClick(v);
            openFilePicker(true);
        });

        btnSelectKey.setOnClickListener(v -> {
            animateButtonClick(v);
            openFilePicker(false);
        });

        btnDecrypt.setOnClickListener(v -> {
            animateButtonClick(v);
            if (isPasswordProtected) {
                showPasswordDialog();
            } else {
                startDecryption(null);
            }
        });

        btnSaveDecrypted.setOnClickListener(v -> {
            animateButtonClick(v);
            saveDecryptedFileDialog();
        });

        btnPreview.setOnClickListener(v -> {
            animateButtonClick(v);
            previewFile();
        });
    }

    private void animateInitialEntry() {
        animateCardEntry(cardEncryptedFile, 0);
        animateCardEntry(cardKeyFile, 150);
    }

    private void animateCardEntry(View card, int delay) {
        card.setAlpha(0f);
        card.setTranslationY(50f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateButtonClick(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void openFilePicker(boolean isEncryptedFile) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        if (isEncryptedFile) {
            encryptedFileLauncher.launch(intent);
        } else {
            keyFileLauncher.launch(intent);
        }
    }

    private void handleFileSelection(Uri uri, boolean isEncryptedFile) {
        try {
            byte[] data = readBytesFromUri(uri);
            String fileName = getFileName(uri);

            if (isEncryptedFile) {
                encryptedData = data;
                txtSelectedEncrypted.setText(fileName);

                // Detect if password-protected
                if (data.length >= 3) {
                    String header = new String(data, 0, 3);
                    isPasswordProtected = "CP2".equals(header);

                    if (isPasswordProtected) {
                        cardKeyFile.setVisibility(View.GONE);
                        showSnackbar("Password-protected file detected", false);
                    } else {
                        cardKeyFile.setVisibility(View.VISIBLE);
                    }
                }

                animateFileIcon(imgUnlockAnimation);
            } else {
                keyBytes = data;
                txtSelectedKey.setText(fileName);
            }

            // Enable decrypt if ready
            if (encryptedData != null && (isPasswordProtected || keyBytes != null)) {
                btnDecrypt.setEnabled(true);
                animateButtonAppearance(btnDecrypt);
            }

            showSnackbar("File selected: " + fileName, false);

        } catch (IOException e) {
            showSnackbar("Error reading file: " + e.getMessage(), true);
        }
    }

    private void animateFileIcon(ImageView icon) {
        icon.setRotation(45f);
        icon.setAlpha(0f);
        icon.animate()
                .rotation(0f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void animateButtonAppearance(MaterialButton button) {
        button.setAlpha(0f);
        button.setTranslationY(20f);
        button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void showPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_decrypt, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter Decryption Password")
                .setView(dialogView)
                .setPositiveButton("Decrypt", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = passwordInput.getText().toString();

                if (password.isEmpty()) {
                    passwordInput.setError("Password required");
                    shakeView(passwordInput);
                    return;
                }

                dialog.dismiss();
                startDecryption(password);
            });
        });

        dialog.show();
        animateDialogEntry(dialogView);
    }

    private void animateDialogEntry(View view) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void shakeView(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX",
                0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f);
        animator.setDuration(500);
        animator.start();
    }

    private void startDecryption(String password) {
        if (encryptedData == null) {
            showSnackbar("Please select an encrypted file", true);
            return;
        }

        if (!isPasswordProtected && keyBytes == null) {
            showSnackbar("Please select a key file", true);
            return;
        }

        showLoadingState(true);
        animateUnlockIcon();

        new Thread(() -> {
            try {
                SecureEncryptionUtil.DecryptionResult result;

                if (isPasswordProtected) {
                    result = SecureEncryptionUtil.decryptWithPassword(encryptedData, password);
                } else {
                    result = SecureEncryptionUtil.decrypt(encryptedData, keyBytes);
                }

                decryptedData = result.decryptedData;
                originalExtension = result.originalExtension;
                isVerified = result.verified;

                runOnUiThread(() -> {
                    showLoadingState(false);
                    showDecryptionSuccess();
                    updateVerificationStatus();
                    animateActionsCard();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showSnackbar("Decryption failed: " + e.getMessage(), true);
                    shakeView(cardEncryptedFile);
                });
            }
        }).start();
    }

    private void animateUnlockIcon() {
        runOnUiThread(() -> {
            imgUnlockAnimation.setVisibility(View.VISIBLE);

            // Unlock animation
            ObjectAnimator rotate = ObjectAnimator.ofFloat(imgUnlockAnimation, "rotation", 0f, -15f, 15f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(imgUnlockAnimation, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(imgUnlockAnimation, "scaleY", 1f, 1.2f, 1f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(rotate, scaleX, scaleY);
            set.setDuration(800);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
        });
    }

    private void showLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnDecrypt.setEnabled(!loading);

        if (loading) {
            txtStatus.setText("Decrypting your file...");
            txtStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        }
    }

    private void showDecryptionSuccess() {
        txtStatus.setText("✓ Decryption successful!");
        txtStatus.setTextColor(getColor(android.R.color.holo_green_dark));

        txtStatus.setScaleX(0.5f);
        txtStatus.setScaleY(0.5f);
        txtStatus.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();

        showSnackbar("File decrypted successfully! Extension: " + originalExtension, false);
    }

    private void updateVerificationStatus() {
        if (isVerified) {
            chipVerified.setVisibility(View.VISIBLE);
            chipWarning.setVisibility(View.GONE);
            txtVerificationStatus.setText("File integrity verified ✓");
            txtVerificationStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            chipVerified.setVisibility(View.GONE);
            chipWarning.setVisibility(View.VISIBLE);
            txtVerificationStatus.setText("⚠ Warning: File may be corrupted or tampered");
            txtVerificationStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        }

        animateChipAppearance(isVerified ? chipVerified : chipWarning);
    }

    private void animateChipAppearance(Chip chip) {
        chip.setScaleX(0f);
        chip.setScaleY(0f);
        chip.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void animateActionsCard() {
        cardActions.setVisibility(View.VISIBLE);
        cardActions.setAlpha(0f);
        cardActions.setTranslationY(50f);

        cardActions.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void saveDecryptedFileDialog() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "decrypted_" + System.currentTimeMillis() + originalExtension);
        saveDecryptedLauncher.launch(intent);
    }

    private void saveDecryptedFile(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            out.write(decryptedData);
            showSnackbar("Decrypted file saved successfully", false);
            animateSuccessPulse(btnSaveDecrypted);
        } catch (IOException e) {
            showSnackbar("Failed to save: " + e.getMessage(), true);
        }
    }

    private void previewFile() {
        try {
            File tempFile = new File(getCacheDir(), "preview" + originalExtension);

            // Securely write to temp file
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decryptedData);
            }

            Uri contentUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", tempFile);

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, getMimeType(originalExtension));
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(viewIntent, "Preview with"));

            // Schedule secure deletion
            scheduleSecureDeletion(tempFile);

        } catch (Exception e) {
            showSnackbar("Preview failed: " + e.getMessage(), true);
        }
    }

    private void scheduleSecureDeletion(File file) {
        // Delete temp file after 5 minutes
        new Thread(() -> {
            try {
                Thread.sleep(300000); // 5 minutes
                if (file.exists()) {
                    // Overwrite with random data before deletion
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] random = new byte[(int) file.length()];
                        new java.security.SecureRandom().nextBytes(random);
                        fos.write(random);
                    }
                    file.delete();
                }
            } catch (Exception e) {
                // Silent cleanup failure
            }
        }).start();
    }

    private void animateSuccessPulse(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(300);
        set.start();
    }

    private void showSnackbar(String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message,
                isError ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);

        if (isError) {
            snackbar.setBackgroundTint(getColor(android.R.color.holo_red_light));
        } else {
            snackbar.setBackgroundTint(getColor(android.R.color.holo_green_dark));
        }

        snackbar.show();
    }

    private String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case ".txt": return "text/plain";
            case ".pdf": return "application/pdf";
            case ".jpg": case ".jpeg": return "image/jpeg";
            case ".png": return "image/png";
            case ".mp4": return "video/mp4";
            case ".mp3": return "audio/mpeg";
            case ".zip": return "application/zip";
            case ".doc": case ".docx": return "application/msword";
            default: return "*/*";
        }
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Secure cleanup
        if (decryptedData != null) {
            java.util.Arrays.fill(decryptedData, (byte) 0);
        }
        if (keyBytes != null) {
            java.util.Arrays.fill(keyBytes, (byte) 0);
        }
    }
}