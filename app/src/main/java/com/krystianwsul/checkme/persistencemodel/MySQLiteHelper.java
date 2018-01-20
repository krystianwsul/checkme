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

        TaskRecord.Companion.onCreate(sqLiteDatabase);
        TaskHierarchyRecord.Companion.onCreate(sqLiteDatabase);

        ScheduleRecord.Companion.onCreate(sqLiteDatabase);
        SingleScheduleRecord.Companion.onCreate(sqLiteDatabase);
        DailyScheduleRecord.Companion.onCreate(sqLiteDatabase);
        WeeklyScheduleRecord.Companion.onCreate(sqLiteDatabase);
        MonthlyDayScheduleRecord.Companion.onCreate(sqLiteDatabase);
        MonthlyWeekScheduleRecord.Companion.onCreate(sqLiteDatabase);

        InstanceRecord.Companion.onCreate(sqLiteDatabase);

        InstanceShownRecord.Companion.onCreate(sqLiteDatabase);

        UuidRecord.Companion.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.beginTransaction();

        try {
            if (oldVersion < 19) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocalCustomTimeRecord.Companion.getTABLE_CUSTOM_TIMES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DailyScheduleRecord.Companion.getTABLE_DAILY_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceRecord.Companion.getTABLE_INSTANCES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceShownRecord.Companion.getTABLE_INSTANCES_SHOWN());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyDayScheduleRecord.Companion.getTABLE_MONTHLY_DAY_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyWeekScheduleRecord.Companion.getTABLE_MONTHLY_WEEK_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ScheduleRecord.Companion.getTABLE_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SingleScheduleRecord.Companion.getTABLE_SINGLE_SCHEDULES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskHierarchyRecord.Companion.getTABLE_TASK_HIERARCHIES());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskRecord.Companion.getTABLE_TASKS());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UuidRecord.Companion.getTABLE_UUID());
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeeklyScheduleRecord.Companion.getTABLE_WEEKLY_SCHEDULES());

                onCreate(sqLiteDatabase);
            } else {
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
