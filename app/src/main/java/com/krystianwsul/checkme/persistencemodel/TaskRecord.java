package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

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

    private final int id;

    @NonNull
    private String name;

    private final long startTime;

    @Nullable
    private Long endTime;

    @Nullable
    private Integer oldestVisibleYear;

    @Nullable
    private Integer oldestVisibleMonth;

    @Nullable
    private Integer oldestVisibleDay;

    @Nullable
    private String note;

    public static void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
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

    @NonNull
    static List<TaskRecord> getTaskRecords(@NonNull SQLiteDatabase sqLiteDatabase) {
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

    @NonNull
    private static TaskRecord cursorToTaskRecord(@NonNull Cursor cursor) {
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

    static int getMaxId(@NonNull SQLiteDatabase sqLiteDatabase) {
        return getMaxId(sqLiteDatabase, TABLE_TASKS, COLUMN_ID);
    }

    TaskRecord(boolean created, int id, @NonNull String name, long startTime, @Nullable Long endTime, @Nullable Integer oldestVisibleYear, @Nullable Integer oldestVisibleMonth, @Nullable Integer oldestVisibleDay, @Nullable String note) {
        super(created);

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(endTime == null || startTime <= endTime);
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleMonth == null));
        Assert.assertTrue((oldestVisibleYear == null) == (oldestVisibleDay == null));

        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;

        this.oldestVisibleDay = oldestVisibleDay;
        this.oldestVisibleMonth = oldestVisibleMonth;
        this.oldestVisibleYear = oldestVisibleYear;

        this.note = note;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    @Nullable
    public Long getEndTime() {
        return endTime;
    }

    @Nullable
    public Integer getOldestVisibleYear() {
        return oldestVisibleYear;
    }

    @Nullable
    public Integer getOldestVisibleMonth() {
        return oldestVisibleMonth;
    }

    @Nullable
    public Integer getOldestVisibleDay() {
        return oldestVisibleDay;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public void setName(@Nullable String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.name = name;
        changed = true;
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(this.endTime == null);
        Assert.assertTrue(startTime <= endTime);

        this.endTime = endTime;
        changed = true;
    }

    public void setOldestVisibleYear(int oldestVisibleYear) {
        this.oldestVisibleYear = oldestVisibleYear;
        changed = true;
    }

    public void setOldestVisibleMonth(int oldestVisibleYearMonth) {
        oldestVisibleMonth = oldestVisibleYearMonth;
        changed = true;
    }

    public void setOldestVisibleDay(int oldestVisibleDay) {
        this.oldestVisibleDay = oldestVisibleDay;
        changed = true;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
        changed = true;
    }

    @NonNull
    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, name);
        contentValues.put(COLUMN_START_TIME, startTime);
        contentValues.put(COLUMN_END_TIME, endTime);
        contentValues.put(COLUMN_OLDEST_VISIBLE_YEAR, oldestVisibleYear);
        contentValues.put(COLUMN_OLDEST_VISIBLE_MONTH, oldestVisibleMonth);
        contentValues.put(COLUMN_OLDEST_VISIBLE_DAY, oldestVisibleDay);
        contentValues.put(COLUMN_NOTE, note);
        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_TASKS, COLUMN_ID, id);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_TASKS);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_TASKS, COLUMN_ID, id);
    }
}
