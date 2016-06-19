package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskRecord extends Record {
    static final String TABLE_TASKS = "tasks";

    static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_START_TIME = "startTime";
    private static final String COLUMN_END_TIME = "endTime";
    private static final String COLUMN_RELEVANT = "relevant";
    private static final String COLUMN_OLDEST_VISIBLE = "oldestVisible";

    private static final String INDEX_RELEVANT = "tasksIndexRelevant";

    private final int mId;
    private String mName;

    private final long mStartTime;
    private Long mEndTime;

    private boolean mRelevant;
    private Long mOldestVisible;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_TASKS
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_START_TIME + " TEXT NOT NULL, "
                + COLUMN_END_TIME + " TEXT, "
                + COLUMN_RELEVANT + " INTEGER NOT NULL DEFAULT 1, "
                + COLUMN_OLDEST_VISIBLE + " INTEGER);");
    }

    @SuppressWarnings("UnusedParameters")
    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        //sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        //onCreate(sqLiteDatabase);

        if (oldVersion <= 5) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_TASKS
                    + " ADD COLUMN " + COLUMN_RELEVANT + " INTEGER NOT NULL DEFAULT 1");

            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_TASKS
                    + " ADD COLUMN " + COLUMN_OLDEST_VISIBLE + " INTEGER");
        }

        if (oldVersion <= 7) {
            sqLiteDatabase.execSQL("CREATE INDEX " + INDEX_RELEVANT + " ON " + TABLE_TASKS + "(" + COLUMN_RELEVANT + " DESC)");
        }
    }

    public static ArrayList<TaskRecord> getTaskRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<TaskRecord> taskRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_TASKS, null, COLUMN_RELEVANT + " = 1", null, null, null, null);
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
        boolean relevant = (cursor.getInt(4) == 1);
        Long oldestVisible = (cursor.isNull(5) ? null : cursor.getLong(5));

        Assert.assertTrue(name != null);
        Assert.assertTrue(endTime == null || startTime <= endTime);

        return new TaskRecord(true, id, name, startTime, endTime, relevant, oldestVisible);
    }

    static int getMaxId(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);
        return getMaxId(sqLiteDatabase, TABLE_TASKS, COLUMN_ID);
    }

    TaskRecord(boolean created, int id, String name, long startTime, Long endTime, boolean relevant, Long oldestVisible) {
        super(created);

        Assert.assertTrue(name != null);
        Assert.assertTrue(endTime == null || startTime <= endTime);

        mId = id;
        mName = name;
        mStartTime = startTime;
        mEndTime = endTime;

        mRelevant = relevant;
        mOldestVisible = oldestVisible;
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

    public boolean getRelevant() {
        return mRelevant;
    }

    public Long getOldestVisible() {
        return mOldestVisible;
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

    public void setRelevant(boolean relevant) {
        mRelevant = relevant;
        mChanged = true;
    }

    public void setOldestVisible(long oldestVisible) {
        mOldestVisible = oldestVisible;
        mChanged = true;
    }

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, mName);
        contentValues.put(COLUMN_START_TIME, mStartTime);
        contentValues.put(COLUMN_END_TIME, mEndTime);
        contentValues.put(COLUMN_RELEVANT, mRelevant ? 1 : 0);
        contentValues.put(COLUMN_OLDEST_VISIBLE, mOldestVisible);
        return contentValues;
    }

    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_TASKS, COLUMN_ID, mId);
    }

    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_TASKS);
    }
}
