package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

import java.util.ArrayList;

public class ScheduleRecord extends Record {
    static final String TABLE_SCHEDULES = "schedules";

    static final String COLUMN_ID = "_id";
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
                + COLUMN_ROOT_TASK_ID + " INTEGER NOT NULL REFERENCES " + TaskRecord.TABLE_TASKS + "(" + TaskRecord.COLUMN_ID + "), "
                + COLUMN_START_TIME + " INTEGER NOT NULL, "
                + COLUMN_END_TIME + " INTEGER, "
                + COLUMN_TYPE + " INTEGER NOT NULL);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        onCreate(sqLiteDatabase);
    }

    public static ScheduleRecord createScheduleRecord(SQLiteDatabase sqLiteDatabase, int rootTaskId, long startTime, Long endTime, int type) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue((endTime == null) || startTime < endTime);

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ROOT_TASK_ID, rootTaskId);
        contentValues.put(COLUMN_START_TIME, startTime);
        contentValues.put(COLUMN_END_TIME, endTime);
        contentValues.put(COLUMN_TYPE, type);

        long insertId = sqLiteDatabase.insert(TABLE_SCHEDULES, null, contentValues);
        Assert.assertTrue(insertId != -1);

        Cursor cursor = sqLiteDatabase.query(TABLE_SCHEDULES, null, COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();

        ScheduleRecord scheduleRecord = cursorToScheduleRecord(cursor);
        Assert.assertTrue(scheduleRecord != null);

        cursor.close();
        return scheduleRecord;
    }

    public static ArrayList<ScheduleRecord> getScheduleRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<ScheduleRecord> scheduleRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_SCHEDULES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            scheduleRecords.add(cursorToScheduleRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return scheduleRecords;
    }

    private static ScheduleRecord cursorToScheduleRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        int taskId = cursor.getInt(1);
        long startTime = cursor.getLong(2);
        Long endTime = (cursor.isNull(3) ? null : cursor.getLong(3));
        int type = cursor.getInt(4);

        Assert.assertTrue((endTime == null) || startTime < endTime);

        return new ScheduleRecord(id, taskId, startTime, endTime, type);
    }

    private ScheduleRecord(int id, int rootTaskId, long startTime, Long endTime, int type) {
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

    public int getType() {
        return mType;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(mEndTime == null);
        Assert.assertTrue(mStartTime <= endTime);

        mEndTime = endTime;
        mChanged = true;
    }

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ROOT_TASK_ID, mRootTaskId);
        contentValues.put(COLUMN_START_TIME, mStartTime);
        contentValues.put(COLUMN_END_TIME, mEndTime);
        contentValues.put(COLUMN_TYPE, mType);
        return contentValues;
    }

    @Override
    void update(SQLiteDatabase sqLiteDatabase) {
        update(sqLiteDatabase, TABLE_SCHEDULES, COLUMN_ID, mId);
    }
}
