package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

public class ScheduleRecord {
    private static final String TABLE_SCHEDULES = "schedules";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ROOT_TASK_ID = "rootTaskId";
    private static final String COLUMN_START_TIME = "startTime";
    private static final String COLUMN_END_TIME = "endTime";
    private static final String COLUMN_TYPE = "type";

    private final int mId;
    private final int mRootTaskId;

    private final long mStartTime;
    private Long mEndTime;

    private final int mType;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_SCHEDULES
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ROOT_TASK_ID + " INTEGER NOT NULL, "
                + COLUMN_START_TIME + " INTEGER NOT NULL, "
                + COLUMN_END_TIME + " INTEGER, "
                + COLUMN_TYPE + " INTEGER NOT NULL);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        onCreate(sqLiteDatabase);
    }

    public ScheduleRecord(int id, int rootTaskId, long startTime, Long endTime, int type) {
        Assert.assertTrue((endTime == null) || startTime < endTime);

        mId = id;
        mRootTaskId = rootTaskId;

        mStartTime = startTime;
        mEndTime = endTime;

        mType = type;
    }

    public int getId() {
        return mId;
    }

    public int getRootTaskId() {
        return mRootTaskId;
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

    public int getType() {
        return mType;
    }
}
