package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert

class MonthlyDayScheduleRecord(created: Boolean, val scheduleId: Int, val dayOfMonth: Int, val beginningOfMonth: Boolean, val customTimeId: Int?, val hour: Int?, val minute: Int?) : Record(created) {

    companion object {

        val TABLE_MONTHLY_DAY_SCHEDULES = "monthlyDaySchedules"

        private const val COLUMN_SCHEDULE_ID = "scheduleId"
        private const val COLUMN_DAY_OF_MONTH = "dayOfMonth"
        private const val COLUMN_BEGINNING_OF_MONTH = "beginningOfMonth"
        private const val COLUMN_CUSTOM_TIME_ID = "customTimeId"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_MINUTE = "minute"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_MONTHLY_DAY_SCHEDULES " +
                    "($COLUMN_SCHEDULE_ID INTEGER NOT NULL UNIQUE REFERENCES ${ScheduleRecord.TABLE_SCHEDULES} (${ScheduleRecord.COLUMN_ID}), " +
                    "$COLUMN_DAY_OF_MONTH INTEGER NOT NULL, " +
                    "$COLUMN_BEGINNING_OF_MONTH INTEGER NOT NULL, " +
                    "$COLUMN_CUSTOM_TIME_ID INTEGER REFERENCES ${LocalCustomTimeRecord.TABLE_CUSTOM_TIMES} (${LocalCustomTimeRecord.COLUMN_ID}), " +
                    "$COLUMN_HOUR INTEGER, " +
                    "$COLUMN_MINUTE INTEGER);")
        }

        fun getMonthlyDayScheduleRecords(sqLiteDatabase: SQLiteDatabase) = mutableListOf<MonthlyDayScheduleRecord>().apply {
            sqLiteDatabase.query(TABLE_MONTHLY_DAY_SCHEDULES, null, null, null, null, null, null).use {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    add(cursorToWeeklyScheduleRecord(it))
                    it.moveToNext()
                }
            }
        }

        private fun cursorToWeeklyScheduleRecord(cursor: Cursor) = cursor.run {
            val scheduleId = getInt(0)
            val dayOfMonth = getInt(1)
            val beginningOfMonth = getInt(2) == 1
            val customTimeId = if (isNull(3)) null else getInt(3)
            val hour = if (isNull(4)) null else getInt(4)
            val minute = if (isNull(5)) null else getInt(5)

            Assert.assertTrue(hour == null == (minute == null))
            Assert.assertTrue(hour == null || customTimeId == null)
            Assert.assertTrue(hour != null || customTimeId != null)

            MonthlyDayScheduleRecord(true, scheduleId, dayOfMonth, beginningOfMonth, customTimeId, hour, minute)
        }
    }

    init {
        Assert.assertTrue(hour == null == (minute == null))
        Assert.assertTrue(hour == null || customTimeId == null)
        Assert.assertTrue(hour != null || customTimeId != null)
    }

    override fun getContentValues() = ContentValues().apply {
        put(COLUMN_SCHEDULE_ID, scheduleId)
        put(COLUMN_DAY_OF_MONTH, dayOfMonth)
        put(COLUMN_BEGINNING_OF_MONTH, if (beginningOfMonth) 1 else 0)
        put(COLUMN_CUSTOM_TIME_ID, customTimeId)
        put(COLUMN_HOUR, hour)
        put(COLUMN_MINUTE, minute)
    }

    override fun getUpdateCommand() = getUpdateCommand(TABLE_MONTHLY_DAY_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)

    override fun getInsertCommand() = getInsertCommand(TABLE_MONTHLY_DAY_SCHEDULES)

    override fun getDeleteCommand() = getDeleteCommand(TABLE_MONTHLY_DAY_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)
}
