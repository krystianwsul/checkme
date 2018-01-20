package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert
import kotlin.properties.Delegates

class ScheduleRecord(created: Boolean, val id: Int, val rootTaskId: Int, val startTime: Long, _endTime: Long?, val type: Int) : Record(created) {

    companion object {

        val TABLE_SCHEDULES = "schedules"

        val COLUMN_ID = "_id"
        private const val COLUMN_ROOT_TASK_ID = "rootTaskId"
        private const val COLUMN_START_TIME = "startTime"
        private const val COLUMN_END_TIME = "endTime"
        private const val COLUMN_TYPE = "type"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_SCHEDULES " +
                    "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_ROOT_TASK_ID INTEGER NOT NULL REFERENCES ${TaskRecord.TABLE_TASKS} (${TaskRecord.COLUMN_ID}), " +
                    "$COLUMN_START_TIME INTEGER NOT NULL, " +
                    "$COLUMN_END_TIME INTEGER, " +
                    "$COLUMN_TYPE INTEGER NOT NULL);")
        }

        fun getScheduleRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_SCHEDULES, this::cursorToScheduleRecord)

        private fun cursorToScheduleRecord(cursor: Cursor) = cursor.run {
            val id = getInt(0)
            val taskId = getInt(1)
            val startTime = getLong(2)
            val endTime = if (isNull(3)) null else getLong(3)
            val type = getInt(4)

            Assert.assertTrue(endTime == null || startTime <= endTime)

            ScheduleRecord(true, id, taskId, startTime, endTime, type)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = Record.getMaxId(sqLiteDatabase, TABLE_SCHEDULES, COLUMN_ID)
    }

    var endTime by Delegates.observable(_endTime) { _, oldValue, newValue ->
        Assert.assertTrue(oldValue == null)
        Assert.assertTrue(startTime <= newValue!!)

        changed = true
    }

    init {
        Assert.assertTrue(_endTime == null || startTime <= _endTime)
    }

    override val contentValues = ContentValues().apply {
        put(COLUMN_ROOT_TASK_ID, rootTaskId)
        put(COLUMN_START_TIME, startTime)
        put(COLUMN_END_TIME, endTime)
        put(COLUMN_TYPE, type)
    }

    override val updateCommand get() = getUpdateCommand(TABLE_SCHEDULES, COLUMN_ID, id)

    override val insertCommand get() = getInsertCommand(TABLE_SCHEDULES)

    override val deleteCommand get() = getDeleteCommand(TABLE_SCHEDULES, COLUMN_ID, id)
}
