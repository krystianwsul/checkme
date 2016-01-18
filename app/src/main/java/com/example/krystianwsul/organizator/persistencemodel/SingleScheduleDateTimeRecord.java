package com.example.krystianwsul.organizator.persistencemodel;

import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;
public class SingleScheduleDateTimeRecord {
    private static final String TABLE_SINGLE_SCHEDULE_DATE_TIMES = "singleScheduleDateTimes";

    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_MONTH = "month";
    private static final String COLUMN_DAY = "day";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mScheduleId;

    private final int mYear;
    private final int mMonth;
    private final int mDay;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_SINGLE_SCHEDULE_DATE_TIMES
                + " (" + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL, "
                + COLUMN_YEAR + " INTEGER NOT NULL, "
                + COLUMN_MONTH + " INTEGER NOT NULL, "
                + COLUMN_DAY + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER, "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SINGLE_SCHEDULE_DATE_TIMES);
        onCreate(sqLiteDatabase);
    }

    SingleScheduleDateTimeRecord(int scheduleId, int year, int month, int day, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mScheduleId = scheduleId;

        mYear = year;
        mMonth = month;
        mDay = day;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getScheduleId() {
        return mScheduleId;
    }

    public int getYear() {
        return mYear;
    }

    public int getMonth() {
        return mMonth;
    }

    public int getDay() {
        return mDay;
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
