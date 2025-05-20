package com.example.ciphershield;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.*;
import java.math.BigInteger;
import java.util.Random;

public class Encryption_Algos extends AppCompatActivity {

    private TextView txtPrivateKey;
    private Button btnSaveKey, btnSaveEncrypted, btnShare, homeB , ExitB;
    private BigInteger d, e, n;
    private String encryptedBinary;

    private File savedEncryptedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encryption_algos);

        txtPrivateKey = findViewById(R.id.txt_private_key);
        btnSaveKey = findViewById(R.id.btn_save_private_key);
        btnSaveEncrypted = findViewById(R.id.btn_save_encrypted_file);
        btnShare = findViewById(R.id.btn_share_encrypted_file);
        homeB = findViewById(R.id.HomeBUtton2);
        ExitB =findViewById(R.id.Exit);
        btnShare.setVisibility(Button.GONE); // initially hidden

        Uri fileUri = getIntent().getParcelableExtra("file_uri");
        if (fileUri == null) {
            Toast.makeText(this, "File URI is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            byte[] fileBytes = readBytesFromUri(fileUri);
            if (fileBytes.length == 0) {
                Toast.makeText(this, "File is empty.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            generateKeys(1024);
            String binaryString = bytesToBinaryString(fileBytes);
            encryptedBinary = encryptBinaryString(binaryString);
            txtPrivateKey.setText("Private Key (d, n):\n" + d.toString() + "\n" + n.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to read file.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Save encrypted file
        btnSaveEncrypted.setOnClickListener(v -> {
            savedEncryptedFile = saveEncryptedFile(encryptedBinary);
            if (savedEncryptedFile != null) {
                btnShare.setVisibility(Button.VISIBLE);
            }
        });
        ExitB.setOnClickListener(view ->
        {
            finish();
            System.exit(0);
        });
        homeB.setOnClickListener(view ->
        {
            Intent i=new Intent(Encryption_Algos.this, front.class);
        });

        // Save private key file
        btnSaveKey.setOnClickListener(v -> savePrivateKeyToFile(d.toString(), n.toString()));

        // Share button
        btnShare.setOnClickListener(v -> {
            if (savedEncryptedFile != null && savedEncryptedFile.exists()) {
                shareFile(savedEncryptedFile);
            } else {
                Toast.makeText(this, "Save the file before sharing.", Toast.LENGTH_SHORT).show();
            }
        });

        // Copy private key on long press
        txtPrivateKey.setOnLongClickListener(v -> {
            String keyText = txtPrivateKey.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Private Key", keyText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Private key copied to clipboard.", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private String bytesToBinaryString(byte[] fileBytes) {
        StringBuilder binaryString = new StringBuilder();
        for (byte b : fileBytes) {
            binaryString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return binaryString.toString();
    }

    private void generateKeys(int bitLength) {
        Random rand = new Random();
        BigInteger p = BigInteger.probablePrime(bitLength / 2, rand);
        BigInteger q = BigInteger.probablePrime(bitLength / 2, rand);
        n = p.multiply(q);
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        e = BigInteger.valueOf(65537);
        d = e.modInverse(phi);
    }

    private String encryptBinaryString(String binary) {
        BigInteger message = new BigInteger(binary, 2);
        BigInteger encrypted = message.modPow(e, n);
        return encrypted.toString(16); // Hex
    }

    private File saveEncryptedFile(String data) {
        String fileName = "encrypted_" + System.currentTimeMillis() + ".cyps";
        File dir = new File(getExternalFilesDir(null), "EncryptedFiles");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data.getBytes());
            Toast.makeText(this, "Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return file;
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Failed to save encrypted file.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void savePrivateKeyToFile(String dKey, String nKey) {
        String fileName = "private_key_" + System.currentTimeMillis() + ".txt";
        File dir = new File(getExternalFilesDir(null), "CipherShieldKeys");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            String content = "Private Key:\nd: " + dKey + "\nn: " + nKey;
            fos.write(content.getBytes());
            Toast.makeText(this, "Key saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException ex) {
            Toast.makeText(this, "Failed to save key.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Encrypted File"));
    }
}
