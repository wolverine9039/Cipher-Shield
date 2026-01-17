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

import com.example.ciphershield.security.ChunkedEncryptionUtil;
import com.example.ciphershield.security.SecureEncryptionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.*;

public class ModernEncryptionActivity extends AppCompatActivity {

    private MaterialCardView cardFileSelection, cardEncryptionMethod, cardActions;
    private TextView txtSelectedFile, txtFileSize, txtStatus, txtProgress;
    private MaterialButton btnSelectFile, btnEncrypt, btnSaveKey, btnSaveFile, btnUsePassword, btnHome, btnExit;
    private Chip chipKeyMode, chipPasswordMode;
    private CircularProgressIndicator progressBar;
    private LinearProgressIndicator linearProgress;
    private ImageView imgFileIcon, imgLockAnimation;
    private boolean isLargeFile = false;

    private Uri selectedFileUri = null;
    private byte[] encryptedData = null;
    private Uri encryptedFileUri = null;
    private File tempEncryptedFile = null;
    private byte[] encryptionKey = null;
    private String originalFileName = "";
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
                    saveKeyToUri(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<Intent> saveEncryptedLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    saveEncryptedToUri(result.getData().getData());
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
        txtProgress = findViewById(R.id.txtProgress);

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnSaveKey = findViewById(R.id.btnSaveKey);
        btnSaveFile = findViewById(R.id.btnSaveFile);
        btnUsePassword = findViewById(R.id.btnUsePassword);
        btnHome = findViewById(R.id.btnHome);
        btnExit = findViewById(R.id.btnExit);

        chipKeyMode = findViewById(R.id.chipKeyMode);
        chipPasswordMode = findViewById(R.id.chipPasswordMode);

        progressBar = findViewById(R.id.progressBar);
        linearProgress = findViewById(R.id.linearProgress);
        imgFileIcon = findViewById(R.id.imgFileIcon);
        imgLockAnimation = findViewById(R.id.imgLockAnimation);

        cardActions.setVisibility(View.GONE);
        cardActions.setAlpha(0f);

        if (linearProgress != null) {
            linearProgress.setVisibility(View.GONE);
        }
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

        btnHome.setOnClickListener(v -> {
            animateButtonClick(v);
            finish();
        });

        btnExit.setOnClickListener(v -> {
            animateButtonClick(v);
            finishAffinity();
        });
    }

