package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.ArrayList;

public class CustomTimeRecord extends Record {
    static final String TABLE_CUSTOM_TIMES = "customTimes";

    static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SUNDAY_HOUR = "sundayHour";
    private static final String COLUMN_SUNDAY_MINUTE = "sundayMinute";
    private static final String COLUMN_MONDAY_HOUR = "mondayHour";
    private static final String COLUMN_MONDAY_MINUTE = "mondayMinute";
    private static final String COLUMN_TUESDAY_HOUR = "tuesdayHour";
    private static final String COLUMN_TUESDAY_MINUTE = "tuesdayMinute";
    private static final String COLUMN_WEDNESDAY_HOUR = "wednesdayHour";
    private static final String COLUMN_WEDNESDAY_MINUTE = "wednesdayMinute";
    private static final String COLUMN_THURSDAY_HOUR = "thursdayHour";
    private static final String COLUMN_THURSDAY_MINUTE = "thursdayMinute";
    private static final String COLUMN_FRIDAY_HOUR = "fridayHour";
    private static final String COLUMN_FRIDAY_MINUTE = "fridayMinute";
    private static final String COLUMN_SATURDAY_HOUR = "saturdayHour";
    private static final String COLUMN_SATURDAY_MINUTE = "saturdayMinute";
    private static final String COLUMN_CURRENT = "current";

    private final int mId;
    private String mName;

    private int mSundayHour;
    private int mSundayMinute;

    private int mMondayHour;
    private int mMondayMinute;

    private int mTuesdayHour;
    private int mTuesdayMinute;

    private int mWednesdayHour;
    private int mWednesdayMinute;

    private int mThursdayHour;
    private int mThursdayMinute;

    private int mFridayHour;
    private int mFridayMinute;

    private int mSaturdayHour;
    private int mSaturdayMinute;

