package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import kotlin.properties.Delegates.observable

class TaskRecord(created: Boolean, val id: Int, _name: String, val startTime: Long, _endTime: Long?, _oldestVisibleYear: Int?, _oldestVisibleMonth: Int?, _oldestVisibleDay: Int?, _note: String?) : Record(created) {

    companion object {

        val TABLE_TASKS = "tasks"

        val COLUMN_ID = "_id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_START_TIME = "startTime"
        private const val COLUMN_END_TIME = "endTime"
        private const val COLUMN_OLDEST_VISIBLE_YEAR = "oldestVisibleYear"
        private const val COLUMN_OLDEST_VISIBLE_MONTH = "oldestVisibleMonth"
        private const val COLUMN_OLDEST_VISIBLE_DAY = "oldestVisibleDay"
        private const val COLUMN_NOTE = "note"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_TASKS " +
                    "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_NAME TEXT NOT NULL, " +
                    "$COLUMN_START_TIME TEXT NOT NULL, " +
                    "$COLUMN_END_TIME TEXT, " +
                    "$COLUMN_OLDEST_VISIBLE_YEAR INTEGER, " +
                    "$COLUMN_OLDEST_VISIBLE_MONTH INTEGER, " +
                    "$COLUMN_OLDEST_VISIBLE_DAY INTEGER, " +
                    "$COLUMN_NOTE TEXT);")
        }

        fun getTaskRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_TASKS, this::cursorToTaskRecord)

        private fun cursorToTaskRecord(cursor: Cursor) = cursor.run {
            val id = getInt(0)
            val name = getString(1)
            val startTime = getLong(2)
            val endTime = if (isNull(3)) null else getLong(3)
            val oldestVisibleYear = if (isNull(4)) null else getInt(4)
            val oldestVisibleMonth = if (isNull(5)) null else getInt(5)
            val oldestVisibleDay = if (isNull(6)) null else getInt(6)
            val note = getString(7)

            check(name.isNotEmpty())
            check(endTime == null || startTime <= endTime)
            check(oldestVisibleYear == null == (oldestVisibleMonth == null))
            check(oldestVisibleYear == null == (oldestVisibleDay == null))

            TaskRecord(true, id, name, startTime, endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, note)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = Record.getMaxId(sqLiteDatabase, TABLE_TASKS, COLUMN_ID)
    }

    var name by observable(_name) { _, _, newValue ->
        check(newValue.isNotEmpty())

        changed = true
    }

    var endTime by observable(_endTime) { _, _, newValue ->
        newValue?.let { check(startTime <= it) }

        changed = true
    }

    var oldestVisibleYear by observable(_oldestVisibleYear) { _, _, _ -> changed = true }

    var oldestVisibleMonth by observable(_oldestVisibleMonth) { _, _, _ -> changed = true }

    var oldestVisibleDay by observable(_oldestVisibleDay) { _, _, _ -> changed = true }

    var note by observable(_note) { _, _, _ -> changed = true }

    init {
        check(_name.isNotEmpty())
        check(_endTime == null || startTime <= _endTime)
        check(_oldestVisibleYear == null == (_oldestVisibleMonth == null))
        check(_oldestVisibleYear == null == (_oldestVisibleDay == null))
    }

    override val contentValues
        get() = ContentValues().apply {
        put(COLUMN_NAME, name)
        put(COLUMN_START_TIME, startTime)
        put(COLUMN_END_TIME, endTime)
        put(COLUMN_OLDEST_VISIBLE_YEAR, oldestVisibleYear)
        put(COLUMN_OLDEST_VISIBLE_MONTH, oldestVisibleMonth)
        put(COLUMN_OLDEST_VISIBLE_DAY, oldestVisibleDay)
        put(COLUMN_NOTE, note)
    }

    override val commandTable = TABLE_TASKS
    override val commandIdColumn = COLUMN_ID
    override val commandId = id
}
