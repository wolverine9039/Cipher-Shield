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
import com.example.ciphershield.security.ChunkedEncryptionUtil;
import com.example.ciphershield.security.SecureEncryptionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.io.*;
import java.nio.ByteBuffer;

public class ModernDecryptionActivity extends AppCompatActivity {

    private MaterialCardView cardEncryptedFile, cardKeyFile, cardActions;
    private TextView txtSelectedEncrypted, txtSelectedKey, txtStatus, txtVerificationStatus, txtProgress;
    private MaterialButton btnSelectEncrypted, btnSelectKey, btnDecrypt, btnSaveDecrypted, btnPreview;
    private Chip chipVerified, chipWarning;
    private CircularProgressIndicator progressBar;
    private LinearProgressIndicator linearProgress;
    private ImageView imgUnlockAnimation;

    private Uri encryptedFileUri = null;
    private byte[] encryptedData = null;
    private byte[] keyBytes = null;
    private byte[] decryptedData = null;
    private Uri decryptedFileUri = null;
    private File tempDecryptedFile = null;
    private String originalExtension = "";
    private boolean isPasswordProtected = false;
    private boolean isVerified = false;
    private boolean isLargeFile = false;

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
        txtProgress = findViewById(R.id.txtProgress);

        btnSelectEncrypted = findViewById(R.id.btnSelectEncrypted);
        btnSelectKey = findViewById(R.id.btnSelectKey);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        btnSaveDecrypted = findViewById(R.id.btnSaveDecrypted);
        btnPreview = findViewById(R.id.btnPreview);

        chipVerified = findViewById(R.id.chipVerified);
        chipWarning = findViewById(R.id.chipWarning);

        progressBar = findViewById(R.id.progressBar);
        linearProgress = findViewById(R.id.linearProgress);
        imgUnlockAnimation = findViewById(R.id.imgUnlockAnimation);

        cardActions.setVisibility(View.GONE);
        cardActions.setAlpha(0f);

