package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import junit.framework.Assert

import kotlin.properties.Delegates.observable

class TaskHierarchyRecord(created: Boolean, val id: Int, val parentTaskId: Int, val childTaskId: Int, val startTime: Long, mEndTime: Long?) : Record(created) {

    companion object {

        val TABLE_TASK_HIERARCHIES = "taskHierarchies"

        private val COLUMN_ID = "_id"
        private val COLUMN_PARENT_TASK_ID = "parentTaskId"
        private val COLUMN_CHILD_TASK_ID = "childTaskId"
        private val COLUMN_START_TIME = "startTime"
        private val COLUMN_END_TIME = "endTime"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_TASK_HIERARCHIES " +
                    "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_PARENT_TASK_ID INTEGER NOT NULL REFERENCES ${TaskRecord.TABLE_TASKS} (${TaskRecord.COLUMN_ID}), " +
                    "$COLUMN_CHILD_TASK_ID INTEGER NOT NULL REFERENCES ${TaskRecord.TABLE_TASKS} (${TaskRecord.COLUMN_ID}), " +
                    "$COLUMN_START_TIME INTEGER NOT NULL, " +
                    "$COLUMN_END_TIME INTEGER);")
        }

        fun getTaskHierarchyRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_TASK_HIERARCHIES, this::cursorToTaskHierarchyRecord)

        private fun cursorToTaskHierarchyRecord(cursor: Cursor) = cursor.run {
            val id = getInt(0)
            val parentTaskId = getInt(1)
            val childTaskId = getInt(2)
            val startTime = getLong(3)
            val endTime = if (isNull(4)) null else getLong(4)

            Assert.assertTrue(parentTaskId != childTaskId)
            Assert.assertTrue(endTime == null || startTime <= endTime)

            TaskHierarchyRecord(true, id, parentTaskId, childTaskId, startTime, endTime)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = Record.getMaxId(sqLiteDatabase, TABLE_TASK_HIERARCHIES, COLUMN_ID)
    }

    var endTime by observable(mEndTime) { _, _, _ -> changed = true }

    init {
        Assert.assertTrue(parentTaskId != childTaskId)
        Assert.assertTrue(mEndTime == null || startTime <= mEndTime)
    }

    override val contentValues
        get() = ContentValues().apply {
        put(COLUMN_PARENT_TASK_ID, parentTaskId)
        put(COLUMN_CHILD_TASK_ID, childTaskId)
        put(COLUMN_START_TIME, startTime)
        put(COLUMN_END_TIME, endTime)
    }

    override val commandTable = TABLE_TASK_HIERARCHIES
    override val commandIdColumn = COLUMN_ID
    override val commandId = id
}
