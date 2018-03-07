package com.example.lee.zenwatchapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteManager extends SQLiteOpenHelper{

    private final static String GyroscopeTable = "Gyroscope";
    private final static String AccelerometerTable = "Accelerometer";

    public SQLiteManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CreateGyroscopeTable = "CREATE TABLE IF NOT EXISTS " + GyroscopeTable + "( " +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "axisX FLOAT, " +
                "axisY FLOAT, " +
                "axisZ FLOAT, " +
                "StoreDate DATETIME, " +
                "UnixStoreDate INT" +
                ");";
        final String CreateAccelerometerTable = "CREATE TABLE IF NOT EXISTS " + AccelerometerTable + "( " +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "axisX FLOAT, " +
                "axisY FLOAT, " +
                "axisZ FLOAT, " +
                "StoreDate DATETIME, " +
                "UnixStoreDate INT" +
                ");";
        db.execSQL(CreateGyroscopeTable);
        db.execSQL(CreateAccelerometerTable);
        WatchMainActivity.updateLog("SQLiteManager", "----> onCreate()");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        WatchMainActivity.updateLog("SQLiteManager", "----> onUpgrade()");
        if (newVersion > oldVersion){
            db.beginTransaction();

            boolean success = false;

            switch (oldVersion){
                case 1:
                    final String DropGyroscopeTable_v1 = "DROP TABLE IF EXISTS " + GyroscopeTable;
                    final String DropAccelerometerTable_v1 = "DROP TABLE IF EXISTS " + AccelerometerTable;
                    db.execSQL(DropGyroscopeTable_v1);
                    db.execSQL(DropAccelerometerTable_v1);
                    WatchMainActivity.updateLog("SQLiteManager", "Drop Table");
                    success = true;
                    break;
            }
            if (success){
                db.setTransactionSuccessful();
            }
            db.endTransaction();
        }
        else {
            onCreate(db);
        }
    }
}
