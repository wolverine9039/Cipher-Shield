package com.example.ciphershield;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.util.Random;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class Encryption_Algos extends AppCompatActivity {

    private TextView txtPrivateKey;
    private Button btnSaveKey, btnSaveEncrypted, btnShare, btnCopyKey, btnHome, btnExit;
    private Uri fileUri;
    private byte[] encryptedAESKey, encryptedFileData;

    private BigInteger d, e, n;
    private String pendingKeyText;
    private byte[] pendingEncryptedBytes;
    private File savedEncryptedFile;

    private ActivityResultLauncher<Intent> encryptedSaveLauncher, keySaveLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encryption_algos);

        txtPrivateKey = findViewById(R.id.txt_private_key);
        btnSaveKey = findViewById(R.id.btn_save_private_key);
        btnSaveEncrypted = findViewById(R.id.btn_save_encrypted_file);
        btnShare = findViewById(R.id.btn_share_encrypted_file);
        btnCopyKey = findViewById(R.id.btn_copy_key);
        btnHome = findViewById(R.id.btn_home);
        btnExit = findViewById(R.id.btn_exit);
        btnShare.setVisibility(Button.GONE);

        // Setup file launchers
        initFileLaunchers();

        fileUri = getIntent().getParcelableExtra("file_uri");
        String originalExt = getIntent().getStringExtra("original_extension");
        if (originalExt == null) originalExt = "bin";

        if (fileUri == null) {
            Toast.makeText(this, "File URI missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            byte[] fileBytes = readBytesFromUri(fileUri);

            // AES Encrypt
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();

            Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            encryptedFileData = aesCipher.doFinal(fileBytes);

            // RSA Encrypt AES key
            generateRSAKeys(2048);
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey rsaPublic = kf.generatePublic(new java.security.spec.RSAPublicKeySpec(n, e));
            rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublic);
            encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

            // Preview Key
            String dPreview = d.toString().substring(0, 4) + "...";
            String nPreview = n.toString().substring(0, 4) + "...";
            txtPrivateKey.setText("Private Key (d, n):\n" + dPreview + "\n" + nPreview);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption failed.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCopyKey.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String fullKey = "d: " + d.toString() + "\nn: " + n.toString();
            ClipData clip = ClipData.newPlainText("Private Key", fullKey);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Full private key copied.", Toast.LENGTH_SHORT).show();
        });

        btnSaveEncrypted.setOnClickListener(v -> {
            pendingEncryptedBytes = getHybridEncryptedData();
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, "encrypted_" + System.currentTimeMillis() + ".cyps");
            encryptedSaveLauncher.launch(intent);
        });

        String finalOriginalExt = originalExt;
        btnSaveKey.setOnClickListener(v -> {
            pendingKeyText = "Private Key:\nd: " + d + "\nn: " + n + "\next: " + finalOriginalExt;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "private_key_" + System.currentTimeMillis() + ".txt");
            keySaveLauncher.launch(intent);
        });

        btnShare.setOnClickListener(v -> {
            if (savedEncryptedFile != null && savedEncryptedFile.exists()) {
                shareFile(savedEncryptedFile);
            } else {
                Toast.makeText(this, "Save the file before sharing.", Toast.LENGTH_SHORT).show();
            }
        });

        btnHome.setOnClickListener(v -> {
            Intent i = new Intent(Encryption_Algos.this, front.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });

        btnExit.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });
    }

    private void initFileLaunchers() {
        encryptedSaveLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            out.write(pendingEncryptedBytes);
                            Toast.makeText(this, "Encrypted file saved.", Toast.LENGTH_SHORT).show();
                            btnShare.setVisibility(Button.VISIBLE);
                            savedEncryptedFile = new File(uri.getPath());
                        } catch (IOException e) {
                            Toast.makeText(this, "Error saving file.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        keySaveLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            out.write(pendingKeyText.getBytes());
                            Toast.makeText(this, "Private key saved.", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(this, "Error saving key.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private void generateRSAKeys(int keySize) throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        BigInteger p = BigInteger.probablePrime(keySize / 2, random);
        BigInteger q = BigInteger.probablePrime(keySize / 2, random);
        n = p.multiply(q);
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        e = BigInteger.valueOf(65537);
        d = e.modInverse(phi);
    }

    private byte[] getHybridEncryptedData() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(intToBytes(encryptedAESKey.length));
            output.write(encryptedAESKey);
            output.write(encryptedFileData);
            return output.toByteArray();
        } catch (IOException e) {
            return null;
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

    private byte[] intToBytes(int val) {
        return new byte[]{
                (byte) (val >>> 24),
                (byte) (val >>> 16),
                (byte) (val >>> 8),
                (byte) val
        };
    }
}
