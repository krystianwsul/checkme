package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert
import java.util.*

class SingleScheduleRecord(created: Boolean, val scheduleId: Int, val year: Int, val month: Int, val day: Int, val customTimeId: Int?, val hour: Int?, val minute: Int?) : Record(created) {

    companion object {

        val TABLE_SINGLE_SCHEDULES = "singleSchedules"

        private const val COLUMN_SCHEDULE_ID = "scheduleId"
        private const val COLUMN_YEAR = "year"
        private const val COLUMN_MONTH = "month"
        private const val COLUMN_DAY = "day"
        private const val COLUMN_CUSTOM_TIME_ID = "customTimeId"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_MINUTE = "minute"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_SINGLE_SCHEDULES " +
                    "($COLUMN_SCHEDULE_ID INTEGER NOT NULL UNIQUE REFERENCES ${ScheduleRecord.TABLE_SCHEDULES} (${ScheduleRecord.COLUMN_ID}), " +
                    "$COLUMN_YEAR INTEGER NOT NULL, " +
                    "$COLUMN_MONTH INTEGER NOT NULL, " +
                    "$COLUMN_DAY INTEGER NOT NULL, " +
                    "$COLUMN_CUSTOM_TIME_ID INTEGER REFERENCES ${LocalCustomTimeRecord.TABLE_CUSTOM_TIMES} (${LocalCustomTimeRecord.COLUMN_ID}), " +
                    "$COLUMN_HOUR INTEGER, " +
                    "$COLUMN_MINUTE INTEGER);")
        }

        fun getSingleScheduleRecords(sqLiteDatabase: SQLiteDatabase): List<SingleScheduleRecord> { // todo single function
            val singleScheduleDateTimeRecords = ArrayList<SingleScheduleRecord>()

            val cursor = sqLiteDatabase.query(TABLE_SINGLE_SCHEDULES, null, null, null, null, null, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                singleScheduleDateTimeRecords.add(cursorToSingleScheduleRecord(cursor))
                cursor.moveToNext()
            }
            cursor.close()

            return singleScheduleDateTimeRecords
        }

        private fun cursorToSingleScheduleRecord(cursor: Cursor) = cursor.run {
            val scheduleId = getInt(0)
            val year = getInt(1)
            val month = getInt(2)
            val day = getInt(3)
            val customTimeId = if (isNull(4)) null else getInt(4)
            val hour = if (isNull(5)) null else getInt(5)
            val minute = if (isNull(6)) null else getInt(6)

            Assert.assertTrue(hour == null == (minute == null))
            Assert.assertTrue(hour == null || customTimeId == null)
            Assert.assertTrue(hour != null || customTimeId != null)

            SingleScheduleRecord(true, scheduleId, year, month, day, customTimeId, hour, minute)
        }
    }

    init {
        Assert.assertTrue(hour == null == (minute == null))
        Assert.assertTrue(hour == null || customTimeId == null)
        Assert.assertTrue(hour != null || customTimeId != null)
    }

    override val contentValues = ContentValues().apply {
        put(COLUMN_SCHEDULE_ID, scheduleId)
        put(COLUMN_YEAR, year)
        put(COLUMN_MONTH, month)
        put(COLUMN_DAY, day)
        put(COLUMN_CUSTOM_TIME_ID, customTimeId)
        put(COLUMN_HOUR, hour)
        put(COLUMN_MINUTE, minute)
    }

    override val updateCommand = getUpdateCommand(TABLE_SINGLE_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)

    override val insertCommand = getInsertCommand(TABLE_SINGLE_SCHEDULES)

    override val deleteCommand = getDeleteCommand(TABLE_SINGLE_SCHEDULES, COLUMN_SCHEDULE_ID, scheduleId)
}
