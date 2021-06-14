package com.krystianwsul.checkme.persistencemodel

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.krystianwsul.checkme.MyApplication

class MySQLiteHelper private constructor() : SQLiteOpenHelper(MyApplication.instance, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_NAME = "tasks.db"
        private const val DATABASE_VERSION = 23

        val database by lazy { MySQLiteHelper().writableDatabase!! }
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        InstanceShownRecord.onCreate(sqLiteDatabase)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteDatabase.beginTransaction()

        try {
            if (oldVersion < 23) {
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS customTimes")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS dailySchedules")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS instances")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS monthlyDaySchedules")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS monthlyWeekSchedules")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS schedules")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS singleSchedules")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS taskHierarchies")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS tasks")
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS weeklySchedules")
            }

            sqLiteDatabase.setTransactionSuccessful()
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }
}
