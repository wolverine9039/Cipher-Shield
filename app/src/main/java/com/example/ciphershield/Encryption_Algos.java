package com.example.ciphershield;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import java.io.*;

public class Encryption_Algos extends AppCompatActivity {

    private Uri fileUri;
    private byte[] encryptedData = null;
    private byte[] encryptionKey = null;
    private String encryptionMethod;
    private String fileExtension = "";

    private TextView txtKeyDisplay, txtStatus;
    private Button btnEncrypt, btnCopyKey, btnSaveKey, btnSaveFile, btnShareEncrypted;

    private final ActivityResultLauncher<Intent> saveKeyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    saveBytesToUri(result.getData().getData(), encryptionKey, "Key saved", "Key save failed");
                }
            });

    private final ActivityResultLauncher<Intent> saveEncryptedLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    saveBytesToUri(result.getData().getData(), encryptedData, "File saved", "Save failed");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encryption_algos);

        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnCopyKey = findViewById(R.id.btnCopyKey);
        btnSaveKey = findViewById(R.id.btnSaveKey);
        btnSaveFile = findViewById(R.id.btnSaveFile);
        btnShareEncrypted = findViewById(R.id.btnShareEncrypted);
        txtKeyDisplay = findViewById(R.id.txtKeyDisplay);
        txtStatus = findViewById(R.id.txtStatus);

        Intent intent = getIntent();
        fileUri = Uri.parse(intent.getStringExtra("file_uri"));
        encryptionMethod = intent.getStringExtra("encryption_method");
        fileExtension = getFileExtension(fileUri);

        Button btnhome = findViewById(R.id.btn_home);
        Button btn_exit = findViewById(R.id.btn_exit);

        btn_exit.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        btnhome.setOnClickListener(v -> {
            Intent intent1 = new Intent(Encryption_Algos.this, front.class);
            startActivity(intent1);
        });

        btnEncrypt.setOnClickListener(v -> startEncryption());
        btnCopyKey.setOnClickListener(v -> copyToClipboard(Base64.encodeToString(encryptionKey, Base64.DEFAULT)));
        btnSaveKey.setOnClickListener(v -> saveKeyFile());
        btnSaveFile.setOnClickListener(v -> saveEncryptedFile());
        btnShareEncrypted.setOnClickListener(v -> shareEncryptedFile());
    }

    private void startEncryption() {
        new Thread(() -> {
            try {
                byte[] inputBytes = readBytesFromUri(fileUri);
                if ("HYBRID_RSA_AES".equals(encryptionMethod)) {
                    HybridEncryptionUtil.Result result = HybridEncryptionUtil.encrypt(inputBytes, fileExtension);
                    encryptedData = result.encryptedData;
                    encryptionKey = result.privateKey;
                } else {
                    HuffmanEncryptionUtil.Result result = HuffmanEncryptionUtil.compress(inputBytes, fileExtension);
                    encryptedData = result.encryptedData;
                    encryptionKey = result.treeBytes;
                }
                runOnUiThread(() -> {
                    String fullKey = Base64.encodeToString(encryptionKey, Base64.DEFAULT);
                    // Show only first 4 characters followed by "..."
                    String displayedKey = fullKey.substring(0, Math.min(4, fullKey.length())) + "...";
                    txtKeyDisplay.setText(displayedKey);
                    txtStatus.setText("Encryption complete");
                    btnSaveKey.setEnabled(true);
                    btnSaveFile.setEnabled(true);
                    btnShareEncrypted.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> txtStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void saveKeyFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "encryption_key.key");
        saveKeyLauncher.launch(intent);
    }

    private void saveEncryptedFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "encrypted" +
                (encryptionMethod.equals("HYBRID_RSA_AES") ? ".cyps" : ".huf"));
        saveEncryptedLauncher.launch(intent);
    }

    private void shareEncryptedFile() {
        try {
            File tempFile = File.createTempFile("encrypted", ".tmp", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(encryptedData);
            }
            Uri contentUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", tempFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Encrypted File"));
        } catch (IOException e) {
            Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    private void saveBytesToUri(Uri uri, byte[] data, String successMsg, String errorMsg) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            out.write(data);
            Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Encryption Key", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Full key copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private String getFileExtension(Uri uri) {
        String name = getFileName(uri);
        return name != null && name.contains(".") ?
                name.substring(name.lastIndexOf('.')) : "";
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
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
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}