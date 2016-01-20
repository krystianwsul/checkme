package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

import java.util.ArrayList;

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
                + " (" + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL REFERENCES " + ScheduleRecord.TABLE_SCHEDULES + "(" + ScheduleRecord.COLUMN_ID + "), "
                + COLUMN_YEAR + " INTEGER NOT NULL, "
                + COLUMN_MONTH + " INTEGER NOT NULL, "
                + COLUMN_DAY + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SINGLE_SCHEDULE_DATE_TIMES);
        onCreate(sqLiteDatabase);
    }

    public static SingleScheduleDateTimeRecord createSingleScheduleDateTimeRecord(SQLiteDatabase sqLiteDatabase, int scheduleId, int year, int month, int day, Integer customTimeId, Integer hour, Integer minute) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SCHEDULE_ID, scheduleId);
        contentValues.put(COLUMN_YEAR, year);
        contentValues.put(COLUMN_MONTH, month);
        contentValues.put(COLUMN_DAY, day);
        contentValues.put(COLUMN_CUSTOM_TIME_ID, customTimeId);
        contentValues.put(COLUMN_HOUR, hour);
        contentValues.put(COLUMN_MINUTE, minute);

        sqLiteDatabase.insert(TABLE_SINGLE_SCHEDULE_DATE_TIMES, null, contentValues);

        Cursor cursor = sqLiteDatabase.query(TABLE_SINGLE_SCHEDULE_DATE_TIMES, null, COLUMN_SCHEDULE_ID + " = " + scheduleId, null, null, null, null);
        cursor.moveToFirst();

        SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = cursorToSingleScheduleDateTimeRecord(cursor);
        Assert.assertTrue(singleScheduleDateTimeRecord != null);

        cursor.close();
        return singleScheduleDateTimeRecord;
    }

    public static ArrayList<SingleScheduleDateTimeRecord> getSingleScheduleDateTimeRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<SingleScheduleDateTimeRecord> singleScheduleDateTimeRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_SINGLE_SCHEDULE_DATE_TIMES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            singleScheduleDateTimeRecords.add(cursorToSingleScheduleDateTimeRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return singleScheduleDateTimeRecords;
    }

    private static SingleScheduleDateTimeRecord cursorToSingleScheduleDateTimeRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int scheduleId = cursor.getInt(0);
        int year = cursor.getInt(1);
        int month = cursor.getInt(2);
        int day = cursor.getInt(3);
        Integer customTimeId = (cursor.isNull(4) ? null : cursor.getInt(4));
        Integer hour = (cursor.isNull(5) ? null : cursor.getInt(5));
        Integer minute = (cursor.isNull(6) ? null : cursor.getInt(6));

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        return new SingleScheduleDateTimeRecord(scheduleId, year, month, day, customTimeId, hour, minute);
    }

    private SingleScheduleDateTimeRecord(int scheduleId, int year, int month, int day, Integer customTimeId, Integer hour, Integer minute) {
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
