package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import junit.framework.Assert;

public class TaskRecord {
    private static final String TABLE_TASKS = "tasks";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_START_TIME = "startTime";
    private static final String COLUMN_END_TIME = "endTime";

    private final int mId;
    private String mName;

    private final long mStartTime;
    private Long mEndTime;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_TASKS
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_START_TIME + " TEXT NOT NULL, "
                + COLUMN_END_TIME + " TEXT);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(sqLiteDatabase);
    }

    TaskRecord(int id, String name, long startTime, Long endTime) {
        Assert.assertTrue(name != null);

        mId = id;
        mName = name;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mName = name;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(mEndTime == null);
        Assert.assertTrue(mStartTime <= endTime);

        mEndTime = endTime;
    }
}
