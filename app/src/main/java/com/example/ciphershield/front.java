package com.example.ciphershield;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

public class front extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front);

        Button btnEncrypt = findViewById(R.id.btn_encrypt);
        Button btnDecrypt = findViewById(R.id.btn_decrypt);
        Button btnAbout = findViewById(R.id.btn_about);
        Button btnusr=findViewById(R.id.btn_usr_manual);

        // Open Encryption flow
        btnEncrypt.setOnClickListener(v -> {
            Intent intent = new Intent(front.this, Encryption.class);
            startActivity(intent);
        });

        // Placeholder for Decryption (you can link a decryption activity here)
        btnDecrypt.setOnClickListener(v -> {
            try {
                Intent i = new Intent(front.this, Decryption.class);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Decryption screen not available.", Toast.LENGTH_SHORT).show();
            }

        });

        btnusr.setOnClickListener(v->{
            Intent i=new Intent(front.this, UserManual.class);
            startActivity(i);

        });

        // Show About App info
        btnAbout.setOnClickListener(v -> {
            Toast.makeText(front.this, "Cipher Shield v1.0\nSecure File Encryption App", Toast.LENGTH_LONG).show();
        });
    }
}
