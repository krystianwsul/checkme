package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import junit.framework.Assert;

import java.util.ArrayList;

public class InstanceShownRecord extends Record {
    private static final String TABLE_INSTANCES_SHOWN = "instancesShown";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TASK_ID = "taskId";
    private static final String COLUMN_SCHEDULE_YEAR = "scheduleYear";
    private static final String COLUMN_SCHEDULE_MONTH = "scheduleMonth";
    private static final String COLUMN_SCHEDULE_DAY = "scheduleDay";
    private static final String COLUMN_SCHEDULE_CUSTOM_TIME_ID = "scheduleCustomTimeId";
    private static final String COLUMN_SCHEDULE_HOUR = "scheduleHour";
    private static final String COLUMN_SCHEDULE_MINUTE = "scheduleMinute";
    private static final String COLUMN_NOTIFIED = "notified";
    private static final String COLUMN_NOTIFICATION_SHOWN = "notificationShown";

    private final int mId;
    private final String mTaskId;

    private final int mScheduleYear;
    private final int mScheduleMonth;
    private final int mScheduleDay;

    private final Integer mScheduleCustomTimeId;

    private final Integer mScheduleHour;
    private final Integer mScheduleMinute;

    private boolean mNotified;
    private boolean mNotificationShown;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_INSTANCES_SHOWN
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TASK_ID + " INTEGER NOT NULL REFERENCES " + TaskRecord.TABLE_TASKS + "(" + TaskRecord.COLUMN_ID + "), "
                + COLUMN_SCHEDULE_YEAR + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_MONTH + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_DAY + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_SCHEDULE_HOUR + " INTEGER, "
                + COLUMN_SCHEDULE_MINUTE + " INTEGER, "
                + COLUMN_NOTIFIED + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_NOTIFICATION_SHOWN + " INTEGER NOT NULL DEFAULT 0);");
    }

    @SuppressWarnings({"UnusedParameters", "EmptyMethod"})
    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }

    @NonNull
    static ArrayList<InstanceShownRecord> getInstancesShownRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<InstanceShownRecord> instancesShownRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_INSTANCES_SHOWN, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            instancesShownRecords.add(cursorToInstanceShownRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return instancesShownRecords;
    }

    @SuppressWarnings("ConstantConditions")
    private static InstanceShownRecord cursorToInstanceShownRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        String taskId = cursor.getString(1);
        int scheduleYear = cursor.getInt(2);
        int scheduleMonth = cursor.getInt(3);
        int scheduleDay = cursor.getInt(4);
        Integer scheduleCustomTimeId = (cursor.isNull(5) ? null : cursor.getInt(5));
        Integer scheduleHour = (cursor.isNull(6) ? null : cursor.getInt(6));
        Integer scheduleMinute = (cursor.isNull(7) ? null : cursor.getInt(7));
        boolean notified = (cursor.getInt(8) == 1);
        boolean notificationShown = (cursor.getInt(9) == 1);

        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        return new InstanceShownRecord(true, id, taskId, scheduleYear, scheduleMonth, scheduleDay, scheduleCustomTimeId, scheduleHour, scheduleMinute, notified, notificationShown);
    }

    static int getMaxId(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);
        return getMaxId(sqLiteDatabase, TABLE_INSTANCES_SHOWN, COLUMN_ID);
    }

    InstanceShownRecord(boolean created, int id, @NonNull String taskId, int scheduleYear, int scheduleMonth, int scheduleDay, Integer scheduleCustomTimeId, Integer scheduleHour, Integer scheduleMinute, boolean notified, boolean notificationShown) {
        super(created);

        Assert.assertTrue((scheduleHour == null) == (scheduleMinute == null));
        Assert.assertTrue((scheduleHour == null) != (scheduleCustomTimeId == null));

        mId = id;
        mTaskId = taskId;

        mScheduleYear = scheduleYear;
        mScheduleMonth = scheduleMonth;
        mScheduleDay = scheduleDay;

        mScheduleCustomTimeId = scheduleCustomTimeId;

        mScheduleHour = scheduleHour;
        mScheduleMinute = scheduleMinute;

        mNotified = notified;
        mNotificationShown = notificationShown;
    }

    public int getId() {
        return mId;
    }

    @NonNull
    public String getTaskId() {
        return mTaskId;
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

    public boolean getNotified() {
        return mNotified;
    }

    public boolean getNotificationShown() {
        return mNotificationShown;
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
        contentValues.put(COLUMN_SCHEDULE_YEAR, mScheduleYear);
        contentValues.put(COLUMN_SCHEDULE_MONTH, mScheduleMonth);
        contentValues.put(COLUMN_SCHEDULE_DAY, mScheduleDay);
        contentValues.put(COLUMN_SCHEDULE_CUSTOM_TIME_ID, mScheduleCustomTimeId);
        contentValues.put(COLUMN_SCHEDULE_HOUR, mScheduleHour);
        contentValues.put(COLUMN_SCHEDULE_MINUTE, mScheduleMinute);
        contentValues.put(COLUMN_NOTIFIED, mNotified ? 1 : 0);
        contentValues.put(COLUMN_NOTIFICATION_SHOWN, mNotificationShown ? 1 : 0);
        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_INSTANCES_SHOWN, COLUMN_ID, mId);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_INSTANCES_SHOWN);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_INSTANCES_SHOWN, COLUMN_ID, mId);
    }
}