    private void animateInitialEntry() {
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
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> button.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    private void animateChipSelection(Chip selected, Chip unselected) {
        selected.setChecked(true);
        unselected.setChecked(false);
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(selected, "scaleX", 1f, 1.1f, 1f);
        scaleUp.setDuration(200);
        scaleUp.start();
    }

    private void updateEncryptionModeUI() {
        if (usePasswordMode) {
            btnSaveKey.setVisibility(View.GONE);
        } else {
            btnSaveKey.setVisibility(View.VISIBLE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelected(Uri uri) {
        selectedFileUri = uri;
        originalFileName = getFileName(uri);
        long fileSize = getFileSize(uri);

        // Extract extension properly
        fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        }

        isLargeFile = fileSize > (10 * 1024 * 1024); // 10MB

        txtSelectedFile.setText(originalFileName != null ? originalFileName : "Unknown File");
        txtFileSize.setText(formatFileSize(fileSize));

        if (isLargeFile) {
            txtFileSize.append(" • Large file - chunked encryption");
            txtStatus.setText("📊 Optimized encryption for large file");
            txtStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        }

        animateFileIcon();
        btnEncrypt.setEnabled(true);
        animateButtonAppearance(btnEncrypt);

        showSnackbar("Selected: " + originalFileName + " (" + fileExtension + ")", false);
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
        button.animate().alpha(1f).translationY(0f).setDuration(300).start();
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
        view.animate().alpha(1f).translationY(0f).setDuration(300).start();
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

        showLoadingState(true);
        animateLockIcon();

        new Thread(() -> {
            try {
                if (isLargeFile) {
                    encryptLargeFile();
                } else {
                    encryptStandardFile();
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showSnackbar("Encryption failed: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void encryptStandardFile() throws Exception {
        byte[] inputBytes = readBytesFromUri(selectedFileUri);

        SecureEncryptionUtil.EncryptionResult result;

        // Pass the extension (including the dot)
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
    }

    private void encryptLargeFile() throws Exception {
        runOnUiThread(() -> {
            if (linearProgress != null) {
                linearProgress.setVisibility(View.VISIBLE);
                linearProgress.setProgress(0);
            }
            if (txtProgress != null) {
                txtProgress.setVisibility(View.VISIBLE);
            }
        });

        File outputFile = File.createTempFile("encrypted_", ".csk", getCacheDir());
        Uri outputUri = Uri.fromFile(outputFile);

        ChunkedEncryptionUtil.ProgressCallback callback = new ChunkedEncryptionUtil.ProgressCallback() {
            @Override
            public void onProgress(int percentage, long bytesProcessed, long totalBytes) {
                runOnUiThread(() -> {
                    if (linearProgress != null) {
                        linearProgress.setProgress(percentage);
                    }
                    if (txtProgress != null) {
                        txtProgress.setText("Progress: " + percentage + "%");
                    }
                    txtStatus.setText("Encrypting: " + formatFileSize(bytesProcessed) +
                            " / " + formatFileSize(totalBytes));
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showEncryptionSuccess("Large file");
                    animateActionsCard();
                    if (linearProgress != null) {
                        linearProgress.setVisibility(View.GONE);
                    }
                    if (txtProgress != null) {
                        txtProgress.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showSnackbar("Encryption failed: " + e.getMessage(), true);
                    if (linearProgress != null) {
                        linearProgress.setVisibility(View.GONE);
                    }
                });
            }
        };

        // Pass the extension to chunked encryption
        ChunkedEncryptionUtil.EncryptionResult result =
                ChunkedEncryptionUtil.encryptLargeFile(
                        this, selectedFileUri, outputUri, fileExtension, callback
                );

        encryptedFileUri = result.encryptedFileUri;
        encryptionKey = result.privateKey;
        tempEncryptedFile = outputFile;
    }

    private void animateLockIcon() {
        runOnUiThread(() -> {
            imgLockAnimation.setVisibility(View.VISIBLE);
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

        txtStatus.setScaleX(0.5f);
        txtStatus.setScaleY(0.5f);
        txtStatus.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator())
                .start();

        showSnackbar("Encrypted! Extension preserved: " + fileExtension, false);
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
        intent.putExtra(Intent.EXTRA_TITLE, "key_" + System.currentTimeMillis() + ".key");
        saveKeyLauncher.launch(intent);
    }

    private void saveKeyToUri(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            out.write(encryptionKey);
            showSnackbar("Key saved (" + formatFileSize(encryptionKey.length) + ")", false);
        } catch (IOException e) {
            showSnackbar("Failed to save key: " + e.getMessage(), true);
        }
    }

    private void saveEncryptedFile() {
        if (encryptedData == null && encryptedFileUri == null) {
            showSnackbar("No encrypted data available", true);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/octet-stream");
        String extension = usePasswordMode ? ".csp" : ".csk";
        intent.putExtra(Intent.EXTRA_TITLE, "encrypted_" + System.currentTimeMillis() + extension);
        saveEncryptedLauncher.launch(intent);
    }

    private void saveEncryptedToUri(Uri uri) {
        try {
            if (isLargeFile && encryptedFileUri != null) {
                try (InputStream in = new FileInputStream(new File(encryptedFileUri.getPath()));
                     OutputStream out = getContentResolver().openOutputStream(uri)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    long totalWritten = 0;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                        totalWritten += len;
                    }
                    showSnackbar("Encrypted file saved (" + formatFileSize(totalWritten) + ")", false);
                }
            } else {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    out.write(encryptedData);
                    showSnackbar("Encrypted file saved (" + formatFileSize(encryptedData.length) + ")", false);
                }
            }
        } catch (IOException e) {
            showSnackbar("Failed to save: " + e.getMessage(), true);
        }
    }

    private void showSnackbar(String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message,
                isError ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(isError ?
                android.R.color.holo_red_light : android.R.color.holo_green_dark));
        snackbar.show();
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

    private long getFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) return cursor.getLong(idx);
            }
        }
        return 0;
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

        if (tempEncryptedFile != null && tempEncryptedFile.exists()) {
            tempEncryptedFile.delete();
        }

        if (encryptionKey != null) {
            java.util.Arrays.fill(encryptionKey, (byte) 0);
        }
        if (encryptionPassword != null) {
            encryptionPassword = null;
        }
    }
}