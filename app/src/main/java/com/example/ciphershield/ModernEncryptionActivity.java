package com.example.ciphershield;

import android.animation.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import com.example.ciphershield.security.SecureEncryptionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.*;

public class ModernEncryptionActivity extends AppCompatActivity {

    private MaterialCardView cardFileSelection, cardEncryptionMethod, cardActions;
    private TextView txtSelectedFile, txtFileSize, txtStatus;
    private MaterialButton btnSelectFile, btnEncrypt, btnSaveKey, btnSaveFile, btnUsePassword;
    private Chip chipKeyMode, chipPasswordMode;
    private CircularProgressIndicator progressBar;
    private ImageView imgFileIcon, imgLockAnimation;

    private Uri selectedFileUri = null;
    private byte[] encryptedData = null;
    private byte[] encryptionKey = null;
    private String fileExtension = "";
    private boolean usePasswordMode = false;
    private String encryptionPassword = null;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleFileSelected(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<Intent> saveKeyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    saveBytesToUri(result.getData().getData(), encryptionKey, "Key saved securely");
                }
            });

    private final ActivityResultLauncher<Intent> saveEncryptedLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    saveBytesToUri(result.getData().getData(), encryptedData, "Encrypted file saved");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_encryption);

        initializeViews();
        setupClickListeners();
        animateInitialEntry();
    }

    private void initializeViews() {
        cardFileSelection = findViewById(R.id.cardFileSelection);
        cardEncryptionMethod = findViewById(R.id.cardEncryptionMethod);
        cardActions = findViewById(R.id.cardActions);

        txtSelectedFile = findViewById(R.id.txtSelectedFile);
        txtFileSize = findViewById(R.id.txtFileSize);
        txtStatus = findViewById(R.id.txtStatus);

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnSaveKey = findViewById(R.id.btnSaveKey);
        btnSaveFile = findViewById(R.id.btnSaveFile);
        btnUsePassword = findViewById(R.id.btnUsePassword);

        chipKeyMode = findViewById(R.id.chipKeyMode);
        chipPasswordMode = findViewById(R.id.chipPasswordMode);

        progressBar = findViewById(R.id.progressBar);
        imgFileIcon = findViewById(R.id.imgFileIcon);
        imgLockAnimation = findViewById(R.id.imgLockAnimation);

        // Initially hide action cards
        cardActions.setVisibility(View.GONE);
        cardActions.setAlpha(0f);
    }

    private void setupClickListeners() {
        btnSelectFile.setOnClickListener(v -> {
            animateButtonClick(v);
            openFilePicker();
        });

        chipKeyMode.setOnClickListener(v -> {
            usePasswordMode = false;
            animateChipSelection(chipKeyMode, chipPasswordMode);
            updateEncryptionModeUI();
        });

        chipPasswordMode.setOnClickListener(v -> {
            usePasswordMode = true;
            animateChipSelection(chipPasswordMode, chipKeyMode);
            updateEncryptionModeUI();
        });

        btnEncrypt.setOnClickListener(v -> {
            animateButtonClick(v);
            if (usePasswordMode) {
                showPasswordDialog();
            } else {
                startEncryption();
            }
        });

        btnSaveKey.setOnClickListener(v -> {
            animateButtonClick(v);
            saveKeyFile();
        });

        btnSaveFile.setOnClickListener(v -> {
            animateButtonClick(v);
            saveEncryptedFile();
        });
    }

    private void animateInitialEntry() {
        // Stagger animation for cards
        animateCardEntry(cardFileSelection, 0);
        animateCardEntry(cardEncryptionMethod, 150);
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
        // Scale animation for button press
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

    private void animateChipSelection(Chip selected, Chip unselected) {
        // Animate selection
        selected.setChecked(true);
        unselected.setChecked(false);

        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(selected, "scaleX", 1f, 1.1f, 1f);
        scaleUp.setDuration(200);
        scaleUp.start();
    }

    private void updateEncryptionModeUI() {
        if (usePasswordMode) {
            btnUsePassword.setVisibility(View.VISIBLE);
            btnSaveKey.setVisibility(View.GONE);
            animateFadeIn(btnUsePassword);
        } else {
            btnUsePassword.setVisibility(View.GONE);
            btnSaveKey.setVisibility(View.VISIBLE);
        }
    }

    private void animateFadeIn(View view) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelected(Uri uri) {
        selectedFileUri = uri;
        String fileName = getFileName(uri);
        long fileSize = getFileSize(uri);
        fileExtension = getFileExtension(uri);

        // Update UI with animation
        txtSelectedFile.setText(fileName != null ? fileName : "Unknown File");
        txtFileSize.setText(formatFileSize(fileSize));

        // Animate file icon
        animateFileIcon();

        // Enable encryption button
        btnEncrypt.setEnabled(true);
        animateButtonAppearance(btnEncrypt);

        // Show success message
        showSnackbar("File selected: " + fileName, false);
    }

    private void animateFileIcon() {
        imgFileIcon.setScaleX(0f);
        imgFileIcon.setScaleY(0f);
        imgFileIcon.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password, null);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.passwordInputLayout);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        TextInputLayout confirmLayout = dialogView.findViewById(R.id.confirmPasswordLayout);
        TextInputEditText confirmInput = dialogView.findViewById(R.id.confirmPasswordInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set Encryption Password")
                .setView(dialogView)
                .setPositiveButton("Encrypt", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = passwordInput.getText().toString();
                String confirm = confirmInput.getText().toString();

                if (password.length() < 8) {
                    passwordLayout.setError("Password must be at least 8 characters");
                    shakeView(passwordInput);
                    return;
                }

                if (!password.equals(confirm)) {
                    confirmLayout.setError("Passwords do not match");
                    shakeView(confirmInput);
                    return;
                }

                encryptionPassword = password;
                dialog.dismiss();
                startEncryption();
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

    private void startEncryption() {
        if (selectedFileUri == null) {
            showSnackbar("Please select a file first", true);
            return;
        }

        // Show loading state
        showLoadingState(true);
        animateLockIcon();

        new Thread(() -> {
            try {
                byte[] inputBytes = readBytesFromUri(selectedFileUri);

                SecureEncryptionUtil.EncryptionResult result;
                if (usePasswordMode && encryptionPassword != null) {
                    result = SecureEncryptionUtil.encryptWithPassword(
                            inputBytes, encryptionPassword, fileExtension
                    );
                } else {
                    result = SecureEncryptionUtil.encrypt(inputBytes, fileExtension);
                }

                encryptedData = result.encryptedData;
                encryptionKey = result.privateKey;

                runOnUiThread(() -> {
                    showLoadingState(false);
                    showEncryptionSuccess(result.checksum);
                    animateActionsCard();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showSnackbar("Encryption failed: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void animateLockIcon() {
        runOnUiThread(() -> {
            imgLockAnimation.setVisibility(View.VISIBLE);

            // Rotate and scale animation
            ObjectAnimator rotate = ObjectAnimator.ofFloat(imgLockAnimation, "rotation", 0f, 360f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(imgLockAnimation, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(imgLockAnimation, "scaleY", 1f, 1.2f, 1f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(rotate, scaleX, scaleY);
            set.setDuration(1000);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
        });
    }

    private void showLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEncrypt.setEnabled(!loading);

        if (loading) {
            txtStatus.setText("Encrypting your file securely...");
            txtStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        }
    }

    private void showEncryptionSuccess(String checksum) {
        txtStatus.setText("✓ Encryption successful!");
        txtStatus.setTextColor(getColor(android.R.color.holo_green_dark));

        // Animate status text
        txtStatus.setScaleX(0.5f);
        txtStatus.setScaleY(0.5f);
        txtStatus.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();

        showSnackbar("File encrypted successfully! Checksum: " + checksum.substring(0, 8) + "...", false);
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

    private void saveKeyFile() {
        if (encryptionKey == null) {
            showSnackbar("No encryption key available", true);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "encryption_key_" + System.currentTimeMillis() + ".key");
        saveKeyLauncher.launch(intent);
    }

    private void saveEncryptedFile() {
        if (encryptedData == null) {
            showSnackbar("No encrypted data available", true);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/octet-stream");
        String extension = usePasswordMode ? ".csp" : ".csk";
        intent.putExtra(Intent.EXTRA_TITLE, "encrypted_" + System.currentTimeMillis() + extension);
        saveEncryptedLauncher.launch(intent);
    }

    private void saveBytesToUri(Uri uri, byte[] data, String successMsg) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            out.write(data);
            showSnackbar(successMsg, false);
            animateSuccessCheck();
        } catch (IOException e) {
            showSnackbar("Failed to save: " + e.getMessage(), true);
        }
    }

    private void animateSuccessCheck() {
        // Create a checkmark animation overlay
        ImageView checkmark = new ImageView(this);
        checkmark.setImageResource(android.R.drawable.ic_menu_save);
        // Add to layout and animate
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

    // Helper methods

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

    private long getFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) return cursor.getLong(idx);
            }
        }
        return 0;
    }

    private String getFileExtension(Uri uri) {
        String name = getFileName(uri);
        return (name != null && name.contains(".")) ?
                name.substring(name.lastIndexOf('.')) : "";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Secure cleanup
        if (encryptionKey != null) {
            java.util.Arrays.fill(encryptionKey, (byte) 0);
        }
        if (encryptionPassword != null) {
            encryptionPassword = null;
        }
    }
}