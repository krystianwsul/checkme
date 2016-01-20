package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskHierarchyRecord {
    private static final String TABLE_TASK_HIERARCHIES = "taskHierarchies";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_PARENT_TASK_ID = "parentTaskId";
    private static final String COLUMN_CHILD_TASK_ID = "childTaskId";
    private static final String COLUMN_START_TIME = "startTime";
    private static final String COLUMN_END_TIME = "endTime";

    private final int mId;

    private final int mParentTaskId;
    private final int mChildTaskId;

    private final long mStartTime;
    private Long mEndTime;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_TASK_HIERARCHIES
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PARENT_TASK_ID + " INTEGER NOT NULL REFERENCES " + TaskRecord.TABLE_TASKS + "(" + TaskRecord.COLUMN_ID + "), "
                + COLUMN_CHILD_TASK_ID + " INTEGER NOT NULL REFERENCES " + TaskRecord.TABLE_TASKS + "(" + TaskRecord.COLUMN_ID + "), "
                + COLUMN_START_TIME + " INTEGER NOT NULL, "
                + COLUMN_END_TIME + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK_HIERARCHIES);
        onCreate(sqLiteDatabase);
    }

    public static TaskHierarchyRecord createTaskHierarchyRecord(SQLiteDatabase sqLiteDatabase, int parentTaskId, int childTaskId, long startTime, Long endTime) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(parentTaskId != childTaskId);
        Assert.assertTrue(endTime == null || startTime <= endTime);

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_PARENT_TASK_ID, parentTaskId);
        contentValues.put(COLUMN_CHILD_TASK_ID, childTaskId);
        contentValues.put(COLUMN_START_TIME, startTime);
        contentValues.put(COLUMN_END_TIME, endTime);

        long insertId = sqLiteDatabase.insert(TABLE_TASK_HIERARCHIES, null, contentValues);
        Assert.assertTrue(insertId != -1);

        Cursor cursor = sqLiteDatabase.query(TABLE_TASK_HIERARCHIES, null, COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();

        TaskHierarchyRecord taskHierarchyRecord = cursorToTaskHierarchyRecord(cursor);
        Assert.assertTrue(taskHierarchyRecord != null);

        cursor.close();
        return taskHierarchyRecord;
    }

    public static ArrayList<TaskHierarchyRecord> getTaskHierarchyRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<TaskHierarchyRecord> taskHierarchyRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_TASK_HIERARCHIES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            taskHierarchyRecords.add(cursorToTaskHierarchyRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return taskHierarchyRecords;
    }

    private static TaskHierarchyRecord cursorToTaskHierarchyRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        int parentTaskId = cursor.getInt(1);
        int childTaskId = cursor.getInt(2);
        long startTime = cursor.getLong(3);
        Long endTime = (cursor.isNull(4) ? null : cursor.getLong(4));

        Assert.assertTrue(parentTaskId != childTaskId);
        Assert.assertTrue(endTime == null || startTime <= endTime);

        return new TaskHierarchyRecord(id, parentTaskId, childTaskId, startTime, endTime);
    }

    private TaskHierarchyRecord(int id, int parentTaskId, int childTaskId, long startTime, Long endTime) {
        Assert.assertTrue(parentTaskId != childTaskId);
        Assert.assertTrue(endTime == null || startTime <= endTime);

        mId = id;
        mParentTaskId = parentTaskId;
        mChildTaskId = childTaskId;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public int getId() {
        return mId;
    }

    public int getParentTaskId() {
        return mParentTaskId;
    }

    public int getChildTaskId() {
        return mChildTaskId;
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
