package com.example.ciphershield;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.*;

public class Decryption extends AppCompatActivity {

    private TextView txtSelectedEncrypted, txtSelectedKey, txtStatus;
    private Button btnSelectEncrypted, btnSelectKey, btnDecrypt, btnSaveDecrypted, btnPreview;
    private byte[] encryptedData, keyBytes, decryptedData;
    private String originalExtension = "";

    private final ActivityResultLauncher<Intent> encryptedFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFileSelection(result.getData().getData(), true);
                }
            });

    private final ActivityResultLauncher<Intent> keyFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFileSelection(result.getData().getData(), false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryption);

        txtSelectedEncrypted = findViewById(R.id.txt_selected_encrypted);
        txtSelectedKey = findViewById(R.id.txt_selected_key);
        txtStatus = findViewById(R.id.txtStatus);
        btnSelectEncrypted = findViewById(R.id.btnSelectEncryptedFile);
        btnSelectKey = findViewById(R.id.btnSelectKeyFile);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        btnSaveDecrypted = findViewById(R.id.btnSaveDecrypted);
        btnPreview = findViewById(R.id.btnPreview);

        btnSelectEncrypted.setOnClickListener(v -> openFilePicker(true));
        btnSelectKey.setOnClickListener(v -> openFilePicker(false));
        btnDecrypt.setOnClickListener(v -> decryptFile());
        btnSaveDecrypted.setOnClickListener(v -> saveDecryptedFile());
        btnPreview.setOnClickListener(v -> previewFile());
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
            if (isEncryptedFile) {
                encryptedData = data;
                txtSelectedEncrypted.setText("Encrypted: " + getFileName(uri));
            } else {
                keyBytes = data;
                txtSelectedKey.setText("Key: " + getFileName(uri));
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptFile() {
        if (encryptedData == null || keyBytes == null) {
            Toast.makeText(this, "Please select both files first", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                DecryptionUtil.Result result = DecryptionUtil.decrypt(encryptedData, keyBytes);
                decryptedData = result.decryptedBytes;
                originalExtension = result.originalExtension;

                runOnUiThread(() -> {
                    txtStatus.setText("Decrypted! Original extension: " + originalExtension);
                    btnSaveDecrypted.setEnabled(true);
                    btnPreview.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        txtStatus.setText("Decryption failed: " + e.getMessage()));
            }
        }).start();
    }

    private void saveDecryptedFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "decrypted" + originalExtension);
        startActivityForResult(intent, 1);
    }

    private void previewFile() {
        try {
            File tempFile = File.createTempFile("preview", originalExtension, getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decryptedData);
            }

            Uri contentUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", tempFile);

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, getMimeType(originalExtension));
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(viewIntent);
        } catch (IOException e) {
            Toast.makeText(this, "Preview failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                out.write(decryptedData);
                Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case ".txt": return "text/plain";
            case ".pdf": return "application/pdf";
            case ".jpg": case ".jpeg": return "image/jpeg";
            case ".png": return "image/png";
            default: return "*/*";
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