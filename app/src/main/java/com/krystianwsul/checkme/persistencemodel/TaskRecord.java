package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskRecord extends Record {
    static final String TABLE_TASKS = "tasks";

    static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_START_TIME = "startTime";
    private static final String COLUMN_END_TIME = "endTime";
    private static final String COLUMN_OLDEST_VISIBLE_YEAR = "oldestVisibleYear";
    private static final String COLUMN_OLDEST_VISIBLE_MONTH = "oldestVisibleMonth";
    private static final String COLUMN_OLDEST_VISIBLE_DAY = "oldestVisibleDay";
    private static final String COLUMN_NOTE = "note";

    private final int mId;
    private String mName;

    private final long mStartTime;
    private Long mEndTime;

    private Integer mOldestVisibleYear;
    private Integer mOldestVisibleMonth;
    private Integer mOldestVisibleDay;

    private String mNote;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_TASKS
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_START_TIME + " TEXT NOT NULL, "
                + COLUMN_END_TIME + " TEXT, "
                + COLUMN_OLDEST_VISIBLE_YEAR + " INTEGER, "
                + COLUMN_OLDEST_VISIBLE_MONTH + " INTEGER, "
                + COLUMN_OLDEST_VISIBLE_DAY + " INTEGER, "
                + COLUMN_NOTE + " TEXT);");
    }

    static ArrayList<TaskRecord> getTaskRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<TaskRecord> taskRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_TASKS, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            taskRecords.add(cursorToTaskRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return taskRecords;
    }

    private static TaskRecord cursorToTaskRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        String name = cursor.getString(1);
        long startTime = cursor.getLong(2);
        Long endTime = (cursor.isNull(3) ? null : cursor.getLong(3));
        Integer oldestVisibleYear = (cursor.isNull(4) ? null : cursor.getInt(4));
        Integer oldestVisibleMonth = (cursor.isNull(5) ? null : cursor.getInt(5));
        Integer oldestVisibleDay = (cursor.isNull(6) ? null : cursor.getInt(6));
        String note = cursor.getString(7);

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));

        return new TaskRecord(true, id, name, startTime, endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, note);
    }

    static int getMaxId(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);
        return getMaxId(sqLiteDatabase, TABLE_TASKS, COLUMN_ID);
    }

    TaskRecord(boolean created, int id, String name, long startTime, Long endTime, Integer oldestVisibleYear, Integer oldestVisibleMonth, Integer oldestVisibleDay, String note) {
        super(created);

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));

        mId = id;
        mName = name;
        mStartTime = startTime;
        mEndTime = endTime;

        mOldestVisibleDay = oldestVisibleDay;
        mOldestVisibleMonth = oldestVisibleMonth;
        mOldestVisibleYear = oldestVisibleYear;

        mNote = note;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }

    public Integer getOldestVisibleYear() {
        return mOldestVisibleYear;
    }

    public Integer getOldestVisibleMonth() {
        return mOldestVisibleMonth;
    }

    public Integer getOldestVisibleDay() {
        return mOldestVisibleDay;
    }

    public String getNote() {
        return mNote;
    }

    public void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mName = name;
        mChanged = true;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(mEndTime == null);
        Assert.assertTrue(mStartTime <= endTime);

        mEndTime = endTime;
        mChanged = true;
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        mOldestVisibleYear = oldestVisibleYear;
        mChanged = true;
    }

    public void setOldestVisibleMonth(int oldestVisibleYearMonth) {
        mOldestVisibleMonth = oldestVisibleYearMonth;
        mChanged = true;
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        mOldestVisibleDay = oldestVisibleDay;
        mChanged = true;
    }

    public void setNote(String note) {
        mNote = note;
        mChanged = true;
    }

    @NonNull
    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, mName);
        contentValues.put(COLUMN_START_TIME, mStartTime);
        contentValues.put(COLUMN_END_TIME, mEndTime);
        contentValues.put(COLUMN_OLDEST_VISIBLE_YEAR, mOldestVisibleYear);
        contentValues.put(COLUMN_OLDEST_VISIBLE_MONTH, mOldestVisibleMonth);
        contentValues.put(COLUMN_OLDEST_VISIBLE_DAY, mOldestVisibleDay);
        contentValues.put(COLUMN_NOTE, mNote);
        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_TASKS, COLUMN_ID, mId);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_TASKS);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_TASKS, COLUMN_ID, mId);
    }
}
