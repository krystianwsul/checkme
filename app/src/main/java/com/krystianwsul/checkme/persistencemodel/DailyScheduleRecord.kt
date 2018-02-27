package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

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

        fun getDailyScheduleRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_DAILY_SCHEDULES, this::cursorToDailyScheduleTimeRecord)

        private fun cursorToDailyScheduleTimeRecord(cursor: Cursor) = cursor.run {
            val scheduleId = getInt(0)
            val customTimeId = if (isNull(1)) null else getInt(1)
            val hour = if (isNull(2)) null else getInt(2)
            val minute = if (isNull(3)) null else getInt(3)

            check(hour == null == (minute == null))
            check(hour == null || customTimeId == null)
            check(hour != null || customTimeId != null)

            DailyScheduleRecord(true, scheduleId, customTimeId, hour, minute)
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
        put(COLUMN_CUSTOM_TIME_ID, customTimeId)
        put(COLUMN_HOUR, hour)
        put(COLUMN_MINUTE, minute)
    }

    override val commandTable = TABLE_DAILY_SCHEDULES
    override val commandIdColumn = COLUMN_SCHEDULE_ID
    override val commandId = scheduleId
}
