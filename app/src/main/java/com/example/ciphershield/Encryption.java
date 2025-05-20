package com.example.ciphershield;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;

import java.io.File;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;

public class Encryption extends AppCompatActivity {

    private static final int FILE_REQUEST_CODE = 1;
    private TextView txtFileName;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encryption);

        txtFileName = findViewById(R.id.textView2);
        Button btnSelectFile = findViewById(R.id.select_file);
        Button btnContinue = findViewById(R.id.Continue);

        requestPermissions();

        btnSelectFile.setOnClickListener(v -> openFilePicker());

        btnContinue.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extract file extension (fallback to "bin" if unknown)
            String extension = getFileExtension(selectedFileUri);
            if (extension == null || extension.trim().isEmpty()) {
                extension = "bin";
            }

            Intent intent = new Intent(Encryption.this, Encryption_Algos.class);
            intent.putExtra("file_uri", selectedFileUri);
            intent.putExtra("original_extension", extension);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{READ_MEDIA_IMAGES, READ_MEDIA_VIDEO},
                    PackageManager.PERMISSION_GRANTED);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    PackageManager.PERMISSION_GRANTED);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_REQUEST_CODE);
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();

            if (selectedFileUri != null) {
                txtFileName.setText("Selected File:\n" + selectedFileUri.getLastPathSegment());

                try {
                    final int takeFlags = data.getFlags() &
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(selectedFileUri, takeFlags);
                } catch (SecurityException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Permission not granted for selected file.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "File selection failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFileExtension(Uri uri) {
        String extension = null;

        // Try from content resolver
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        // Fallback: extract from URI string
        if (extension == null && uri.getPath() != null) {
            String path = uri.getPath();
            int dot = path.lastIndexOf('.');
            if (dot != -1) {
                extension = path.substring(dot + 1);
            }
        }

        return extension;
    }
}
