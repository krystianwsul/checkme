package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class MonthlyWeekScheduleRecord extends Record {
    private static final String TABLE_MONTHLY_WEEK_SCHEDULES = "monthlyWeekSchedules";

    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_DAY_OF_MONTH = "dayOfMonth";
    private static final String COLUMN_DAY_OF_WEEK = "dayOfWeek";
    private static final String COLUMN_BEGINNING_OF_MONTH = "beginningOfMonth";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mScheduleId;

    private final int mDayOfMonth;
    private final int mDayOfWeek;
    private final boolean mBeginningOfMonth;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_MONTHLY_WEEK_SCHEDULES
                + " (" + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL UNIQUE REFERENCES " + ScheduleRecord.TABLE_SCHEDULES + "(" + ScheduleRecord.COLUMN_ID + "), "
                + COLUMN_DAY_OF_MONTH + " INTEGER NOT NULL, "
                + COLUMN_DAY_OF_WEEK + " INTEGER NOT NULL, "
                + COLUMN_BEGINNING_OF_MONTH + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    @Deprecated
    public static void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion <= 13)
            onCreate(sqLiteDatabase);

        if (oldVersion < 16) {
            sqLiteDatabase.delete(TABLE_MONTHLY_WEEK_SCHEDULES, COLUMN_SCHEDULE_ID + " NOT IN (SELECT " + ScheduleRecord.COLUMN_ID + " FROM " + ScheduleRecord.TABLE_SCHEDULES + ")", null);
        }
    }

    @NonNull
    static List<MonthlyWeekScheduleRecord> getMonthlyWeekScheduleRecords(@NonNull SQLiteDatabase sqLiteDatabase) {
        List<MonthlyWeekScheduleRecord> monthlyWeekScheduleRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_MONTHLY_WEEK_SCHEDULES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            monthlyWeekScheduleRecords.add(cursorToMonthlyWeekScheduleRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return monthlyWeekScheduleRecords;
    }

    @NonNull
    private static MonthlyWeekScheduleRecord cursorToMonthlyWeekScheduleRecord(@NonNull Cursor cursor) {
        int scheduleId = cursor.getInt(0);
        int dayOfMonth = cursor.getInt(1);
        int dayOfWeek = cursor.getInt(2);
        boolean beginningOfMonth = (cursor.getInt(3) == 1);
        Integer customTimeId = (cursor.isNull(4) ? null : cursor.getInt(4));
        Integer hour = (cursor.isNull(5) ? null : cursor.getInt(5));
        Integer minute = (cursor.isNull(6) ? null : cursor.getInt(6));

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        return new MonthlyWeekScheduleRecord(true, scheduleId, dayOfMonth, dayOfWeek, beginningOfMonth, customTimeId, hour, minute);
    }

    MonthlyWeekScheduleRecord(boolean created, int scheduleId, int dayOfMonth, int dayOfWeek, boolean beginningOfMonth, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(created);

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mScheduleId = scheduleId;

        mDayOfMonth = dayOfMonth;
        mDayOfWeek = dayOfWeek;
        mBeginningOfMonth = beginningOfMonth;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    int getScheduleId() {
        return mScheduleId;
    }

    public int getDayOfMonth() {
        return mDayOfMonth;
    }

    public int getDayOfWeek() {
        return mDayOfWeek;
    }

    public boolean getBeginningOfMonth() {
        return mBeginningOfMonth;
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
        contentValues.put(COLUMN_DAY_OF_MONTH, mDayOfMonth);
        contentValues.put(COLUMN_DAY_OF_WEEK, mDayOfWeek);
        contentValues.put(COLUMN_BEGINNING_OF_MONTH, mBeginningOfMonth ? 1 : 0);
        contentValues.put(COLUMN_CUSTOM_TIME_ID, mCustomTimeId);
        contentValues.put(COLUMN_HOUR, mHour);
        contentValues.put(COLUMN_MINUTE, mMinute);
        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_MONTHLY_WEEK_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_MONTHLY_WEEK_SCHEDULES);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_MONTHLY_WEEK_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }
}
