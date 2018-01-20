package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert
import java.util.*
import kotlin.properties.Delegates.observable

class InstanceRecord(
        created: Boolean,
        val id: Int,
        val taskId: Int,
        mDone: Long?,
        val scheduleYear: Int,
        val scheduleMonth: Int,
        val scheduleDay: Int,
        val scheduleCustomTimeId: Int?,
        val scheduleHour: Int?,
        val scheduleMinute: Int?,
        mInstanceYear: Int?,
        mInstanceMonth: Int?,
        mInstanceDay: Int?,
        mInstanceCustomTimeId: Int?,
        mInstanceHour: Int?,
        mInstanceMinute: Int?,
        val hierarchyTime: Long,
        mNotified: Boolean,
        mNotificationShown: Boolean) : Record(created) {

    companion object {

        val TABLE_INSTANCES = "instances"

        private val COLUMN_ID = "_id"
        private val COLUMN_TASK_ID = "taskId"
        private val COLUMN_DONE = "done"
        private val COLUMN_SCHEDULE_YEAR = "scheduleYear"
        private val COLUMN_SCHEDULE_MONTH = "scheduleMonth"
        private val COLUMN_SCHEDULE_DAY = "scheduleDay"
        private val COLUMN_SCHEDULE_CUSTOM_TIME_ID = "scheduleCustomTimeId"
        private val COLUMN_SCHEDULE_HOUR = "scheduleHour"
        private val COLUMN_SCHEDULE_MINUTE = "scheduleMinute"
        private val COLUMN_INSTANCE_YEAR = "instanceYear"
        private val COLUMN_INSTANCE_MONTH = "instanceMonth"
        private val COLUMN_INSTANCE_DAY = "instanceDay"
        private val COLUMN_INSTANCE_CUSTOM_TIME_ID = "instanceCustomTimeId"
        private val COLUMN_INSTANCE_HOUR = "instanceHour"
        private val COLUMN_INSTANCE_MINUTE = "instanceMinute"
        private val COLUMN_HIERARCHY_TIME = "hierarchyTime"
        private val COLUMN_NOTIFIED = "notified"
        private val COLUMN_NOTIFICATION_SHOWN = "notificationShown"

        private val INDEX_TASK_SCHEDULE_HOUR_MINUTE = "instanceIndexTaskScheduleHourMinute"
        private val INDEX_TASK_SCHEDULE_CUSTOM_TIME_ID = "instanceIndexTaskScheduleCustomTimeId"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_INSTANCES " +
                    "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_TASK_ID INTEGER NOT NULL REFERENCES ${TaskRecord.TABLE_TASKS} (${TaskRecord.COLUMN_ID}), " +
                    "$COLUMN_DONE INTEGER, " +
                    "$COLUMN_SCHEDULE_YEAR INTEGER NOT NULL, " +
                    "$COLUMN_SCHEDULE_MONTH INTEGER NOT NULL, " +
                    "$COLUMN_SCHEDULE_DAY INTEGER NOT NULL, " +
                    "$COLUMN_SCHEDULE_CUSTOM_TIME_ID INTEGER REFERENCES ${LocalCustomTimeRecord.TABLE_CUSTOM_TIMES} (${LocalCustomTimeRecord.COLUMN_ID}), " +
                    "$COLUMN_SCHEDULE_HOUR INTEGER, " +
                    "$COLUMN_SCHEDULE_MINUTE INTEGER, " +
                    "$COLUMN_INSTANCE_YEAR INTEGER, " +
                    "$COLUMN_INSTANCE_MONTH INTEGER, " +
                    "$COLUMN_INSTANCE_DAY INTEGER, " +
                    "$COLUMN_INSTANCE_CUSTOM_TIME_ID INTEGER REFERENCES ${LocalCustomTimeRecord.TABLE_CUSTOM_TIMES} (${LocalCustomTimeRecord.COLUMN_ID}), " +
                    "$COLUMN_INSTANCE_HOUR INTEGER, " +
                    "$COLUMN_INSTANCE_MINUTE INTEGER, " +
                    "$COLUMN_HIERARCHY_TIME INTEGER NOT NULL, " +
                    "$COLUMN_NOTIFIED INTEGER NOT NULL DEFAULT 0, " +
                    "$COLUMN_NOTIFICATION_SHOWN INTEGER NOT NULL DEFAULT 0);")

            sqLiteDatabase.execSQL("CREATE UNIQUE INDEX $INDEX_TASK_SCHEDULE_HOUR_MINUTE ON $TABLE_INSTANCES " +
                    "(" +
                    "$COLUMN_TASK_ID, " +
                    "$COLUMN_SCHEDULE_YEAR, " +
                    "$COLUMN_SCHEDULE_MONTH, " +
                    "$COLUMN_SCHEDULE_DAY, " +
                    "$COLUMN_SCHEDULE_HOUR, " +
                    "$COLUMN_SCHEDULE_MINUTE)")

            sqLiteDatabase.execSQL("CREATE UNIQUE INDEX $INDEX_TASK_SCHEDULE_CUSTOM_TIME_ID ON $TABLE_INSTANCES " +
                    "(" +
                    "$COLUMN_TASK_ID, " +
                    "$COLUMN_SCHEDULE_YEAR, " +
                    "$COLUMN_SCHEDULE_MONTH, " +
                    "$COLUMN_SCHEDULE_DAY, " +
                    "$COLUMN_SCHEDULE_CUSTOM_TIME_ID)")
        }

        fun getInstanceRecords(sqLiteDatabase: SQLiteDatabase) = ArrayList<InstanceRecord>().apply {
            sqLiteDatabase.query(TABLE_INSTANCES, null, null, null, null, null, null).use {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    add(cursorToInstanceRecord(it))
                    it.moveToNext()
                }
            }
        }

        private fun cursorToInstanceRecord(cursor: Cursor) = cursor.run {
            val id = getInt(0)
            val taskId = getInt(1)
            val done = if (isNull(2)) null else getLong(2)
            val scheduleYear = getInt(3)
            val scheduleMonth = getInt(4)
            val scheduleDay = getInt(5)
            val scheduleCustomTimeId = if (isNull(6)) null else getInt(6)
            val scheduleHour = if (isNull(7)) null else getInt(7)
            val scheduleMinute = if (isNull(8)) null else getInt(8)
            val instanceYear = if (isNull(9)) null else getInt(9)
            val instanceMonth = if (isNull(10)) null else getInt(10)
            val instanceDay = if (isNull(11)) null else getInt(11)
            val instanceCustomTimeId = if (isNull(12)) null else getInt(12)
            val instanceHour = if (isNull(13)) null else getInt(13)
            val instanceMinute = if (isNull(14)) null else getInt(14)
            val hierarchyTime = getLong(15)
            val notified = getInt(16) == 1
            val notificationShown = getInt(17) == 1

            Assert.assertTrue(scheduleHour == null == (scheduleMinute == null))
            Assert.assertTrue(scheduleHour == null != (scheduleCustomTimeId == null))

            Assert.assertTrue(instanceYear == null == (instanceMonth == null))
            Assert.assertTrue(instanceYear == null == (instanceDay == null))
            val hasInstanceDate = instanceYear != null

            Assert.assertTrue(instanceHour == null == (instanceMinute == null))
            Assert.assertTrue(instanceHour == null || instanceCustomTimeId == null)

            val hasInstanceTime = instanceHour != null || instanceCustomTimeId != null
            Assert.assertTrue(hasInstanceDate == hasInstanceTime)

            InstanceRecord(true, id, taskId, done, scheduleYear, scheduleMonth, scheduleDay, scheduleCustomTimeId, scheduleHour, scheduleMinute, instanceYear, instanceMonth, instanceDay, instanceCustomTimeId, instanceHour, instanceMinute, hierarchyTime, notified, notificationShown)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = Record.getMaxId(sqLiteDatabase, TABLE_INSTANCES, COLUMN_ID)
    }

    var instanceYear by observable(mInstanceYear) { _, _, _ -> changed = true }

    var instanceMonth by observable(mInstanceMonth) { _, _, _ -> changed = true }

    var instanceDay by observable(mInstanceDay) { _, _, _ -> changed = true }

    var done by observable(mDone) { _, _, _ -> changed = true }

    var instanceCustomTimeId by observable(mInstanceCustomTimeId) { _, _, _ -> changed = true }

    var instanceHour by observable(mInstanceHour) { _, _, _ -> changed = true }

    var instanceMinute by observable(mInstanceMinute) { _, _, _ -> changed = true }

    var notified by observable(mNotified) { _, _, _ -> changed = true }

    var notificationShown by observable(mNotificationShown) { _, _, _ -> changed = true }

    init {
        Assert.assertTrue(scheduleHour == null == (scheduleMinute == null))
        Assert.assertTrue(scheduleHour == null != (scheduleCustomTimeId == null))

        Assert.assertTrue(mInstanceYear == null == (mInstanceMonth == null))
        Assert.assertTrue(mInstanceYear == null == (mInstanceDay == null))
        val hasInstanceDate = mInstanceYear != null

        Assert.assertTrue(mInstanceHour == null == (mInstanceMinute == null))
        Assert.assertTrue(mInstanceHour == null || mInstanceCustomTimeId == null)
        val hasInstanceTime = mInstanceHour != null || mInstanceCustomTimeId != null
        Assert.assertTrue(hasInstanceDate == hasInstanceTime)
    }

    override val contentValues = ContentValues().apply {
        put(COLUMN_TASK_ID, taskId)
        put(COLUMN_DONE, done)
        put(COLUMN_SCHEDULE_YEAR, scheduleYear)
        put(COLUMN_SCHEDULE_MONTH, scheduleMonth)
        put(COLUMN_SCHEDULE_DAY, scheduleDay)
        put(COLUMN_SCHEDULE_CUSTOM_TIME_ID, scheduleCustomTimeId)
        put(COLUMN_SCHEDULE_HOUR, scheduleHour)
        put(COLUMN_SCHEDULE_MINUTE, scheduleMinute)
        put(COLUMN_INSTANCE_YEAR, instanceYear)
        put(COLUMN_INSTANCE_MONTH, instanceMonth)
        put(COLUMN_INSTANCE_DAY, instanceDay)
        put(COLUMN_INSTANCE_CUSTOM_TIME_ID, instanceCustomTimeId)
        put(COLUMN_INSTANCE_HOUR, instanceHour)
        put(COLUMN_INSTANCE_MINUTE, instanceMinute)
        put(COLUMN_HIERARCHY_TIME, hierarchyTime)
        put(COLUMN_NOTIFIED, if (notified) 1 else 0)
        put(COLUMN_NOTIFICATION_SHOWN, if (notificationShown) 1 else 0)
    }

    override val updateCommand = getUpdateCommand(TABLE_INSTANCES, COLUMN_ID, id)

    override val insertCommand = getInsertCommand(TABLE_INSTANCES)

    override val deleteCommand = getDeleteCommand(TABLE_INSTANCES, COLUMN_ID, id)
}
