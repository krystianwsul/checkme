package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.krystianwsul.common.domain.Instance
import kotlin.properties.Delegates.observable

class InstanceShownRecord(
        created: Boolean,
        val id: Int,
        val taskId: String,
        val scheduleYear: Int,
        val scheduleMonth: Int,
        val scheduleDay: Int,
        val scheduleCustomTimeId: String?,
        val scheduleHour: Int?,
        val scheduleMinute: Int?,
        mNotified: Boolean,
        mNotificationShown: Boolean,
        mProjectId: String
) : Record(created), Instance.Shown {

    companion object {

        const val TABLE_INSTANCES_SHOWN = "instancesShown"

        const val COLUMN_ID = "_id"
        const val COLUMN_TASK_ID = "taskId"
        const val COLUMN_SCHEDULE_YEAR = "scheduleYear"
        const val COLUMN_SCHEDULE_MONTH = "scheduleMonth"
        const val COLUMN_SCHEDULE_DAY = "scheduleDay"
        const val COLUMN_SCHEDULE_CUSTOM_TIME_ID = "scheduleCustomTimeId"
        const val COLUMN_SCHEDULE_HOUR = "scheduleHour"
        const val COLUMN_SCHEDULE_MINUTE = "scheduleMinute"
        const val COLUMN_NOTIFIED = "notified"
        const val COLUMN_NOTIFICATION_SHOWN = "notificationShown"
        const val COLUMN_PROJECT_ID = "projectId"

        private const val INDEX_HOUR_MINUTE = "instanceShownIndexTaskScheduleHourMinute"
        private const val INDEX_CUSTOM_TIME_ID = "instanceShownIndexTaskScheduleCustomTimeId"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_INSTANCES_SHOWN " +
                    "($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_TASK_ID TEXT NOT NULL, " +
                    "$COLUMN_SCHEDULE_YEAR INTEGER NOT NULL, " +
                    "$COLUMN_SCHEDULE_MONTH INTEGER NOT NULL, " +
                    "$COLUMN_SCHEDULE_DAY INTEGER NOT NULL, " +
                    "$COLUMN_SCHEDULE_CUSTOM_TIME_ID TEXT, " +
                    "$COLUMN_SCHEDULE_HOUR INTEGER, " +
                    "$COLUMN_SCHEDULE_MINUTE INTEGER, " +
                    "$COLUMN_NOTIFIED INTEGER NOT NULL DEFAULT 0, " +
                    "$COLUMN_NOTIFICATION_SHOWN INTEGER NOT NULL DEFAULT 0, " +
                    "$COLUMN_PROJECT_ID TEXT NOT NULL);")

            sqLiteDatabase.execSQL("CREATE UNIQUE INDEX $INDEX_HOUR_MINUTE ON $TABLE_INSTANCES_SHOWN " +
                    "(" +
                    "$COLUMN_PROJECT_ID, " +
                    "$COLUMN_TASK_ID, " +
                    "$COLUMN_SCHEDULE_YEAR, " +
                    "$COLUMN_SCHEDULE_MONTH, " +
                    "$COLUMN_SCHEDULE_DAY, " +
                    "$COLUMN_SCHEDULE_HOUR, " +
                    COLUMN_SCHEDULE_MINUTE + ")")

            sqLiteDatabase.execSQL("CREATE UNIQUE INDEX $INDEX_CUSTOM_TIME_ID ON $TABLE_INSTANCES_SHOWN " +
                    "(" +
                    "$COLUMN_PROJECT_ID, " +
                    "$COLUMN_TASK_ID, " +
                    "$COLUMN_SCHEDULE_YEAR, " +
                    "$COLUMN_SCHEDULE_MONTH, " +
                    "$COLUMN_SCHEDULE_DAY, " +
                    "$COLUMN_SCHEDULE_CUSTOM_TIME_ID)")
        }

        fun getInstancesShownRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_INSTANCES_SHOWN, this::cursorToInstanceShownRecord)

        private fun cursorToInstanceShownRecord(cursor: Cursor) = cursor.run {
            val id = getInt(0)
            val taskId = getString(1)
            val scheduleYear = getInt(2)
            val scheduleMonth = getInt(3)
            val scheduleDay = getInt(4)
            val scheduleCustomTimeId = if (isNull(5)) null else getString(5)
            val scheduleHour = if (isNull(6)) null else getInt(6)
            val scheduleMinute = if (isNull(7)) null else getInt(7)
            val notified = getInt(8) == 1
            val notificationShown = getInt(9) == 1
            val projectId = getString(10)

            check(scheduleHour == null == (scheduleMinute == null))
            check(scheduleHour == null != (scheduleCustomTimeId == null))

            InstanceShownRecord(true, id, taskId, scheduleYear, scheduleMonth, scheduleDay, scheduleCustomTimeId, scheduleHour, scheduleMinute, notified, notificationShown, projectId)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = getMaxId(sqLiteDatabase, TABLE_INSTANCES_SHOWN, COLUMN_ID)
    }

    override var notified by observable(mNotified) { _, _, _ -> changed = true }

    override var notificationShown by observable(mNotificationShown) { _, _, _ -> changed = true }

    var projectId by observable(mProjectId) { _, _, _ -> changed = true }

    init {
        check(scheduleHour == null == (scheduleMinute == null))
        check(scheduleHour == null != (scheduleCustomTimeId == null))
        check(mProjectId.isNotEmpty())
    }

    override val contentValues
        get() = ContentValues().apply {
            put(COLUMN_TASK_ID, taskId)
            put(COLUMN_SCHEDULE_YEAR, scheduleYear)
            put(COLUMN_SCHEDULE_MONTH, scheduleMonth)
            put(COLUMN_SCHEDULE_DAY, scheduleDay)
            put(COLUMN_SCHEDULE_CUSTOM_TIME_ID, scheduleCustomTimeId)
            put(COLUMN_SCHEDULE_HOUR, scheduleHour)
            put(COLUMN_SCHEDULE_MINUTE, scheduleMinute)
            put(COLUMN_NOTIFIED, if (notified) 1 else 0)
            put(COLUMN_NOTIFICATION_SHOWN, if (notificationShown) 1 else 0)
            put(COLUMN_PROJECT_ID, projectId)
        }

    override val commandTable = TABLE_INSTANCES_SHOWN
    override val commandIdColumn = COLUMN_ID
    override val commandId = id
}
