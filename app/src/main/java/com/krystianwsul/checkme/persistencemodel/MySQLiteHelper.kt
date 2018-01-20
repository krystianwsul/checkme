package com.krystianwsul.checkme.persistencemodel

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.krystianwsul.checkme.MyApplication

class MySQLiteHelper private constructor() : SQLiteOpenHelper(MyApplication.instance, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_NAME = "tasks.db"
        private const val DATABASE_VERSION = 20

        val database by lazy { MySQLiteHelper().writableDatabase!! }
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        LocalCustomTimeRecord.onCreate(sqLiteDatabase)

        TaskRecord.onCreate(sqLiteDatabase)
        TaskHierarchyRecord.onCreate(sqLiteDatabase)

        ScheduleRecord.onCreate(sqLiteDatabase)
        SingleScheduleRecord.onCreate(sqLiteDatabase)
        DailyScheduleRecord.onCreate(sqLiteDatabase)
        WeeklyScheduleRecord.onCreate(sqLiteDatabase)
        MonthlyDayScheduleRecord.onCreate(sqLiteDatabase)
        MonthlyWeekScheduleRecord.onCreate(sqLiteDatabase)

        InstanceRecord.onCreate(sqLiteDatabase)

        InstanceShownRecord.onCreate(sqLiteDatabase)

        UuidRecord.onCreate(sqLiteDatabase)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteDatabase.beginTransaction()

        try {
            if (oldVersion < 19) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocalCustomTimeRecord.TABLE_CUSTOM_TIMES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DailyScheduleRecord.TABLE_DAILY_SCHEDULES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceRecord.TABLE_INSTANCES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InstanceShownRecord.TABLE_INSTANCES_SHOWN)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyDayScheduleRecord.TABLE_MONTHLY_DAY_SCHEDULES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MonthlyWeekScheduleRecord.TABLE_MONTHLY_WEEK_SCHEDULES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ScheduleRecord.TABLE_SCHEDULES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SingleScheduleRecord.TABLE_SINGLE_SCHEDULES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskHierarchyRecord.TABLE_TASK_HIERARCHIES)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskRecord.TABLE_TASKS)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UuidRecord.TABLE_UUID)
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeeklyScheduleRecord.TABLE_WEEKLY_SCHEDULES)

                onCreate(sqLiteDatabase)
            } else {
                if (oldVersion < 20) {
                    sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + InstanceShownRecord.INDEX_HOUR_MINUTE + " ON " + InstanceShownRecord.TABLE_INSTANCES_SHOWN
                            + "("
                            + InstanceShownRecord.COLUMN_PROJECT_ID + ", "
                            + InstanceShownRecord.COLUMN_TASK_ID + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_YEAR + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_MONTH + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_DAY + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_HOUR + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_MINUTE
                            + ")")

                    sqLiteDatabase.execSQL("CREATE UNIQUE INDEX " + InstanceShownRecord.INDEX_CUSTOM_TIME_ID + " ON " + InstanceShownRecord.TABLE_INSTANCES_SHOWN
                            + "("
                            + InstanceShownRecord.COLUMN_PROJECT_ID + ", "
                            + InstanceShownRecord.COLUMN_TASK_ID + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_YEAR + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_MONTH + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_DAY + ", "
                            + InstanceShownRecord.COLUMN_SCHEDULE_CUSTOM_TIME_ID
                            + ")")
                }
            }

            sqLiteDatabase.setTransactionSuccessful()
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }
}
