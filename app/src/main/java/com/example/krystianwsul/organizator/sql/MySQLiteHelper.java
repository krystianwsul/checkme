package com.example.krystianwsul.organizator.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_TASKS = "tasks";

    public static final String COLUMN_TASKS_ID = "_id";
    public static final String COLUMN_TASKS_NAME = "name";
    public static final String COLUMN_TASKS_START_TIME = "startTime";
    public static final String COLUMN_TASKS_END_TIME = "endTime";

    public static final String DATABASE_NAME = "tasks.db";
    public static final int DATABASE_VERSION = 1;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_TASKS + " (" + COLUMN_TASKS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_TASKS_NAME + " TEXT NOT NULL, " + COLUMN_TASKS_START_TIME + " TEXT NOT NULL, " + COLUMN_TASKS_END_TIME + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(sqLiteDatabase);
    }
}
