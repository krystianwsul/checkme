package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

public class InstanceRecord {
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
                + COLUMN_TASK_ID + " INTEGER NOT NULL, "
                + COLUMN_DONE + " INTEGER, "
                + COLUMN_SCHEDULE_YEAR + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_MONTH + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_DAY + " INTEGER NOT NULL, "
                + COLUMN_SCHEDULE_CUSTOM_TIME_ID + " INTEGER, "
                + COLUMN_SCHEDULE_HOUR + " INTEGER, "
                + COLUMN_SCHEDULE_MINUTE + " INTEGER, "
                + COLUMN_INSTANCE_YEAR + " INTEGER, "
                + COLUMN_INSTANCE_MONTH + " INTEGER, "
                + COLUMN_INSTANCE_DAY + " INTEGER, "
                + COLUMN_INSTANCE_CUSTOM_TIME_ID + " INTEGER, "
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

    public InstanceRecord(int id, int taskId, Long done, int scheduleYear, int scheduleMonth, int scheduleDay, Integer scheduleCustomTimeId, Integer scheduleHour, Integer scheduleMinute, Integer instanceYear, Integer instanceMonth, Integer instanceDay, Integer instanceCustomTimeId, Integer instanceHour, Integer instanceMinute, long hierarchyTime, boolean notified, boolean notificationShown) {
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

    public void setInstanceYear(int instanceYear) {
        mInstanceYear = instanceYear;
    }

    public Integer getInstanceMonth() {
        return mInstanceMonth;
    }

    public void setInstanceMonth(int instanceMonth) {
        mInstanceMonth = instanceMonth;
    }

    public Integer getInstanceDay() {
        return mInstanceDay;
    }

    public void setInstanceDay(int instanceDay) {
        mInstanceDay = instanceDay;
    }

    public Integer getInstanceCustomTimeId() {
        return mInstanceCustomTimeId;
    }

    public void setInstanceCustomTimeId(int instanceCustomTimeId) {
        mInstanceCustomTimeId = instanceCustomTimeId;
    }

    public Integer getInstanceHour() {
        return mInstanceHour;
    }

    public void setInstanceHour(int instanceHour) {
        mInstanceHour = instanceHour;
    }

    public Integer getInstanceMinute() {
        return mInstanceMinute;
    }

    public void setInstanceMinute(int instanceMinute) {
        mInstanceMinute = instanceMinute;
    }

    public void setDone(Long done) {
        mDone = done;
    }

    public long getHierarchyTime() {
        return mHierarchyTime;
    }

    public boolean getNotified() {
        return mNotified;
    }

    public void setNotified(boolean notified) {
        mNotified = notified;
    }

    public boolean getNotificationShown() {
        return mNotificationShown;
    }

    public void setNotificationShown(boolean notificationShown) {
        mNotificationShown = notificationShown;
    }
}
