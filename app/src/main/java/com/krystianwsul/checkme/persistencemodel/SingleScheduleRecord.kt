package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase


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

        fun getSingleScheduleRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_SINGLE_SCHEDULES, this::cursorToSingleScheduleRecord)

        private fun cursorToSingleScheduleRecord(cursor: Cursor) = cursor.run {
            val scheduleId = getInt(0)
            val year = getInt(1)
            val month = getInt(2)
            val day = getInt(3)
            val customTimeId = if (isNull(4)) null else getInt(4)
            val hour = if (isNull(5)) null else getInt(5)
            val minute = if (isNull(6)) null else getInt(6)

            check(hour == null == (minute == null))
            check(hour == null || customTimeId == null)
            check(hour != null || customTimeId != null)

            SingleScheduleRecord(true, scheduleId, year, month, day, customTimeId, hour, minute)
        }
    }

    init {
        check(hour == null == (minute == null))
        check(hour == null || customTimeId == null)
        check(hour != null || customTimeId != null)
    }

    override val contentValues
        get() = ContentValues().apply {
        put(COLUMN_SCHEDULE_ID, scheduleId)
        put(COLUMN_YEAR, year)
        put(COLUMN_MONTH, month)
        put(COLUMN_DAY, day)
        put(COLUMN_CUSTOM_TIME_ID, customTimeId)
        put(COLUMN_HOUR, hour)
        put(COLUMN_MINUTE, minute)
    }

    override val commandTable = TABLE_SINGLE_SCHEDULES
    override val commandIdColumn = COLUMN_SCHEDULE_ID
    override val commandId = scheduleId
}
