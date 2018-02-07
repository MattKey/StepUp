package com.example.matt.stepup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Matt on 1/19/2018.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Exercise.db";
    public static final String TABLE_NAME = "ex_table";
    public static final String COL_1 = "ENTRYNUM";
    public static final String COL_2 = "DATETIME";
    public static final String COL_3 = "STEPS";
    public static final String COL_4 = "LATITUDE";
    public static final String COL_5 = "LONGITUDE";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME +"(ENTRYNUM INTEGER PRIMARY KEY AUTOINCREMENT,DATETIME TEXT,STEPS INTEGER,LATITUDE REAL,LONGITUDE REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String dateTime,int steps, double lat, double lon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2,dateTime);
        contentValues.put(COL_3,steps);
        contentValues.put(COL_4,lat);
        contentValues.put(COL_5,lon);
        long result = db.insert(TABLE_NAME,null ,contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        return res;
    }

    public boolean updateData(String entrynum,String dateTime,int steps, double lat, double lon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1,entrynum);
        contentValues.put(COL_2,dateTime);
        contentValues.put(COL_3,steps);
        contentValues.put(COL_4,lat);
        contentValues.put(COL_5,lon);
        db.update(TABLE_NAME, contentValues, "ENTRYNUM = ?",new String[] { entrynum });
        return true;
    }

    public Integer deleteData (String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ENTRYNUM = ?",new String[] {id});
    }

    public void clearData(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}
