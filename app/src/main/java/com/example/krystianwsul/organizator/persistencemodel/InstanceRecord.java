package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

import java.util.ArrayList;

public class InstanceRecord extends Record {
    private static final String TABLE_INSTANCES = "instances";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TASK_ID = "taskId";
    private static final String COLUMN_DONE = "done";
    private static final String COLUMN_SCHEDULE_YEAR = "scheduleYear";
    private static final String COLUMN_SCHEDULE_MONTH = "scheduleMonth";
    private static final String COLUMN_SCHEDULE_DAY = "scheduleDay";
    private static final String COLUMN_SCHEDULE_CUSTOM_TIME_ID = "scheduleCustomTimeId";
    private static final String COLUMN_SCHEDULE_HOUR = "scheduleHour";
    private static final String COLUMN_SCHEDULE_MINUTE = "scheduleMinute";
    private static final String COLUMN_INSTANCE_YEAR = "instanceYear";
    private static final String COLUMN_INSTANCE_MONTH = "instanceMonth";
    private static final String COLUMN_INSTANCE_DAY = "instanceDay";
    private static final String COLUMN_INSTANCE_CUSTOM_TIME_ID = "instanceCustomTimeId";
    private static final String COLUMN_INSTANCE_HOUR = "instanceHour";
    private static final String COLUMN_INSTANCE_MINUTE = "instanceMinute";
    private static final String COLUMN_HIERARCHY_TIME = "hierarchyTime";
    private static final String COLUMN_NOTIFIED = "notified";
    private static final String COLUMN_NOTIFICATION_SHOWN = "notificationShown";

    private final int mId;
    private final int mTaskId;

    private Long mDone;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private final Integer mScheduleCustomTimeId;

    private final Integer mScheduleHour;
    private final Integer mScheduleMinute;

    private Integer mInstanceYear;
    private Integer mInstanceMonth;
    private Integer mInstanceDay;

    private Integer mInstanceCustomTimeId;

    private Integer mInstanceHour;
    private Integer mInstanceMinute;

    private final long mHierarchyTime;

