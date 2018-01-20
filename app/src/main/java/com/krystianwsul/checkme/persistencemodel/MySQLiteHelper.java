package com.krystianwsul.checkme.persistencemodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class MySQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 20;

    @Nullable
    private static SQLiteDatabase sSQLiteDatabase;

    @NonNull
    static SQLiteDatabase getDatabase(@NonNull Context applicationContext) {
        if (sSQLiteDatabase == null)
            sSQLiteDatabase = new MySQLiteHelper(applicationContext).getWritableDatabase();
        return sSQLiteDatabase;
    }

    private MySQLiteHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
        LocalCustomTimeRecord.Companion.onCreate(sqLiteDatabase);

        TaskRecord.onCreate(sqLiteDatabase);
        TaskHierarchyRecord.Companion.onCreate(sqLiteDatabase);

        ScheduleRecord.onCreate(sqLiteDatabase);
        SingleScheduleRecord.onCreate(sqLiteDatabase);
        DailyScheduleRecord.Companion.onCreate(sqLiteDatabase);
        WeeklyScheduleRecord.onCreate(sqLiteDatabase);
        MonthlyDayScheduleRecord.Companion.onCreate(sqLiteDatabase);
        MonthlyWeekScheduleRecord.onCreate(sqLiteDatabase);

        InstanceRecord.Companion.onCreate(sqLiteDatabase);

        InstanceShownRecord.Companion.onCreate(sqLiteDatabase);

        UuidRecord.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.beginTransaction();

        try
        {
            if (oldVersion < 17) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocalCustomTimeRecord.Companion.getTABLE_CUSTOM_TIMES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DailyScheduleRecord.Companion.getTABLE_DAILY_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceRecord.Companion.getTABLE_INSTANCES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyDayScheduleRecord.Companion.getTABLE_MONTHLY_DAY_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyWeekScheduleRecord.TABLE_MONTHLY_WEEK_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ScheduleRecord.TABLE_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SingleScheduleRecord.TABLE_SINGLE_SCHEDULES);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskHierarchyRecord.Companion.getTABLE_TASK_HIERARCHIES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskRecord.TABLE_TASKS);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UuidRecord.TABLE_UUID);
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeeklyScheduleRecord.TABLE_WEEKLY_SCHEDULES);

                onCreate(sqLiteDatabase);
            } else {
                if (oldVersion < 18) {
                    String columns = InstanceShownRecord.Companion.getCOLUMN_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_TASK_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_YEAR() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MONTH() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_DAY() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_CUSTOM_TIME_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_HOUR() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MINUTE() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_NOTIFIED() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_NOTIFICATION_SHOWN();

                    sqLiteDatabase.execSQL("CREATE TEMPORARY TABLE t2_backup(" + columns + ");");
                    sqLiteDatabase.execSQL("INSERT INTO t2_backup SELECT " + columns + " FROM " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN() + ";");
                    sqLiteDatabase.execSQL("DROP TABLE " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN() + ";");
                    sqLiteDatabase.execSQL("CREATE TABLE " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN()
                            + " (" + InstanceShownRecord.Companion.getCOLUMN_ID() + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + InstanceShownRecord.Companion.getCOLUMN_TASK_ID() + " TEXT NOT NULL, "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_YEAR() + " INTEGER NOT NULL, "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MONTH() + " INTEGER NOT NULL, "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_DAY() + " INTEGER NOT NULL, "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_CUSTOM_TIME_ID() + " TEXT, "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_HOUR() + " INTEGER, "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MINUTE() + " INTEGER, "
                            + InstanceShownRecord.Companion.getCOLUMN_NOTIFIED() + " INTEGER NOT NULL DEFAULT 0, "
                            + InstanceShownRecord.Companion.getCOLUMN_NOTIFICATION_SHOWN() + " INTEGER NOT NULL DEFAULT 0"
                            + ");");
                    sqLiteDatabase.execSQL("INSERT INTO " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN() + " SELECT * FROM t2_backup;");
                    sqLiteDatabase.execSQL("DROP TABLE t2_backup;");
                }

                if (oldVersion < 19) {
                    sqLiteDatabase.execSQL("ALTER TABLE " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN()
                            + " ADD COLUMN " + InstanceShownRecord.Companion.getCOLUMN_PROJECT_ID() + " TEXT");
                }

                if (oldVersion < 20) {
                    sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + InstanceShownRecord.Companion.getINDEX_HOUR_MINUTE() + " ON " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN()
                            + "("
                            + InstanceShownRecord.Companion.getCOLUMN_PROJECT_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_TASK_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_YEAR() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MONTH() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_DAY() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_HOUR() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MINUTE()
                            + ")");

                    sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + InstanceShownRecord.Companion.getINDEX_CUSTOM_TIME_ID() + " ON " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN()
                            + "("
                            + InstanceShownRecord.Companion.getCOLUMN_PROJECT_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_TASK_ID() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_YEAR() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_MONTH() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_DAY() + ", "
                            + InstanceShownRecord.Companion.getCOLUMN_SCHEDULE_CUSTOM_TIME_ID()
                            + ")");
                }
            }

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }
}
