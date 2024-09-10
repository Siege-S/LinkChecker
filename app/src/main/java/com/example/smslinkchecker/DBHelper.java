package com.example.smslinkchecker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "linkGuard.db";
    public static final int DATABASE_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE urlmessagestbl (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, contactnumber TEXT, message TEXT, apiurl TEXT, analysis TEXT, screenshot BLOB, analysisJSON TEXT, timestamp TEXT DEFAULT (datetime('now','localtime')) )";
        db.execSQL(sql);
        String newTable = "CREATE TABLE urlOfflineTbl (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, sender TEXT, message TEXT, timestamp TEXT DEFAULT (datetime('now','localtime')))";
        db.execSQL(newTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS urlmessagestbl");
        if (oldVersion < 2) {
            // Add the new table when upgrading
            db.execSQL("DROP TABLE IF EXISTS urlOfflineTbl");
        }
        onCreate(db);
    }

    public void deleteRecordById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("urlOfflineTbl", "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Cursor getOfflineData(){
        SQLiteDatabase DB = this.getReadableDatabase();
        Cursor cursor = DB.rawQuery("SELECT * FROM urlOfflineTbl ORDER BY timestamp DESC", null);
        return cursor;
    }
    public int getOfflineDataCount() {
        SQLiteDatabase DB = this.getReadableDatabase();
        Cursor cursor = DB.rawQuery("SELECT COUNT(*) FROM urlOfflineTbl", null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0); // Get the count from the first column
        }

        cursor.close(); // Always close the cursor to avoid memory leaks
        return count;
    }
    public ArrayList<String> getOfflineUrls() {
        ArrayList<String> offlineURL = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Query to get all URLs from the table
            cursor = db.rawQuery("SELECT url FROM urlOfflineTbl ORDER BY timestamp DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    // Add each URL to the ArrayList
                    offlineURL.add(cursor.getString(cursor.getColumnIndexOrThrow("url")));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DBHelper", "Error while retrieving data", e);
        } finally {
            // Close the cursor and database
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return offlineURL;
    }

    public boolean insertOfflineTbl(String url, String sender, String message) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("url", url);
            contentValues.put("sender", sender);
            contentValues.put("message", message);
            contentValues.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long result = db.insert("urlOfflineTbl", null, contentValues);
            if (result == -1) {
                Log.e("DBHelper", "Failed to insert data");
                return false;
            } else {
                Log.v("DBHelper", "Data inserted successfully");
                return true;
            }
        } catch (Exception e) {
            Log.e("DBHelper", "Error inserting data", e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    //urlmessagestbl function
    public void insertData(String url, String sender,  String messageBody, String apiUrl, String analysis, byte[] screenShot, String analysisJSON) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("url", url);
            contentValues.put("contactnumber", sender);
            contentValues.put("message", messageBody);
            contentValues.put("apiurl", apiUrl);
            contentValues.put("analysis", analysis);
            contentValues.put("screenshot", screenShot);
            contentValues.put("analysisJSON", analysisJSON);
            contentValues.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

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

    //urlmessagestbl function
    public Cursor getdata(){
        SQLiteDatabase DB = this.getReadableDatabase();
        Cursor cursor = DB.rawQuery("SELECT * FROM urlmessagestbl ORDER BY timestamp DESC", null);
        return cursor;
    }
    //urlmessagestbl function
    public void deleteRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("urlmessagestbl", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

}
