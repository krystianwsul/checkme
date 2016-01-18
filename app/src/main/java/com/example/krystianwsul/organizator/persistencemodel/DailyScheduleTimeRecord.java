package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

public class DailyScheduleTimeRecord {
    private static final String TABLE_DAILY_SCHEDULE_TIMES = "dailyScheduleTimes";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mId;
    private final int mScheduleId;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_DAILY_SCHEDULE_TIMES
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER, "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY_SCHEDULE_TIMES);
        onCreate(sqLiteDatabase);
    }

    DailyScheduleTimeRecord(int id, int scheduleId, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mId = id;
        mScheduleId = scheduleId;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getId() {
        return mId;
    }

    public int getScheduleId() {
        return mScheduleId;
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
