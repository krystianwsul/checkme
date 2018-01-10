package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class MonthlyDayScheduleRecord extends Record {
    static final String TABLE_MONTHLY_DAY_SCHEDULES = "monthlyDaySchedules";

    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_DAY_OF_MONTH = "dayOfMonth";
    private static final String COLUMN_BEGINNING_OF_MONTH = "beginningOfMonth";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mScheduleId;

    private final int mDayOfMonth;
    private final boolean mBeginningOfMonth;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_MONTHLY_DAY_SCHEDULES
                + " (" + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL UNIQUE REFERENCES " + ScheduleRecord.TABLE_SCHEDULES + "(" + ScheduleRecord.COLUMN_ID + "), "
                + COLUMN_DAY_OF_MONTH + " INTEGER NOT NULL, "
                + COLUMN_BEGINNING_OF_MONTH + " INTEGER NOT NULL, "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.Companion.getTABLE_CUSTOM_TIMES() + "(" + CustomTimeRecord.Companion.getCOLUMN_ID() + "), "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    @NonNull
    static List<MonthlyDayScheduleRecord> getMonthlyDayScheduleRecords(@NonNull SQLiteDatabase sqLiteDatabase) {
        List<MonthlyDayScheduleRecord> monthlyDayScheduleRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_MONTHLY_DAY_SCHEDULES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            monthlyDayScheduleRecords.add(cursorToWeeklyScheduleRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return monthlyDayScheduleRecords;
    }

    @NonNull
    private static MonthlyDayScheduleRecord cursorToWeeklyScheduleRecord(@NonNull Cursor cursor) {
        int scheduleId = cursor.getInt(0);
        int dayOfMonth = cursor.getInt(1);
        boolean beginningOfMonth = (cursor.getInt(2) == 1);
        Integer customTimeId = (cursor.isNull(3) ? null : cursor.getInt(3));
        Integer hour = (cursor.isNull(4) ? null : cursor.getInt(4));
        Integer minute = (cursor.isNull(5) ? null : cursor.getInt(5));

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        return new MonthlyDayScheduleRecord(true, scheduleId, dayOfMonth, beginningOfMonth, customTimeId, hour, minute);
    }

    MonthlyDayScheduleRecord(boolean created, int scheduleId, int dayOfMonth, boolean beginningOfMonth, @Nullable Integer customTimeId, @Nullable Integer hour, @Nullable Integer minute) {
        super(created);

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mScheduleId = scheduleId;

        mDayOfMonth = dayOfMonth;
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

    @NonNull
    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SCHEDULE_ID, mScheduleId);
        contentValues.put(COLUMN_DAY_OF_MONTH, mDayOfMonth);
        contentValues.put(COLUMN_BEGINNING_OF_MONTH, mBeginningOfMonth ? 1 : 0);
        contentValues.put(COLUMN_CUSTOM_TIME_ID, mCustomTimeId);
        contentValues.put(COLUMN_HOUR, mHour);
        contentValues.put(COLUMN_MINUTE, mMinute);
        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_MONTHLY_DAY_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_MONTHLY_DAY_SCHEDULES);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_MONTHLY_DAY_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }
}
