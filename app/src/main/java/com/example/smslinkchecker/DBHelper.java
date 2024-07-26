package com.example.smslinkchecker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "linkGuard.db";
    public static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE urlmessagestbl (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, contactnumber TEXT, message TEXT, apiurl TEXT, screenshot BLOB)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE if exists urlmessagestbl");
        onCreate(db);
    }

    public void insertData(String url, String messageBody, String sender, String apiUrl, byte[] screenShot) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("url", url);
            contentValues.put("contactnumber", sender);
            contentValues.put("message", messageBody);
            contentValues.put("apiurl", apiUrl);
            contentValues.put("screenshot", screenShot);

            long result = db.insert("urlmessagestbl", null, contentValues);
            if (result == -1) {
                Log.e("DBHelper", "Failed to insert data");
            } else {
                Log.v("DBHelper", "Data inserted successfully");
            }
        } catch (Exception e) {
            Log.e("DBHelper", "Error inserting data", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public Cursor getdata(){
        SQLiteDatabase DB = this.getReadableDatabase();
        Cursor cursor = DB.rawQuery("SELECT * FROM urlmessagestbl", null);
        return cursor;
    }
}
