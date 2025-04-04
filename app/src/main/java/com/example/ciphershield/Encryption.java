package com.example.ciphershield;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class Encryption extends AppCompatActivity {
    private static final int FILE_REQUEST_CODE = 1;
 TextView txtFileName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_encryption);
        ActivityCompat.requestPermissions(this,new String[]{READ_MEDIA_IMAGES}
       , PackageManager.PERMISSION_GRANTED);
       ActivityCompat.requestPermissions(this,new String[]{READ_MEDIA_VIDEO}
              , PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this,new String[]{READ_EXTERNAL_STORAGE}
                , PackageManager.PERMISSION_GRANTED);

        Button btnSelectFile = findViewById(R.id.select_file);
        Button Con=findViewById(R.id.Continue);
        txtFileName = findViewById(R.id.textView2);
        Con.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }});}



    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_REQUEST_CODE);
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                String filePath = fileUri.getPath(); // Get file path
                txtFileName.setText("Selected File: " + filePath);
            }}}
    private byte[] convertFileToBytes(Uri fileUri) throws IOException, FileNotFoundException {
        InputStream inputStream = getContentResolver().openInputStream(fileUri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        int bytesRead;
        byte[] buffer = new byte[1024]; // 1KB buffer size

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        return byteBuffer.toByteArray(); // Return file as byte array
    }
    private String bytesToBinaryString(byte[] fileBytes) {
        StringBuilder binaryString = new StringBuilder();

        for (byte b : fileBytes) {
            binaryString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return binaryString.toString(); // Return file as a binary string
    }




}





    

