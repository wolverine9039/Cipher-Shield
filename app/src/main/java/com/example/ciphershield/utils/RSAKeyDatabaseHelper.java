package com.example.ciphershield.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RSAKeyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cipher_shield.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_KEYS = "rsa_keys";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_E = "e"; // Public key exponent
    public static final String COLUMN_D = "d"; // Private key exponent
    public static final String COLUMN_N = "n"; // Modulus

    private static final String TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_KEYS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_E + " TEXT NOT NULL, " +
                    COLUMN_D + " TEXT NOT NULL, " +
                    COLUMN_N + " TEXT NOT NULL);";

    public RSAKeyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYS);
        onCreate(db);
    }
}