        if (linearProgress != null) {
            linearProgress.setVisibility(View.GONE);
        }
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
                .withEndAction(() -> button.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
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
            String fileName = getFileName(uri);
            long fileSize = getFileSize(uri);

            if (isEncryptedFile) {
                encryptedFileUri = uri;
                isLargeFile = fileSize > (10 * 1024 * 1024);

                txtSelectedEncrypted.setText(fileName + " (" + formatFileSize(fileSize) + ")");

                // Read header to detect file type
                byte[] header = readHeaderBytes(uri, 3);
                if (header != null && header.length >= 3) {
                    String headerStr = new String(header, 0, 3);
                    isPasswordProtected = "CP2".equals(headerStr);
                    boolean isChunkedFile = "CL2".equals(headerStr);

                    if (isPasswordProtected) {
                        cardKeyFile.setVisibility(View.GONE);
                        showSnackbar("Password-protected file detected", false);
                    } else {
                        cardKeyFile.setVisibility(View.VISIBLE);
                    }

                    if (isChunkedFile || isLargeFile) {
                        txtStatus.setText("📊 Large file - optimized decryption");
                        txtStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                    }
                }

                // Only load small non-password files
                if (!isLargeFile && !isPasswordProtected) {
                    encryptedData = readBytesFromUri(uri);
                }

                animateFileIcon(imgUnlockAnimation);
            } else {
                // KEY FILE
                if (fileSize > (1024 * 1024)) {
                    showSnackbar("Key file too large (" + formatFileSize(fileSize) +
                            "). Keys are typically < 10KB.", true);
                    return;
                }

                keyBytes = readBytesFromUri(uri);
                txtSelectedKey.setText(fileName + " (" + formatFileSize(fileSize) + ")");
            }

            if (encryptedFileUri != null && (isPasswordProtected || keyBytes != null)) {
                btnDecrypt.setEnabled(true);
                animateButtonAppearance(btnDecrypt);
            }

            showSnackbar("File selected: " + fileName, false);

        } catch (OutOfMemoryError e) {
            showSnackbar("File too large to load into memory!", true);
        } catch (IOException e) {
            showSnackbar("Error reading file: " + e.getMessage(), true);
        }
    }

    private byte[] readHeaderBytes(Uri uri, int numBytes) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            byte[] header = new byte[numBytes];
            int read = in.read(header);
            return read == numBytes ? header : null;
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
        button.animate().alpha(1f).translationY(0f).setDuration(300).start();
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
        view.animate().alpha(1f).translationY(0f).setDuration(300).start();
    }

    private void shakeView(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX",
                0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f);
        animator.setDuration(500);
        animator.start();
    }

    private void startDecryption(String password) {
        if (encryptedFileUri == null) {
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
                if (isLargeFile || needsChunkedDecryption(encryptedFileUri)) {
                    decryptLargeFile(password);
                } else {
                    decryptStandardFile(password);
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showSnackbar("Decryption failed: " + e.getMessage(), true);
                    shakeView(cardEncryptedFile);
                });
            }
        }).start();
    }

    private boolean needsChunkedDecryption(Uri uri) {
        try {
            byte[] header = readHeaderBytes(uri, 3);
            if (header != null && header.length >= 3) {
                return "CL2".equals(new String(header, 0, 3));
            }
        } catch (IOException e) {
            // Ignore
        }
        return false;
    }

    private void decryptStandardFile(String password) throws Exception {
        if (encryptedData == null) {
            long fileSize = getFileSize(encryptedFileUri);
            if (fileSize > (10 * 1024 * 1024)) {
                throw new Exception("File too large for standard decryption");
            }
            encryptedData = readBytesFromUri(encryptedFileUri);
        }

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
    }

    private void decryptLargeFile(String password) throws Exception {
        runOnUiThread(() -> {
            if (linearProgress != null) {
                linearProgress.setVisibility(View.VISIBLE);
                linearProgress.setProgress(0);
            }
            if (txtProgress != null) {
                txtProgress.setVisibility(View.VISIBLE);
            }
        });

        // Extract extension from file header
        String extractedExtension = extractExtensionFromHeader(encryptedFileUri);
        final String fileExtension = extractedExtension.isEmpty() ? ".bin" : extractedExtension;

        // Create temp output file with proper extension
        File outputFile = File.createTempFile("decrypted_", fileExtension, getCacheDir());
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
                    txtStatus.setText("Decrypting: " + formatFileSize(bytesProcessed) +
                            " / " + formatFileSize(totalBytes));
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showDecryptionSuccess();
                    updateVerificationStatus();
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
                    showSnackbar("Decryption failed: " + e.getMessage(), true);
                    if (linearProgress != null) {
                        linearProgress.setVisibility(View.GONE);
                    }
                });
            }
        };

        ChunkedEncryptionUtil.decryptLargeFile(
                this, encryptedFileUri, outputUri, keyBytes, callback
        );

        decryptedFileUri = outputUri;
        tempDecryptedFile = outputFile;
        originalExtension = fileExtension;
        isVerified = true;
    }

    private String extractExtensionFromHeader(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return "";

            // Read version (3 bytes)
            in.skip(3);

            // Read and skip salt
            byte[] saltLenBytes = new byte[4];
            in.read(saltLenBytes);
            int saltLen = ByteBuffer.wrap(saltLenBytes).getInt();
            in.skip(saltLen);

            // Read and skip IV
            byte[] ivLenBytes = new byte[4];
            in.read(ivLenBytes);
            int ivLen = ByteBuffer.wrap(ivLenBytes).getInt();
            in.skip(ivLen);

            // Read extension
            byte[] extLenBytes = new byte[4];
            in.read(extLenBytes);
            int extLen = ByteBuffer.wrap(extLenBytes).getInt();

            if (extLen > 0 && extLen < 20) {
                byte[] extBytes = new byte[extLen];
                in.read(extBytes);
                return new String(extBytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            android.util.Log.w("Decryption", "Could not extract extension: " + e.getMessage());
        }
        return "";
    }

    private void animateUnlockIcon() {
        runOnUiThread(() -> {
            imgUnlockAnimation.setVisibility(View.VISIBLE);
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

        showSnackbar("Decrypted! Original extension: " + originalExtension, false);
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
            txtVerificationStatus.setText("⚠ Warning: File may be corrupted");
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

        // Suggest filename with original extension
        String suggestedName = "decrypted_" + System.currentTimeMillis() + originalExtension;
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);

        saveDecryptedLauncher.launch(intent);
    }

    private void saveDecryptedFile(Uri uri) {
        try {
            if (isLargeFile && decryptedFileUri != null) {
                try (InputStream in = new FileInputStream(new File(decryptedFileUri.getPath()));
                     OutputStream out = getContentResolver().openOutputStream(uri)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    long totalWritten = 0;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                        totalWritten += len;
                    }
                    showSnackbar("Saved (" + formatFileSize(totalWritten) + ") " + originalExtension, false);
                }
            } else {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    out.write(decryptedData);
                    showSnackbar("Saved (" + formatFileSize(decryptedData.length) + ") " + originalExtension, false);
                }
            }
            animateSuccessPulse(btnSaveDecrypted);
        } catch (IOException e) {
            showSnackbar("Failed to save: " + e.getMessage(), true);
        }
    }

    private void previewFile() {
        try {
            File previewFile = new File(getCacheDir(), "preview" + originalExtension);

            if (isLargeFile && decryptedFileUri != null) {
                try (InputStream in = new FileInputStream(new File(decryptedFileUri.getPath()));
                     FileOutputStream fos = new FileOutputStream(previewFile)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            } else {
                try (FileOutputStream fos = new FileOutputStream(previewFile)) {
                    fos.write(decryptedData);
                }
            }

            Uri contentUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", previewFile);

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, getMimeType(originalExtension));
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(viewIntent, "Preview with"));
            scheduleSecureDeletion(previewFile);

        } catch (Exception e) {
            showSnackbar("Preview failed: " + e.getMessage(), true);
        }
    }

    private void scheduleSecureDeletion(File file) {
        new Thread(() -> {
            try {
                Thread.sleep(300000); // 5 minutes
                if (file.exists()) {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] random = new byte[(int) file.length()];
                        new java.security.SecureRandom().nextBytes(random);
                        fos.write(random);
                    }
                    file.delete();
                }
            } catch (Exception e) {
                // Silent cleanup
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
        snackbar.setBackgroundTint(getColor(isError ?
                android.R.color.holo_red_light : android.R.color.holo_green_dark));
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

        if (tempDecryptedFile != null && tempDecryptedFile.exists()) {
            tempDecryptedFile.delete();
        }

        if (decryptedData != null) {
            java.util.Arrays.fill(decryptedData, (byte) 0);
        }
        if (keyBytes != null) {
            java.util.Arrays.fill(keyBytes, (byte) 0);
        }
        if (encryptedData != null) {
            java.util.Arrays.fill(encryptedData, (byte) 0);
        }
    }
}