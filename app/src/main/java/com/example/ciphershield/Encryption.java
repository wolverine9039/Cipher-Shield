package com.example.ciphershield;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class Encryption extends AppCompatActivity {

    private Button btnSelectFile, btnProceed;
    private TextView txtSelectedFile;
    private RadioGroup radioGroup;
    private Uri selectedFileUri = null;
    private String selectedMethod = "HYBRID_RSA_AES";

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    String fileName = getFileName(selectedFileUri);
                    txtSelectedFile.setText("Selected File: " + (fileName != null ? fileName : "Unknown"));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encryption);

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnProceed = findViewById(R.id.btnProceed);
        txtSelectedFile = findViewById(R.id.txtSelectedFile);
        radioGroup = findViewById(R.id.radioGroupMethods);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadio = findViewById(checkedId);
            if (selectedRadio != null) {
                String method = selectedRadio.getText().toString().toUpperCase();
                selectedMethod = method.contains("HYBRID") ? "HYBRID_RSA_AES" : "HUFFMAN";
            }
        });

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        btnProceed.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, Encryption_Algos.class);
            intent.putExtra("file_uri", selectedFileUri.toString());
            intent.putExtra("encryption_method", selectedMethod);
            startActivity(intent);
        });
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