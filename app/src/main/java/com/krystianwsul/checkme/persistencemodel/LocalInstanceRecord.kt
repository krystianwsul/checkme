package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.krystianwsul.checkme.domainmodel.InstanceRecord
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import kotlin.properties.Delegates.observable

class LocalInstanceRecord(
        created: Boolean,
        val id: Int,
        val taskId: Int,
        mDone: Long?,
        override val scheduleYear: Int,
        override val scheduleMonth: Int,
        override val scheduleDay: Int,
        override val scheduleCustomTimeId: Int?,
        override val scheduleHour: Int?,
        override val scheduleMinute: Int?,
        mInstanceYear: Int?,
        mInstanceMonth: Int?,
        mInstanceDay: Int?,
        mInstanceCustomTimeId: Int?,
        mInstanceHour: Int?,
        mInstanceMinute: Int?,
        private val hierarchyTime: Long,
        mNotified: Boolean,
        mNotificationShown: Boolean,
        mOrdinal: Double?) : Record(created), InstanceRecord<Int> {

    companion object {

        const val TABLE_INSTANCES = "instances"

        private const val COLUMN_ID = "_id"
        private const val COLUMN_TASK_ID = "taskId"
        private const val COLUMN_DONE = "done"
        private const val COLUMN_SCHEDULE_YEAR = "scheduleYear"
        private const val COLUMN_SCHEDULE_MONTH = "scheduleMonth"
        private const val COLUMN_SCHEDULE_DAY = "scheduleDay"
        private const val COLUMN_SCHEDULE_CUSTOM_TIME_ID = "scheduleCustomTimeId"
        private const val COLUMN_SCHEDULE_HOUR = "scheduleHour"
        private const val COLUMN_SCHEDULE_MINUTE = "scheduleMinute"
        private const val COLUMN_INSTANCE_YEAR = "instanceYear"
        private const val COLUMN_INSTANCE_MONTH = "instanceMonth"
        private const val COLUMN_INSTANCE_DAY = "instanceDay"
        private const val COLUMN_INSTANCE_CUSTOM_TIME_ID = "instanceCustomTimeId"
        private const val COLUMN_INSTANCE_HOUR = "instanceHour"
        private const val COLUMN_INSTANCE_MINUTE = "instanceMinute"
        private const val COLUMN_HIERARCHY_TIME = "hierarchyTime"
        private const val COLUMN_NOTIFIED = "notified"
        private const val COLUMN_NOTIFICATION_SHOWN = "notificationShown"
        const val COLUMN_ORDINAL = "ordinal"

        private const val INDEX_TASK_SCHEDULE_HOUR_MINUTE = "instanceIndexTaskScheduleHourMinute"
        private const val INDEX_TASK_SCHEDULE_CUSTOM_TIME_ID = "instanceIndexTaskScheduleCustomTimeId"

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
                    "$COLUMN_NOTIFICATION_SHOWN INTEGER NOT NULL DEFAULT 0," +
                    "$COLUMN_ORDINAL REAL);")

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

        fun getInstanceRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_INSTANCES, this::cursorToInstanceRecord)

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
            val ordinal = if (isNull(18)) null else getDouble(18)

            check(scheduleHour == null == (scheduleMinute == null))
            check(scheduleHour == null != (scheduleCustomTimeId == null))

            check(instanceYear == null == (instanceMonth == null))
            check(instanceYear == null == (instanceDay == null))
            val hasInstanceDate = instanceYear != null

            check(instanceHour == null == (instanceMinute == null))
            check(instanceHour == null || instanceCustomTimeId == null)

            val hasInstanceTime = instanceHour != null || instanceCustomTimeId != null
            check(hasInstanceDate == hasInstanceTime)

            LocalInstanceRecord(true, id, taskId, done, scheduleYear, scheduleMonth, scheduleDay, scheduleCustomTimeId, scheduleHour, scheduleMinute, instanceYear, instanceMonth, instanceDay, instanceCustomTimeId, instanceHour, instanceMinute, hierarchyTime, notified, notificationShown, ordinal)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = Record.getMaxId(sqLiteDatabase, TABLE_INSTANCES, COLUMN_ID)
    }

    private var instanceYear by observable(mInstanceYear) { _, _, _ -> changed = true }
    private var instanceMonth by observable(mInstanceMonth) { _, _, _ -> changed = true }
    private var instanceDay by observable(mInstanceDay) { _, _, _ -> changed = true }

    override var instanceDate by observable(instanceYear?.let { Date(instanceYear!!, instanceMonth!!, instanceDay!!) }) { _, _, value ->
        instanceYear = value?.year
        instanceMonth = value?.month
        instanceDay = value?.day
    }

    override var done by observable(mDone) { _, _, _ -> changed = true }

    override var instanceCustomTimeId by observable(mInstanceCustomTimeId) { _, _, _ -> changed = true }

    private var instanceHour by observable(mInstanceHour) { _, _, _ -> changed = true }
    private var instanceMinute by observable(mInstanceMinute) { _, _, _ -> changed = true }

    override var instanceHourMinute by observable(instanceHour?.let { HourMinute(it, instanceMinute!!) }) { _, _, value ->
        instanceHour = value?.hour
        instanceMinute = value?.minute
    }

    var notified by observable(mNotified) { _, _, _ -> changed = true }

    var notificationShown by observable(mNotificationShown) { _, _, _ -> changed = true }

    override var ordinal by observable(mOrdinal) { _, _, _ -> changed = true }

    init {
        check(scheduleHour == null == (scheduleMinute == null))
        check(scheduleHour == null != (scheduleCustomTimeId == null))

        check(mInstanceYear == null == (mInstanceMonth == null))
        check(mInstanceYear == null == (mInstanceDay == null))
        val hasInstanceDate = mInstanceYear != null

        check(mInstanceHour == null == (mInstanceMinute == null))
        check(mInstanceHour == null || mInstanceCustomTimeId == null)
        val hasInstanceTime = mInstanceHour != null || mInstanceCustomTimeId != null
        check(hasInstanceDate == hasInstanceTime)
    }

    override val contentValues
        get() = ContentValues().apply {
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
            put(COLUMN_ORDINAL, ordinal)
        }

    override val commandTable = TABLE_INSTANCES
    override val commandIdColumn = COLUMN_ID
    override val commandId = id
}
