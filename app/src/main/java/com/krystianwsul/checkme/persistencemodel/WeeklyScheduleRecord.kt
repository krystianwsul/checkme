package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert

class WeeklyScheduleRecord(created: Boolean, val scheduleId: Int, val dayOfWeek: Int, val customTimeId: Int?, val hour: Int?, val minute: Int?) : Record(created) {

    companion object {

        val TABLE_WEEKLY_SCHEDULES = "weeklySchedules"

        private const val COLUMN_SCHEDULE_ID = "scheduleId"
        private const val COLUMN_DAY_OF_WEEK = "dayOfWeek"
        private const val COLUMN_CUSTOM_TIME_ID = "customTimeId"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_MINUTE = "minute"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_WEEKLY_SCHEDULES " +
                    "($COLUMN_SCHEDULE_ID INTEGER NOT NULL UNIQUE REFERENCES ${ScheduleRecord.TABLE_SCHEDULES} (${ScheduleRecord.COLUMN_ID}), " +
                    "$COLUMN_DAY_OF_WEEK INTEGER NOT NULL, " +
                    "$COLUMN_CUSTOM_TIME_ID INTEGER REFERENCES ${LocalCustomTimeRecord.TABLE_CUSTOM_TIMES} (${LocalCustomTimeRecord.COLUMN_ID}), " +
                    "$COLUMN_HOUR INTEGER, " +
                    "$COLUMN_MINUTE INTEGER);")
        }

        fun getWeeklyScheduleRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_WEEKLY_SCHEDULES, this::cursorToWeeklyScheduleRecord)

        private fun cursorToWeeklyScheduleRecord(cursor: Cursor) = cursor.run {
            val scheduleId = getInt(0)
            val dayOfWeek = getInt(1)
            val customTimeId = if (isNull(2)) null else getInt(2)
            val hour = if (isNull(3)) null else getInt(3)
            val minute = if (isNull(4)) null else getInt(4)

            Assert.assertTrue(hour == null == (minute == null))
            Assert.assertTrue(hour == null || customTimeId == null)
            Assert.assertTrue(hour != null || customTimeId != null)

            WeeklyScheduleRecord(true, scheduleId, dayOfWeek, customTimeId, hour, minute)
        }
    }

    init {
        Assert.assertTrue(hour == null == (minute == null))
        Assert.assertTrue(hour == null || customTimeId == null)
        Assert.assertTrue(hour != null || customTimeId != null)
    }

    override val contentValues = ContentValues().apply {
        put(COLUMN_SCHEDULE_ID, scheduleId)
        put(COLUMN_DAY_OF_WEEK, dayOfWeek)
        put(COLUMN_CUSTOM_TIME_ID, customTimeId)
        put(COLUMN_HOUR, hour)
        put(COLUMN_MINUTE, minute)
    }

    override val updateCommand get() = getUpdateCommand(TABLE_WEEKLY_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)

    override val insertCommand get() = getInsertCommand(TABLE_WEEKLY_SCHEDULES)

    override val deleteCommand get() = getDeleteCommand(TABLE_WEEKLY_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)
}