    private boolean mNotified;
    private boolean mNotificationShown;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_INSTANCES
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TASK_ID + " INTEGER NOT NULL REFERENCES " + TaskRecord.TABLE_TASKS + "(" + TaskRecord.COLUMN_ID + "), "
                + COLUMN_DONE + " INTEGER, "
                + COLUMN_SCHEDULE_YEAR + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_MONTH + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_DAY + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_SCHEDULE_HOUR + " INTEGER, "
                + COLUMN_SCHEDULE_MINUTE + " INTEGER, "
                + COLUMN_INSTANCE_YEAR + " INTEGER, "
                + COLUMN_INSTANCE_MONTH + " INTEGER, "
                + COLUMN_INSTANCE_DAY + " INTEGER, "
                + COLUMN_INSTANCE_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_INSTANCE_HOUR + " INTEGER, "
                + COLUMN_INSTANCE_MINUTE + " INTEGER, "
                + COLUMN_HIERARCHY_TIME + " INTEGER NOT NULL, "
                + COLUMN_NOTIFIED + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_NOTIFICATION_SHOWN + " INTEGER NOT NULL DEFAULT 0);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTANCES);
        onCreate(sqLiteDatabase);
    }

    public static ArrayList<InstanceRecord> getInstanceRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<InstanceRecord> instanceRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_INSTANCES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            instanceRecords.add(cursorToInstanceRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return instanceRecords;
    }

    private static InstanceRecord cursorToInstanceRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        int taskId = cursor.getInt(1);
        Long done = (cursor.isNull(2) ? null : cursor.getLong(2));
        int scheduleYear = cursor.getInt(3);
        int scheduleMonth = cursor.getInt(4);
        int scheduleDay = cursor.getInt(5);
        Integer scheduleCustomTimeId = (cursor.isNull(6) ? null : cursor.getInt(6));
        Integer scheduleHour = (cursor.isNull(7) ? null : cursor.getInt(7));
        Integer scheduleMinute = (cursor.isNull(8) ? null : cursor.getInt(8));
        Integer instanceYear = (cursor.isNull(9) ? null : cursor.getInt(9));
        Integer instanceMonth = (cursor.isNull(10) ? null : cursor.getInt(10));
        Integer instanceDay = (cursor.isNull(11) ? null : cursor.getInt(11));
        Integer instanceCustomTimeId = (cursor.isNull(12) ? null : cursor.getInt(12));
        Integer instanceHour = (cursor.isNull(13) ? null : cursor.getInt(13));
        Integer instanceMinute = (cursor.isNull(14) ? null : cursor.getInt(14));
        long hierarchyTime = cursor.getLong(15);
        boolean notified = (cursor.getInt(16) == 1);
        boolean notificationShown = (cursor.getInt(17) == 1);

        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        Assert.assertTrue((instanceYear == null) == (instanceMonth == null));
        Assert.assertTrue((instanceYear == null) == (instanceDay == null));
        boolean hasInstanceDate = (instanceYear != null);

        Assert.assertTrue((instanceHour == null) == (instanceMinute == null));
        Assert.assertTrue((instanceHour == null) || (instanceCustomTimeId == null));
        boolean hasInstanceTime = ((instanceHour != null) || (instanceCustomTimeId != null));
        Assert.assertTrue(hasInstanceDate == hasInstanceTime);

        return new InstanceRecord(true, id, taskId, done, scheduleYear, scheduleMonth, scheduleDay, scheduleCustomTimeId, scheduleHour, scheduleMinute, instanceYear, instanceMonth, instanceDay, instanceCustomTimeId, instanceHour, instanceMinute, hierarchyTime, notified, notificationShown);
    }

    InstanceRecord(boolean created, int id, int taskId, Long done, int scheduleYear, int scheduleMonth, int scheduleDay, Integer scheduleCustomTimeId, Integer scheduleHour, Integer scheduleMinute, Integer instanceYear, Integer instanceMonth, Integer instanceDay, Integer instanceCustomTimeId, Integer instanceHour, Integer instanceMinute, long hierarchyTime, boolean notified, boolean notificationShown) {
        super(created);

        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        Assert.assertTrue((instanceYear == null) == (instanceMonth == null));
        Assert.assertTrue((instanceYear == null) == (instanceDay == null));
        boolean hasInstanceDate = (instanceYear != null);

        Assert.assertTrue((instanceHour == null) == (instanceMinute == null));
        Assert.assertTrue((instanceHour == null) || (instanceCustomTimeId == null));
        boolean hasInstanceTime = ((instanceHour != null) || (instanceCustomTimeId != null));
        Assert.assertTrue(hasInstanceDate == hasInstanceTime);

        mId = id;
        mTaskId = taskId;

        mDone = done;

        mScheduleYear = scheduleYear;
        mScheduleMonth = scheduleMonth;
        mScheduleDay = scheduleDay;

        mScheduleCustomTimeId = scheduleCustomTimeId;

        mScheduleHour = scheduleHour;
        mScheduleMinute = scheduleMinute;

        mInstanceYear = instanceYear;
        mInstanceMonth = instanceMonth;
        mInstanceDay = instanceDay;

        mInstanceCustomTimeId = instanceCustomTimeId;

        mInstanceHour = instanceHour;
        mInstanceMinute = instanceMinute;

        mHierarchyTime = hierarchyTime;

        mNotified = notified;

        mNotificationShown = notificationShown;
    }

    public int getId() {
        return mId;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public Long getDone() {
        return mDone;
    }

    public int getScheduleYear() {
        return mScheduleYear;
    }

    public int getScheduleMonth() {
        return mScheduleMonth;
    }

    public int getScheduleDay() {
        return mScheduleDay;
    }

    public Integer getScheduleCustomTimeId() {
        return mScheduleCustomTimeId;
    }

    public Integer getScheduleHour() {
        return mScheduleHour;
    }

    public Integer getScheduleMinute() {
        return mScheduleMinute;
    }

    public Integer getInstanceYear() {
        return mInstanceYear;
    }

    public Integer getInstanceMonth() {
        return mInstanceMonth;
    }

    public Integer getInstanceDay() {
        return mInstanceDay;
    }

    public Integer getInstanceCustomTimeId() {
        return mInstanceCustomTimeId;
    }

    public Integer getInstanceHour() {
        return mInstanceHour;
    }

    public Integer getInstanceMinute() {
        return mInstanceMinute;
    }

    public boolean getNotified() {
        return mNotified;
    }

    public boolean getNotificationShown() {
        return mNotificationShown;
    }

    public long getHierarchyTime() {
        return mHierarchyTime;
    }

    public void setDone(Long done) {
        mDone = done;
        mChanged = true;
    }

    public void setInstanceYear(int instanceYear) {
        mInstanceYear = instanceYear;
        mChanged = true;
    }

    public void setInstanceMonth(int instanceMonth) {
        mInstanceMonth = instanceMonth;
        mChanged = true;
    }

    public void setInstanceDay(int instanceDay) {
        mInstanceDay = instanceDay;
        mChanged = true;
    }

    public void setInstanceCustomTimeId(int instanceCustomTimeId) {
        mInstanceCustomTimeId = instanceCustomTimeId;
        mChanged = true;
    }

    public void setInstanceHour(int instanceHour) {
        mInstanceHour = instanceHour;
        mChanged = true;
    }

    public void setInstanceMinute(int instanceMinute) {
        mInstanceMinute = instanceMinute;
        mChanged = true;
    }

    public void setNotified(boolean notified) {
        mNotified = notified;
        mChanged = true;
    }

    public void setNotificationShown(boolean notificationShown) {
        mNotificationShown = notificationShown;
        mChanged = true;
    }

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TASK_ID, mTaskId);
        contentValues.put(COLUMN_DONE, mDone);
        contentValues.put(COLUMN_SCHEDULE_YEAR, mScheduleYear);
        contentValues.put(COLUMN_SCHEDULE_MONTH, mScheduleMonth);
        contentValues.put(COLUMN_SCHEDULE_DAY, mScheduleDay);
        contentValues.put(COLUMN_SCHEDULE_CUSTOM_TIME_ID, mScheduleCustomTimeId);
        contentValues.put(COLUMN_SCHEDULE_HOUR, mScheduleHour);
        contentValues.put(COLUMN_SCHEDULE_MINUTE, mScheduleMinute);
        contentValues.put(COLUMN_INSTANCE_YEAR, mInstanceYear);
        contentValues.put(COLUMN_INSTANCE_MONTH, mInstanceMonth);
        contentValues.put(COLUMN_INSTANCE_DAY, mInstanceDay);
        contentValues.put(COLUMN_INSTANCE_CUSTOM_TIME_ID, mInstanceCustomTimeId);
        contentValues.put(COLUMN_INSTANCE_HOUR, mInstanceHour);
        contentValues.put(COLUMN_INSTANCE_MINUTE, mInstanceMinute);
        contentValues.put(COLUMN_HIERARCHY_TIME, mHierarchyTime);
        contentValues.put(COLUMN_NOTIFIED, mNotified);
        contentValues.put(COLUMN_NOTIFICATION_SHOWN, mNotificationShown);
        return contentValues;
    }

    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_INSTANCES, COLUMN_ID, mId);
    }

    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_INSTANCES);
    }
}
