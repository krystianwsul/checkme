package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

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
                + " (" + COLUMN_ID + " INTEGER NOT NULL, "
                + COLUMN_PARENT_TASK_ID + " INTEGER NOT NULL, "
                + COLUMN_CHILD_TASK_ID + " INTEGER NOT NULL, "
                + COLUMN_START_TIME + " INTEGER NOT NULL, "
                + COLUMN_END_TIME + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK_HIERARCHIES);
        onCreate(sqLiteDatabase);
    }

    public TaskHierarchyRecord(int id, int parentTaskId, int childTaskId, long startTime, Long endTime) {
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
