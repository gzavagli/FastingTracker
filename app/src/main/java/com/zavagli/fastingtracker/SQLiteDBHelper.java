package com.zavagli.fastingtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by nrjguid on 9/23/2018.
 */

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "fastingtracker";
    public static final String EVENTS_TABLE_NAME = "events";
    public static final String EVENTS_COLUMN_ID = "_id";
    public static final String EVENTS_COLUMN_DATE = "date";
    public static final String EVENTS_COLUMN_EVENT = "event";

    public SQLiteDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                "CREATE TABLE " + EVENTS_TABLE_NAME + " (" +
                EVENTS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EVENTS_COLUMN_DATE + " TEXT, " +
                EVENTS_COLUMN_EVENT + " TEXT" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
