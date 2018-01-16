package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert

class DailyScheduleRecord(created: Boolean, val scheduleId: Int, val customTimeId: Int?, val hour: Int?, val minute: Int?) : Record(created) {

    companion object {

        val TABLE_DAILY_SCHEDULES = "dailySchedules"

        private val COLUMN_SCHEDULE_ID = "scheduleId"
        private val COLUMN_CUSTOM_TIME_ID = "customTimeId"
        private val COLUMN_HOUR = "hour"
        private val COLUMN_MINUTE = "minute"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_DAILY_SCHEDULES " +
                    "($COLUMN_SCHEDULE_ID INTEGER NOT NULL UNIQUE REFERENCES ${ScheduleRecord.TABLE_SCHEDULES} (${ScheduleRecord.COLUMN_ID}), " +
                    "$COLUMN_CUSTOM_TIME_ID INTEGER REFERENCES ${LocalCustomTimeRecord.TABLE_CUSTOM_TIMES} (${LocalCustomTimeRecord.COLUMN_ID}), " +
                    "$COLUMN_HOUR INTEGER, " +
                    "$COLUMN_MINUTE INTEGER);")
        }

        fun getDailyScheduleRecords(sqLiteDatabase: SQLiteDatabase) = mutableListOf<DailyScheduleRecord>().apply {
            sqLiteDatabase.query(TABLE_DAILY_SCHEDULES, null, null, null, null, null, null).use {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    add(cursorToDailyScheduleTimeRecord(it))
                    it.moveToNext()
                }
            }
        }

        private fun cursorToDailyScheduleTimeRecord(cursor: Cursor) = cursor.run {
            val scheduleId = getInt(0)
            val customTimeId = if (isNull(1)) null else getInt(1)
            val hour = if (isNull(2)) null else getInt(2)
            val minute = if (isNull(3)) null else getInt(3)

            Assert.assertTrue(hour == null == (minute == null))
            Assert.assertTrue(hour == null || customTimeId == null)
            Assert.assertTrue(hour != null || customTimeId != null)

            DailyScheduleRecord(true, scheduleId, customTimeId, hour, minute)
        }
    }

    init {
        Assert.assertTrue(hour == null == (minute == null))
        Assert.assertTrue(hour == null || customTimeId == null)
        Assert.assertTrue(hour != null || customTimeId != null)
    }

    override fun getContentValues() = ContentValues().apply {
        put(COLUMN_SCHEDULE_ID, scheduleId)
        put(COLUMN_CUSTOM_TIME_ID, customTimeId)
        put(COLUMN_HOUR, hour)
        put(COLUMN_MINUTE, minute)
    }

    override fun getUpdateCommand() = getUpdateCommand(TABLE_DAILY_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)

    override fun getInsertCommand() = getInsertCommand(TABLE_DAILY_SCHEDULES)

    override fun getDeleteCommand() = getDeleteCommand(TABLE_DAILY_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)
}