    private boolean mCurrent;

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_CUSTOM_TIMES
                + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_SUNDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_SUNDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_MONDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_MONDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_TUESDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_TUESDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_WEDNESDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_WEDNESDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_THURSDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_THURSDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_FRIDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_FRIDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_SATURDAY_HOUR + " INTEGER NOT NULL, "
                + COLUMN_SATURDAY_MINUTE + " INTEGER NOT NULL, "
                + COLUMN_CURRENT + " INTEGER NOT NULL DEFAULT 1);");
    }

    @SuppressWarnings("UnusedParameters")
    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Assert.assertTrue(sqLiteDatabase != null);

        if (oldVersion <= 8) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_CUSTOM_TIMES
                    + " ADD COLUMN relevant INTEGER NOT NULL DEFAULT 1");

            sqLiteDatabase.execSQL("CREATE INDEX customTimesIndexRelevant ON " + TABLE_CUSTOM_TIMES + "(relevant DESC)");
        }

        if (oldVersion < 16) {
            String columnList = COLUMN_NAME
                    + ", " + COLUMN_SUNDAY_HOUR
                    + ", " + COLUMN_SUNDAY_MINUTE
                    + ", " + COLUMN_MONDAY_HOUR
                    + ", " + COLUMN_MONDAY_MINUTE
                    + ", " + COLUMN_TUESDAY_HOUR
                    + ", " + COLUMN_TUESDAY_MINUTE
                    + ", " + COLUMN_WEDNESDAY_HOUR
                    + ", " + COLUMN_WEDNESDAY_MINUTE
                    + ", " + COLUMN_THURSDAY_HOUR
                    + ", " + COLUMN_THURSDAY_MINUTE
                    + ", " + COLUMN_FRIDAY_HOUR
                    + ", " + COLUMN_FRIDAY_MINUTE
                    + ", " + COLUMN_SATURDAY_HOUR
                    + ", " + COLUMN_SATURDAY_MINUTE
                    + ", " + COLUMN_CURRENT;

            sqLiteDatabase.execSQL("DROP INDEX customTimesIndexRelevant");
            sqLiteDatabase.execSQL(
                    "DROP TABLE t1_backup IF EXISTS;" + "CREATE TEMPORARY TABLE t1_backup(" + columnList + ");" +
                            "INSERT INTO t1_backup SELECT " + columnList + " FROM " + TABLE_CUSTOM_TIMES + ";" +
                            "DROP TABLE " + TABLE_CUSTOM_TIMES + ";" +
                            "CREATE TABLE " + TABLE_CUSTOM_TIMES
                            + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + COLUMN_NAME + " TEXT NOT NULL, "
                            + COLUMN_SUNDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_SUNDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_MONDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_MONDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_TUESDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_TUESDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_WEDNESDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_WEDNESDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_THURSDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_THURSDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_FRIDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_FRIDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_SATURDAY_HOUR + " INTEGER NOT NULL, "
                            + COLUMN_SATURDAY_MINUTE + " INTEGER NOT NULL, "
                            + COLUMN_CURRENT + " INTEGER NOT NULL DEFAULT 1);" +
                            "INSERT INTO " + TABLE_CUSTOM_TIMES + " SELECT * FROM t1_backup;" +
                            "DROP TABLE t1_backup;");
        }
    }

    static ArrayList<CustomTimeRecord> getCustomTimeRecords(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        ArrayList<CustomTimeRecord> customTimeRecords = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_CUSTOM_TIMES, null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            customTimeRecords.add(cursorToCustomTimeRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return customTimeRecords;
    }

    private static CustomTimeRecord cursorToCustomTimeRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        int id = cursor.getInt(0);
        String name = cursor.getString(1);
        int sundayHour = cursor.getInt(2);
        int sundayMinute = cursor.getInt(3);
        int mondayHour = cursor.getInt(4);
        int mondayMinute = cursor.getInt(5);
        int tuesdayHour = cursor.getInt(6);
        int tuesdayMinute = cursor.getInt(7);
        int wednesdayHour = cursor.getInt(8);
        int wednesdayMinute = cursor.getInt(9);
        int thursdayHour = cursor.getInt(10);
        int thursdayMinute = cursor.getInt(11);
        int fridayHour = cursor.getInt(12);
        int fridayMinute = cursor.getInt(13);
        int saturdayHour = cursor.getInt(14);
        int saturdayMinute = cursor.getInt(15);
        boolean current = (cursor.getInt(16) == 1);

        return new CustomTimeRecord(true, id, name, sundayHour, sundayMinute, mondayHour, mondayMinute, tuesdayHour, tuesdayMinute, wednesdayHour, wednesdayMinute, thursdayHour, thursdayMinute, fridayHour, fridayMinute, saturdayHour, saturdayMinute, current);
    }

    static int getMaxId(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);
        return getMaxId(sqLiteDatabase, TABLE_CUSTOM_TIMES, COLUMN_ID);
    }

    CustomTimeRecord(boolean created, int id, String name, int sundayHour, int sundayMinute, int mondayHour, int mondayMinute, int tuesdayHour, int tuesdayMinute, int wednesdayHour, int wednesdayMinute, int thursdayHour, int thursdayMinute, int fridayHour, int fridayMinute, int saturdayHour, int saturdayMinute, boolean current) {
        super(created);

        Assert.assertTrue(!TextUtils.isEmpty(name));

        mId = id;
        mName = name;

        mSundayHour = sundayHour;
        mSundayMinute = sundayMinute;

        mMondayHour = mondayHour;
        mMondayMinute = mondayMinute;

        mTuesdayHour = tuesdayHour;
        mTuesdayMinute = tuesdayMinute;

        mWednesdayHour = wednesdayHour;
        mWednesdayMinute = wednesdayMinute;

        mThursdayHour = thursdayHour;
        mThursdayMinute = thursdayMinute;

        mFridayHour = fridayHour;
        mFridayMinute = fridayMinute;

        mSaturdayHour = saturdayHour;
        mSaturdayMinute = saturdayMinute;

        mCurrent = current;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mName = name;
        mChanged = true;
    }

    public int getSundayHour() {
        return mSundayHour;
    }

    public int getSundayMinute() {
        return mSundayMinute;
    }

    public int getMondayHour() {
        return mMondayHour;
    }

    public int getMondayMinute() {
        return mMondayMinute;
    }

    public int getTuesdayHour() {
        return mTuesdayHour;
    }

    public int getTuesdayMinute() {
        return mTuesdayMinute;
    }

    public int getWednesdayHour() {
        return mWednesdayHour;
    }

    public int getWednesdayMinute() {
        return mWednesdayMinute;
    }

    public int getThursdayHour() {
        return mThursdayHour;
    }

    public int getThursdayMinute() {
        return mThursdayMinute;
    }

    public int getFridayHour() {
        return mFridayHour;
    }

    public int getFridayMinute() {
        return mFridayMinute;
    }

    public int getSaturdayHour() {
        return mSaturdayHour;
    }

    public int getSaturdayMinute() {
        return mSaturdayMinute;
    }

    public boolean getCurrent() {
        return mCurrent;
    }

    public void setSundayHour(int hour) {
        mSundayHour = hour;
        mChanged = true;
    }

    public void setSundayMinute(int minute) {
        mSundayMinute = minute;
        mChanged = true;
    }

    public void setMondayHour(int hour) {
        mMondayHour = hour;
        mChanged = true;
    }

    public void setMondayMinute(int minute) {
        mMondayMinute = minute;
        mChanged = true;
    }

    public void setTuesdayHour(int hour) {
        mTuesdayHour = hour;
        mChanged = true;
    }

    public void setTuesdayMinute(int minute) {
        mTuesdayMinute = minute;
        mChanged = true;
    }

    public void setWednesdayHour(int hour) {
        mWednesdayHour = hour;
        mChanged = true;
    }

    public void setWednesdayMinute(int minute) {
        mWednesdayMinute = minute;
        mChanged = true;
    }

    public void setThursdayHour(int hour) {
        mThursdayHour = hour;
        mChanged = true;
    }

    public void setThursdayMinute(int minute) {
        mThursdayMinute = minute;
        mChanged = true;
    }

    public void setFridayHour(int hour) {
        mFridayHour = hour;
        mChanged = true;
    }

    public void setFridayMinute(int minute) {
        mFridayMinute = minute;
        mChanged = true;
    }

    public void setSaturdayHour(int hour) {
        mSaturdayHour = hour;
        mChanged = true;
    }

    public void setSaturdayMinute(int minute) {
        mSaturdayMinute = minute;
        mChanged = true;
    }

    public void setCurrent(boolean current) {
        mCurrent = current;
        mChanged = true;
    }

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, mName);
        contentValues.put(COLUMN_SUNDAY_HOUR, mSundayHour);
        contentValues.put(COLUMN_SUNDAY_MINUTE, mSundayMinute);
        contentValues.put(COLUMN_MONDAY_HOUR, mMondayHour);
        contentValues.put(COLUMN_MONDAY_MINUTE, mMondayMinute);
        contentValues.put(COLUMN_TUESDAY_HOUR, mTuesdayHour);
        contentValues.put(COLUMN_TUESDAY_MINUTE, mTuesdayMinute);
        contentValues.put(COLUMN_WEDNESDAY_HOUR, mWednesdayHour);
        contentValues.put(COLUMN_WEDNESDAY_MINUTE, mWednesdayMinute);
        contentValues.put(COLUMN_THURSDAY_HOUR, mThursdayHour);
        contentValues.put(COLUMN_THURSDAY_MINUTE, mThursdayMinute);
        contentValues.put(COLUMN_FRIDAY_HOUR, mFridayHour);
        contentValues.put(COLUMN_FRIDAY_MINUTE, mFridayMinute);
        contentValues.put(COLUMN_SATURDAY_HOUR, mSaturdayHour);
        contentValues.put(COLUMN_SATURDAY_MINUTE, mSaturdayMinute);
        contentValues.put(COLUMN_CURRENT, mCurrent);

        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        return getUpdateCommand(TABLE_CUSTOM_TIMES, COLUMN_ID, mId);
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_CUSTOM_TIMES);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        return getDeleteCommand(TABLE_CUSTOM_TIMES, COLUMN_ID, mId);
    }
}
