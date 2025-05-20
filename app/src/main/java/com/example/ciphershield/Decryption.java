package com.example.ciphershield;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Decryption extends AppCompatActivity {

    private static final int PICK_ENCRYPTED_FILE = 1;
    private static final int PICK_KEY_FILE = 2;

    private Uri encryptedFileUri;
    private Uri keyFileUri;

    private TextView txtEncryptedName, txtKeyName, txtStatus;
    private Button btnPickEncrypted, btnPickKey, btnDecrypt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decryption);

        txtEncryptedName = findViewById(R.id.txt_selected_encrypted);
        txtKeyName = findViewById(R.id.txt_selected_key);
        txtStatus = findViewById(R.id.txt_status);

        btnPickEncrypted = findViewById(R.id.btn_select_encrypted_file);
        btnPickKey = findViewById(R.id.btn_select_key_file);
        btnDecrypt = findViewById(R.id.btn_decrypt);

        btnPickEncrypted.setOnClickListener(v -> pickFile(PICK_ENCRYPTED_FILE));
        btnPickKey.setOnClickListener(v -> pickFile(PICK_KEY_FILE));

        btnDecrypt.setOnClickListener(v -> {
            if (encryptedFileUri == null || keyFileUri == null) {
                Toast.makeText(this, "Please select both files.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // 1. Read encrypted hex
                String encryptedHex = readTextFromUri(encryptedFileUri);

                // 2. Read keys from key file
                BigInteger d = null, n = null;
                String extension = "bin"; // default
                String keyContent = readTextFromUri(keyFileUri);
                Pattern dPattern = Pattern.compile("d:\\s*(\\d+)");
                Pattern nPattern = Pattern.compile("n:\\s*(\\d+)");
                Pattern extPattern = Pattern.compile("ext:\\s*(\\w+)");

                Matcher matcher = dPattern.matcher(keyContent);
                if (matcher.find()) d = new BigInteger(matcher.group(1));
                matcher = nPattern.matcher(keyContent);
                if (matcher.find()) n = new BigInteger(matcher.group(1));
                matcher = extPattern.matcher(keyContent);
                if (matcher.find()) extension = matcher.group(1);

                if (d == null || n == null) {
                    Toast.makeText(this, "Invalid key file", Toast.LENGTH_LONG).show();
                    return;
                }

                // 3. Decrypt
                BigInteger encrypted = new BigInteger(encryptedHex, 16);
                BigInteger decrypted = encrypted.modPow(d, n);
                byte[] decryptedBytes = decrypted.toByteArray();

                // 4. Save decrypted file
                String outFileName = "decrypted_" + System.currentTimeMillis() + "." + extension;
                File outFile = new File(getExternalFilesDir(null), outFileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(decryptedBytes);
                }

                txtStatus.setText("Decrypted and saved as: " + outFile.getAbsolutePath());
                Toast.makeText(this, "Decryption Successful!", Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                txtStatus.setText("Decryption failed.");
                e.printStackTrace();
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
                txtEncryptedName.setText("Encrypted: " + getFileName(uri));
            } else if (requestCode == PICK_KEY_FILE) {
                keyFileUri = uri;
                txtKeyName.setText("Key: " + getFileName(uri));
            }
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line.trim());
            }
        }
        return builder.toString();
    }

    private String getFileName(Uri uri) {
        String result = uri.getLastPathSegment();
        if (result == null) return "file";
        int idx = result.lastIndexOf('/');
        return idx != -1 ? result.substring(idx + 1) : result;
    }
}
