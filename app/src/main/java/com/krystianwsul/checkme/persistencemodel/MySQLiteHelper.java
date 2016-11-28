package com.krystianwsul.checkme.persistencemodel;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

class MySQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 19;

    private static SQLiteDatabase sSQLiteDatabase;

    @NonNull
    static SQLiteDatabase getDatabase(Context applicationContext) {
        if (sSQLiteDatabase == null)
            sSQLiteDatabase = new MySQLiteHelper(applicationContext).getWritableDatabase();
        return sSQLiteDatabase;
    }

    private MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        CustomTimeRecord.onCreate(sqLiteDatabase);

        TaskRecord.onCreate(sqLiteDatabase);
        TaskHierarchyRecord.onCreate(sqLiteDatabase);

        ScheduleRecord.onCreate(sqLiteDatabase);
        SingleScheduleRecord.onCreate(sqLiteDatabase);
        DailyScheduleRecord.onCreate(sqLiteDatabase);
        WeeklyScheduleRecord.onCreate(sqLiteDatabase);
        MonthlyDayScheduleRecord.onCreate(sqLiteDatabase);
        MonthlyWeekScheduleRecord.onCreate(sqLiteDatabase);

        InstanceRecord.onCreate(sqLiteDatabase);

        InstanceShownRecord.onCreate(sqLiteDatabase);

        UuidRecord.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(oldVersion >= 17);

        sqLiteDatabase.beginTransaction();

        try
        {
            TaskRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            TaskHierarchyRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            if (oldVersion < 11) {
                SingleScheduleRecord.onCreate(sqLiteDatabase);
                DailyScheduleRecord.onCreate(sqLiteDatabase);
                WeeklyScheduleRecord.onCreate(sqLiteDatabase);

                int scheduleMaxId = ScheduleRecord.getMaxId(sqLiteDatabase);

                ArrayList<ScheduleRecord> oldScheduleRecords = new ArrayList<>();

                Cursor scheduleCursor = sqLiteDatabase.query(ScheduleRecord.TABLE_SCHEDULES, null, null, null, null, null, null);
                scheduleCursor.moveToFirst();
                while (!scheduleCursor.isAfterLast()) {
                    oldScheduleRecords.add(ScheduleRecord.cursorToScheduleRecord(scheduleCursor));
                    scheduleCursor.moveToNext();
                }
                scheduleCursor.close();

                List<ScheduleRecord> newScheduleRecords = new ArrayList<>();
                List<SingleScheduleRecord> singleScheduleRecords = new ArrayList<>();
                List<DailyScheduleRecord> dailyScheduleRecords = new ArrayList<>();
                List<WeeklyScheduleRecord> weeklyScheduleRecords = new ArrayList<>();

                for (ScheduleRecord scheduleRecord : oldScheduleRecords) {
                    ScheduleType scheduleType = ScheduleType.values()[scheduleRecord.getType()];
                    switch (scheduleType) {
                        case SINGLE:
                            Cursor singleCursor = sqLiteDatabase.query("singleScheduleDateTimes", null, "scheduleId = " + scheduleRecord.getId(), null, null, null, null);
                            singleCursor.moveToFirst();

                            int singleScheduleId = singleCursor.getInt(0);
                            int singleYear = singleCursor.getInt(1);
                            int singleMonth = singleCursor.getInt(2);
                            int singleDay = singleCursor.getInt(3);
                            Integer singleCustomTimeId = (singleCursor.isNull(4) ? null : singleCursor.getInt(4));
                            Integer singleHour = (singleCursor.isNull(5) ? null : singleCursor.getInt(5));
                            Integer singleMinute = (singleCursor.isNull(6) ? null : singleCursor.getInt(6));

                            Assert.assertTrue((singleHour == null) == (singleMinute == null));
                            Assert.assertTrue((singleHour == null) || (singleCustomTimeId == null));
                            Assert.assertTrue((singleHour != null) || (singleCustomTimeId != null));

                            singleScheduleRecords.add(new SingleScheduleRecord(false, singleScheduleId, singleYear, singleMonth, singleDay, singleCustomTimeId, singleHour, singleMinute));

                            singleCursor.close();
                            break;
                        case DAILY:
                            Cursor dailyCursor = sqLiteDatabase.query("dailyScheduleTimes", null, "scheduleId = " + scheduleRecord.getId(), null, null, null, null);
                            dailyCursor.moveToFirst();

                            int i = 0;
                            while (!dailyCursor.isAfterLast()) {
                                //noinspection UnusedAssignment
                                int dailyId = dailyCursor.getInt(0);
                                int dailyScheduleId = dailyCursor.getInt(1);
                                Integer dailyCustomTimeId = (dailyCursor.isNull(2) ? null : dailyCursor.getInt(2));
                                Integer dailyHour = (dailyCursor.isNull(3) ? null : dailyCursor.getInt(3));
                                Integer dailyMinute = (dailyCursor.isNull(4) ? null : dailyCursor.getInt(4));

                                Assert.assertTrue((dailyHour == null) == (dailyMinute == null));
                                Assert.assertTrue((dailyHour == null) || (dailyCustomTimeId == null));
                                Assert.assertTrue((dailyHour != null) || (dailyCustomTimeId != null));

                                if (i == 0) {
                                    dailyScheduleRecords.add(new DailyScheduleRecord(false, dailyScheduleId, dailyCustomTimeId, dailyHour, dailyMinute));
                                } else {
                                    newScheduleRecords.add(new ScheduleRecord(false, ++scheduleMaxId, scheduleRecord.getRootTaskId(), scheduleRecord.getStartTime(), scheduleRecord.getEndTime(), scheduleRecord.getType()));
                                    dailyScheduleRecords.add(new DailyScheduleRecord(false, scheduleMaxId, dailyCustomTimeId, dailyHour, dailyMinute));
                                }

                                dailyCursor.moveToNext();
                                i++;
                            }
                            dailyCursor.close();

                            break;
                        case WEEKLY:
                            Cursor weeklyCursor = sqLiteDatabase.query("weeklyScheduleDayOfWeekTimes", null, "scheduleId = " + scheduleRecord.getId(), null, null, null, null);
                            weeklyCursor.moveToFirst();

                            int j = 0;
                            while (!weeklyCursor.isAfterLast()) {
                                //noinspection UnusedAssignment
                                int weeklyId = weeklyCursor.getInt(0);
                                int weeklyScheduleId = weeklyCursor.getInt(1);
                                int weeklyDayOfWeek = weeklyCursor.getInt(2);
                                Integer weeklyCustomTimeId = (weeklyCursor.isNull(3) ? null : weeklyCursor.getInt(3));
                                Integer weeklyHour = (weeklyCursor.isNull(4) ? null : weeklyCursor.getInt(4));
                                Integer weeklyMinute = (weeklyCursor.isNull(5) ? null : weeklyCursor.getInt(5));

                                Assert.assertTrue((weeklyHour == null) == (weeklyMinute == null));
                                Assert.assertTrue((weeklyHour == null) || (weeklyCustomTimeId == null));
                                Assert.assertTrue((weeklyHour != null) || (weeklyCustomTimeId != null));

                                if (j == 0) {
                                    weeklyScheduleRecords.add(new WeeklyScheduleRecord(false, weeklyScheduleId, weeklyDayOfWeek, weeklyCustomTimeId, weeklyHour, weeklyMinute));
                                } else {
                                    newScheduleRecords.add(new ScheduleRecord(false, ++scheduleMaxId, scheduleRecord.getRootTaskId(), scheduleRecord.getStartTime(), scheduleRecord.getEndTime(), scheduleRecord.getType()));
                                    weeklyScheduleRecords.add(new WeeklyScheduleRecord(false, scheduleMaxId, weeklyDayOfWeek, weeklyCustomTimeId, weeklyHour, weeklyMinute));
                                }

                                weeklyCursor.moveToNext();
                                j++;
                            }
                            weeklyCursor.close();

                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

                ArrayList<InsertCommand> insertCommands = new ArrayList<>();

                for (ScheduleRecord scheduleRecord : newScheduleRecords) {
                    Assert.assertTrue(scheduleRecord.needsInsert());

                    insertCommands.add(scheduleRecord.getInsertCommand());
                }

                for (SingleScheduleRecord singleScheduleRecord : singleScheduleRecords) {
                    Assert.assertTrue(singleScheduleRecord.needsInsert());

                    insertCommands.add(singleScheduleRecord.getInsertCommand());
                }

                for (DailyScheduleRecord dailyScheduleRecord : dailyScheduleRecords) {
                    Assert.assertTrue(dailyScheduleRecord.needsInsert());

                    insertCommands.add(dailyScheduleRecord.getInsertCommand());
                }

                for (WeeklyScheduleRecord weeklyScheduleRecord : weeklyScheduleRecords) {
                    Assert.assertTrue(weeklyScheduleRecord.needsInsert());

                    insertCommands.add(weeklyScheduleRecord.getInsertCommand());
                }

                SaveService.save(sqLiteDatabase, insertCommands, new ArrayList<>(), new ArrayList<>());
            }

            ScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            SingleScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            DailyScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            WeeklyScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            MonthlyDayScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
            MonthlyWeekScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            InstanceRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            UuidRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);

            if (oldVersion < 18) {
                String columns = InstanceShownRecord.COLUMN_ID + ", "
                        + InstanceShownRecord.COLUMN_TASK_ID + ", "
                        + InstanceShownRecord.COLUMN_SCHEDULE_YEAR + ", "
                        + InstanceShownRecord.COLUMN_SCHEDULE_MONTH + ", "
                        + InstanceShownRecord.COLUMN_SCHEDULE_DAY + ", "
                        + InstanceShownRecord.COLUMN_SCHEDULE_CUSTOM_TIME_ID + ", "
                        + InstanceShownRecord.COLUMN_SCHEDULE_HOUR + ", "
                        + InstanceShownRecord.COLUMN_SCHEDULE_MINUTE + ", "
                        + InstanceShownRecord.COLUMN_NOTIFIED + ", "
                        + InstanceShownRecord.COLUMN_NOTIFICATION_SHOWN;

                sqLiteDatabase.execSQL("CREATE TEMPORARY TABLE t2_backup(" + columns + ");");
                sqLiteDatabase.execSQL("INSERT INTO t2_backup SELECT " + columns + " FROM " + InstanceShownRecord.TABLE_INSTANCES_SHOWN + ";");
                sqLiteDatabase.execSQL("DROP TABLE " + InstanceShownRecord.TABLE_INSTANCES_SHOWN + ";");
                sqLiteDatabase.execSQL("CREATE TABLE " + InstanceShownRecord.TABLE_INSTANCES_SHOWN
                        + " (" + InstanceShownRecord.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + InstanceShownRecord.COLUMN_TASK_ID + " TEXT NOT NULL, "
                        + InstanceShownRecord.COLUMN_SCHEDULE_YEAR + " INTEGER NOT NULL, "
                        + InstanceShownRecord.COLUMN_SCHEDULE_MONTH + " INTEGER NOT NULL, "
                        + InstanceShownRecord.COLUMN_SCHEDULE_DAY + " INTEGER NOT NULL, "
                        + InstanceShownRecord.COLUMN_SCHEDULE_CUSTOM_TIME_ID + " TEXT, "
                        + InstanceShownRecord.COLUMN_SCHEDULE_HOUR + " INTEGER, "
                        + InstanceShownRecord.COLUMN_SCHEDULE_MINUTE + " INTEGER, "
                        + InstanceShownRecord.COLUMN_NOTIFIED + " INTEGER NOT NULL DEFAULT 0, "
                        + InstanceShownRecord.COLUMN_NOTIFICATION_SHOWN + " INTEGER NOT NULL DEFAULT 0"
                        + ");");
                sqLiteDatabase.execSQL("INSERT INTO " + InstanceShownRecord.TABLE_INSTANCES_SHOWN + " SELECT * FROM t2_backup;");
                sqLiteDatabase.execSQL("DROP TABLE t2_backup;");
            }

            if (oldVersion < 19) {
                sqLiteDatabase.execSQL("ALTER TABLE " + InstanceShownRecord.TABLE_INSTANCES_SHOWN
                        + " ADD COLUMN " + InstanceShownRecord.COLUMN_PROJECT_ID + " TEXt");
            }

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }
}
