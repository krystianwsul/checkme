package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

public class WeeklyScheduleDayOfWeekTimeRecord {
    private static final String TABLE_WEEKLY_SCHEDULE_DAY_OF_WEEK_TIMES = "weeklyScheduleDayOfWeekTimes";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_DAY_OF_WEEK = "dayOfWeek";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mId;
    private final int mScheduleId;

    private final int mDayOfWeek;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_WEEKLY_SCHEDULE_DAY_OF_WEEK_TIMES
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL, "
                + COLUMN_DAY_OF_WEEK + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER, "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_WEEKLY_SCHEDULE_DAY_OF_WEEK_TIMES);
        onCreate(sqLiteDatabase);
    }

    WeeklyScheduleDayOfWeekTimeRecord(int id, int scheduleId, int dayOfWeek, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mId = id;
        mScheduleId = scheduleId;

        mDayOfWeek = dayOfWeek;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleId() {
        return mScheduleId;
    }

    public int getDayOfWeek() {
        return mDayOfWeek;
    }

    public Integer getCustomTimeId() {
        return mCustomTimeId;
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }
}
