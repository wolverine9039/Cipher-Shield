package com.example.ciphershield;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Decryption extends AppCompatActivity {

    private static final int PICK_ENCRYPTED_FILE = 1;
    private static final int PICK_KEY_FILE = 2;

    private Uri encryptedFileUri;
    private Uri keyFileUri;
    private Uri savedDecryptedUri;

    private TextView txtEncryptedName, txtKeyName, txtStatus;
    private Button btnPickEncrypted, btnPickKey, btnDecrypt, btnHome, btnExit, btnPreview;

    private byte[] decryptedBytes;
    private String originalExt = "bin";

    private ActivityResultLauncher<Intent> saveDecryptedLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryption);

        // UI elements
        txtEncryptedName = findViewById(R.id.txt_selected_encrypted);
        txtKeyName = findViewById(R.id.txt_selected_key);
        txtStatus = findViewById(R.id.txt_status);
        btnPickEncrypted = findViewById(R.id.btn_select_encrypted_file);
        btnPickKey = findViewById(R.id.btn_select_key_file);
        btnDecrypt = findViewById(R.id.btn_decrypt);
        btnHome = findViewById(R.id.btn_home);
        btnExit = findViewById(R.id.btn_exit);
        btnPreview = findViewById(R.id.btn_preview_decrypted);

        btnPreview.setEnabled(false);

        saveDecryptedLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && decryptedBytes != null) {
                        savedDecryptedUri = result.getData().getData();
                        try (OutputStream out = getContentResolver().openOutputStream(savedDecryptedUri)) {
                            out.write(decryptedBytes);
                            txtStatus.setText("Decrypted and saved to:\n" + savedDecryptedUri.getPath());
                            Toast.makeText(this, "Decryption successful.", Toast.LENGTH_LONG).show();
                            btnPreview.setEnabled(true);
                        } catch (IOException e) {
                            txtStatus.setText("Failed to save file.");
                        }
                    }
                }
        );

        btnPickEncrypted.setOnClickListener(v -> pickFile(PICK_ENCRYPTED_FILE));
        btnPickKey.setOnClickListener(v -> pickFile(PICK_KEY_FILE));

        btnDecrypt.setOnClickListener(v -> {
            if (encryptedFileUri == null || keyFileUri == null) {
                Toast.makeText(this, "Please select both files.", Toast.LENGTH_SHORT).show();
                return;
            }
            decryptFile();
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(Decryption.this, front.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnExit.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        btnPreview.setOnClickListener(v -> {
            if (savedDecryptedUri != null) {
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(savedDecryptedUri, "*/*");
                openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(openIntent, "Open Decrypted File"));
            }
        });
    }

    private void pickFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_ENCRYPTED_FILE) {
                encryptedFileUri = uri;
                txtEncryptedName.setText("Encrypted File: " + getFileName(uri));
            } else if (requestCode == PICK_KEY_FILE) {
                keyFileUri = uri;
                txtKeyName.setText("Key File: " + getFileName(uri));
            }
        }
    }

    private void decryptFile() {
        try {
            byte[] encryptedContent = readBytesFromUri(encryptedFileUri);
            String keyText = readTextFromUri(keyFileUri);

            BigInteger d = extractBigInteger(keyText, "d");
            BigInteger n = extractBigInteger(keyText, "n");
            originalExt = extractString(keyText, "ext");

            if (d == null || n == null) {
                txtStatus.setText("Invalid key file.");
                return;
            }

            ByteArrayInputStream input = new ByteArrayInputStream(encryptedContent);
            int aesKeyLength = readInt(input);
            byte[] encryptedAESKey = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                encryptedAESKey = input.readNBytes(aesKeyLength);
            }
            byte[] encryptedFileData = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                encryptedFileData = input.readAllBytes();
            }

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(new RSAPrivateKeySpec(n, d));
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);

            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
            decryptedBytes = aesCipher.doFinal(encryptedFileData);

            // Let user choose where to save
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, "decrypted_file." + originalExt);
            saveDecryptedLauncher.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
            txtStatus.setText("Decryption failed: " + e.getMessage());
        }
    }

    private int readInt(InputStream input) throws IOException {
        byte[] intBytes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intBytes = input.readNBytes(4);
        }
        return ((intBytes[0] & 0xFF) << 24) |
                ((intBytes[1] & 0xFF) << 16) |
                ((intBytes[2] & 0xFF) << 8) |
                (intBytes[3] & 0xFF);
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line.trim());
            }
        }
        return builder.toString();
    }

    private BigInteger extractBigInteger(String text, String key) {
        Matcher matcher = Pattern.compile(key + ":\\s*(\\d+)").matcher(text);
        return matcher.find() ? new BigInteger(matcher.group(1)) : null;
    }

    private String extractString(String text, String key) {
        Matcher matcher = Pattern.compile(key + ":\\s*(\\w+)").matcher(text);
        return matcher.find() ? matcher.group(1) : "bin";
    }

    private String getFileName(Uri uri) {
        String path = uri.getLastPathSegment();
        return (path != null && path.contains("/")) ? path.substring(path.lastIndexOf("/") + 1) : path;
    }
}
