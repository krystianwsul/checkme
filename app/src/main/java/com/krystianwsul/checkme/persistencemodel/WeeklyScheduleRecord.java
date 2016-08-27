package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class WeeklyScheduleRecord extends Record {
    private static final String TABLE_WEEKLY_SCHEDULES = "weeklySchedules";

    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_DAY_OF_WEEK = "dayOfWeek";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mScheduleId;

    private final int mDayOfWeek;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_WEEKLY_SCHEDULES
                + " (" + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL UNIQUE REFERENCES " + ScheduleRecord.TABLE_SCHEDULES + "(" + ScheduleRecord.COLUMN_ID + "), "
                + COLUMN_DAY_OF_WEEK + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }

    public static ArrayList<WeeklyScheduleRecord> getWeeklyScheduleRecords(SQLiteDatabase sqLiteDatabase, List<Integer> scheduleIds) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(scheduleIds != null);
        Assert.assertTrue(!scheduleIds.isEmpty());

        ArrayList<WeeklyScheduleRecord> weeklyScheduleDayOfWeekTimeRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_WEEKLY_SCHEDULES, null, COLUMN_SCHEDULE_ID + " IN (" + TextUtils.join(", ", scheduleIds) + ")", null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            weeklyScheduleDayOfWeekTimeRecords.add(cursorToWeeklyScheduleRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return weeklyScheduleDayOfWeekTimeRecords;
    }

    private static WeeklyScheduleRecord cursorToWeeklyScheduleRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int scheduleId = cursor.getInt(0);
        int dayOfWeek = cursor.getInt(1);
        Integer customTimeId = (cursor.isNull(2) ? null : cursor.getInt(2));
        Integer hour = (cursor.isNull(3) ? null : cursor.getInt(3));
        Integer minute = (cursor.isNull(4) ? null : cursor.getInt(4));

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        return new WeeklyScheduleRecord(true, scheduleId, dayOfWeek, customTimeId, hour, minute);
    }

    WeeklyScheduleRecord(boolean created, int scheduleId, int dayOfWeek, Integer customTimeId, Integer hour, Integer minute) {
        super(created);

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mScheduleId = scheduleId;

        mDayOfWeek = dayOfWeek;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    public int getScheduleId() {
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

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SCHEDULE_ID, mScheduleId);
        contentValues.put(COLUMN_DAY_OF_WEEK, mDayOfWeek);
        contentValues.put(COLUMN_CUSTOM_TIME_ID, mCustomTimeId);
        contentValues.put(COLUMN_HOUR, mHour);
        contentValues.put(COLUMN_MINUTE, mMinute);
        return contentValues;
    }

    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_WEEKLY_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }

    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_WEEKLY_SCHEDULES);
    }
}
