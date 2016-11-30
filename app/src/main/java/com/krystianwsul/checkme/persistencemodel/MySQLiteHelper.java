package com.krystianwsul.checkme.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import junit.framework.Assert;

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

        sqLiteDatabase.beginTransaction();

        try
        {
            if (oldVersion < 17) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CustomTimeRecord.TABLE_CUSTOM_TIMES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DailyScheduleRecord.TABLE_DAILY_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceRecord.TABLE_INSTANCES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceShownRecord.TABLE_INSTANCES_SHOWN);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyDayScheduleRecord.TABLE_MONTHLY_DAY_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyWeekScheduleRecord.TABLE_MONTHLY_WEEK_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ScheduleRecord.TABLE_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SingleScheduleRecord.TABLE_SINGLE_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskHierarchyRecord.TABLE_TASK_HIERARCHIES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskRecord.TABLE_TASKS);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UuidRecord.TABLE_UUID);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeeklyScheduleRecord.TABLE_WEEKLY_SCHEDULES);
            } else {
                TaskRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
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
            }

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }
}
