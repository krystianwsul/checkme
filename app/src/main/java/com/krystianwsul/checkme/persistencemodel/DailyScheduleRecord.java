package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class DailyScheduleRecord extends Record {
    static final String TABLE_DAILY_SCHEDULES = "dailySchedules";

    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_CUSTOM_TIME_ID = "customTimeId";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";

    private final int mScheduleId;

    private final Integer mCustomTimeId;

    private final Integer mHour;
    private final Integer mMinute;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_DAILY_SCHEDULES
                + " (" + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL UNIQUE REFERENCES " + ScheduleRecord.TABLE_SCHEDULES + "(" + ScheduleRecord.COLUMN_ID + "), "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    @Deprecated
    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion < 16) {
            sqLiteDatabase.delete(TABLE_DAILY_SCHEDULES, COLUMN_SCHEDULE_ID + " NOT IN (SELECT " + ScheduleRecord.COLUMN_ID + " FROM " + ScheduleRecord.TABLE_SCHEDULES + ")", null);
        }
    }

    @NonNull
    static List<DailyScheduleRecord> getDailyScheduleRecords(@NonNull SQLiteDatabase sqLiteDatabase) {
        List<DailyScheduleRecord> dailyScheduleTimeRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_DAILY_SCHEDULES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            dailyScheduleTimeRecords.add(cursorToDailyScheduleTimeRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return dailyScheduleTimeRecords;
    }

    private static DailyScheduleRecord cursorToDailyScheduleTimeRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int scheduleId = cursor.getInt(0);
        Integer customTimeId = (cursor.isNull(1) ? null : cursor.getInt(1));
        Integer hour = (cursor.isNull(2) ? null : cursor.getInt(2));
        Integer minute = (cursor.isNull(3) ? null : cursor.getInt(3));

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        return new DailyScheduleRecord(true, scheduleId, customTimeId, hour, minute);
    }

    DailyScheduleRecord(boolean created, int scheduleId, Integer customTimeId, Integer hour, Integer minute) {
        super(created);

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        mScheduleId = scheduleId;

        mCustomTimeId = customTimeId;

        mHour = hour;
        mMinute = minute;
    }

    int getScheduleId() {
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

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SCHEDULE_ID, mScheduleId);
        contentValues.put(COLUMN_CUSTOM_TIME_ID, mCustomTimeId);
        contentValues.put(COLUMN_HOUR, mHour);
        contentValues.put(COLUMN_MINUTE, mMinute);
        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_DAILY_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_DAILY_SCHEDULES);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_DAILY_SCHEDULES, COLUMN_SCHEDULE_ID, mScheduleId);
    }
}
