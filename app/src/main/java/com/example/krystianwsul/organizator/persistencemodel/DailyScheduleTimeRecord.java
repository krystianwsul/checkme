package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

import java.util.ArrayList;

public class DailyScheduleTimeRecord extends Record {
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
                + COLUMN_SCHEDULE_ID + " INTEGER NOT NULL REFERENCES " + ScheduleRecord.TABLE_SCHEDULES + "(" + ScheduleRecord.COLUMN_ID + "), "
                + COLUMN_CUSTOM_TIME_ID + " INTEGER REFERENCES " + CustomTimeRecord.TABLE_CUSTOM_TIMES + "(" + CustomTimeRecord.COLUMN_ID + "), "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER);");
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY_SCHEDULE_TIMES);
        onCreate(sqLiteDatabase);
    }

    public static ArrayList<DailyScheduleTimeRecord> getDailyScheduleTimeRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_DAILY_SCHEDULE_TIMES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            dailyScheduleTimeRecords.add(cursorToDailyScheduleTimeRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return dailyScheduleTimeRecords;
    }

    private static DailyScheduleTimeRecord cursorToDailyScheduleTimeRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        int scheduleId = cursor.getInt(1);
        Integer customTimeId = (cursor.isNull(2) ? null : cursor.getInt(2));
        Integer hour = (cursor.isNull(3) ? null : cursor.getInt(3));
        Integer minute = (cursor.isNull(4) ? null : cursor.getInt(4));

        Assert.assertTrue((hour == null) == (minute == null));
        Assert.assertTrue((hour == null) || (customTimeId == null));
        Assert.assertTrue((hour != null) || (customTimeId != null));

        return new DailyScheduleTimeRecord(true, id, scheduleId, customTimeId, hour, minute);
    }

    DailyScheduleTimeRecord(boolean created, int id, int scheduleId, Integer customTimeId, Integer hour, Integer minute) {
        super(created);

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

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SCHEDULE_ID, mScheduleId);
        contentValues.put(COLUMN_CUSTOM_TIME_ID, mCustomTimeId);
        contentValues.put(COLUMN_HOUR, mHour);
        contentValues.put(COLUMN_MINUTE, mMinute);
        return contentValues;
    }

    @Override
    void update(SQLiteDatabase sqLiteDatabase) {
        update(sqLiteDatabase, TABLE_DAILY_SCHEDULE_TIMES, COLUMN_ID, mId);
    }

    @Override
    void create(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        if (mCreated)
            return;

        long insertId = create(sqLiteDatabase, TABLE_DAILY_SCHEDULE_TIMES);
        Assert.assertTrue(insertId == mId);
    }
}
