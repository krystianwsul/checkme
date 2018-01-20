package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.krystianwsul.checkme.domainmodel.CustomTimeRecord
import junit.framework.Assert
import kotlin.properties.Delegates.observable

class LocalCustomTimeRecord(
        created: Boolean,
        val id: Int,
        mName: String,
        mSundayHour: Int,
        mSundayMinute: Int,
        mMondayHour: Int,
        mMondayMinute: Int,
        mTuesdayHour: Int,
        mTuesdayMinute: Int,
        mWednesdayHour: Int,
        mWednesdayMinute: Int,
        mThursdayHour: Int,
        mThursdayMinute: Int,
        mFridayHour: Int,
        mFridayMinute: Int,
        mSaturdayHour: Int,
        mSaturdayMinute: Int,
        mCurrent: Boolean) : Record(created), CustomTimeRecord {

    companion object {

        val TABLE_CUSTOM_TIMES = "customTimes"

        val COLUMN_ID = "_id"
        private val COLUMN_NAME = "name"
        private val COLUMN_SUNDAY_HOUR = "sundayHour"
        private val COLUMN_SUNDAY_MINUTE = "sundayMinute"
        private val COLUMN_MONDAY_HOUR = "mondayHour"
        private val COLUMN_MONDAY_MINUTE = "mondayMinute"
        private val COLUMN_TUESDAY_HOUR = "tuesdayHour"
        private val COLUMN_TUESDAY_MINUTE = "tuesdayMinute"
        private val COLUMN_WEDNESDAY_HOUR = "wednesdayHour"
        private val COLUMN_WEDNESDAY_MINUTE = "wednesdayMinute"
        private val COLUMN_THURSDAY_HOUR = "thursdayHour"
        private val COLUMN_THURSDAY_MINUTE = "thursdayMinute"
        private val COLUMN_FRIDAY_HOUR = "fridayHour"
        private val COLUMN_FRIDAY_MINUTE = "fridayMinute"
        private val COLUMN_SATURDAY_HOUR = "saturdayHour"
        private val COLUMN_SATURDAY_MINUTE = "saturdayMinute"
        private val COLUMN_CURRENT = "current"

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_CUSTOM_TIMES"
                    + " ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "$COLUMN_NAME TEXT NOT NULL, "
                    + "$COLUMN_SUNDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_SUNDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_MONDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_MONDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_TUESDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_TUESDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_WEDNESDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_WEDNESDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_THURSDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_THURSDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_FRIDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_FRIDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_SATURDAY_HOUR INTEGER NOT NULL, "
                    + "$COLUMN_SATURDAY_MINUTE INTEGER NOT NULL, "
                    + "$COLUMN_CURRENT INTEGER NOT NULL DEFAULT 1);")
        }

        fun getCustomTimeRecords(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_CUSTOM_TIMES, this::cursorToCustomTimeRecord)

        private fun cursorToCustomTimeRecord(cursor: Cursor): LocalCustomTimeRecord = cursor.run {
            val id = getInt(0)
            val name = getString(1)
            val sundayHour = getInt(2)
            val sundayMinute = getInt(3)
            val mondayHour = getInt(4)
            val mondayMinute = getInt(5)
            val tuesdayHour = getInt(6)
            val tuesdayMinute = getInt(7)
            val wednesdayHour = getInt(8)
            val wednesdayMinute = getInt(9)
            val thursdayHour = getInt(10)
            val thursdayMinute = getInt(11)
            val fridayHour = getInt(12)
            val fridayMinute = getInt(13)
            val saturdayHour = getInt(14)
            val saturdayMinute = getInt(15)
            val current = getInt(16) == 1

            LocalCustomTimeRecord(true, id, name, sundayHour, sundayMinute, mondayHour, mondayMinute, tuesdayHour, tuesdayMinute, wednesdayHour, wednesdayMinute, thursdayHour, thursdayMinute, fridayHour, fridayMinute, saturdayHour, saturdayMinute, current)
        }

        fun getMaxId(sqLiteDatabase: SQLiteDatabase) = Record.getMaxId(sqLiteDatabase, TABLE_CUSTOM_TIMES, COLUMN_ID)
    }

    var name by observable(mName) { _, _, _ -> changed = true }

    override var sundayHour by observable(mSundayHour) { _, _, _ -> changed = true }
    override var sundayMinute by observable(mSundayMinute) { _, _, _ -> changed = true }

    override var mondayHour by observable(mMondayHour) { _, _, _ -> changed = true }
    override var mondayMinute by observable(mMondayMinute) { _, _, _ -> changed = true }

    override var tuesdayHour by observable(mTuesdayHour) { _, _, _ -> changed = true }
    override var tuesdayMinute by observable(mTuesdayMinute) { _, _, _ -> changed = true }

    override var wednesdayHour by observable(mWednesdayHour) { _, _, _ -> changed = true }
    override var wednesdayMinute by observable(mWednesdayMinute) { _, _, _ -> changed = true }

    override var thursdayHour by observable(mThursdayHour) { _, _, _ -> changed = true }
    override var thursdayMinute by observable(mThursdayMinute) { _, _, _ -> changed = true }

    override var fridayHour by observable(mFridayHour) { _, _, _ -> changed = true }
    override var fridayMinute by observable(mFridayMinute) { _, _, _ -> changed = true }

    override var saturdayHour by observable(mSaturdayHour) { _, _, _ -> changed = true }
    override var saturdayMinute by observable(mSaturdayMinute) { _, _, _ -> changed = true }

    var current by observable(mCurrent) { _, _, _ -> changed = true }

    init {
        Assert.assertTrue(mName.isNotEmpty())
    }

    override val contentValues = ContentValues().apply {
        put(COLUMN_NAME, name)
        put(COLUMN_SUNDAY_HOUR, sundayHour)
        put(COLUMN_SUNDAY_MINUTE, sundayMinute)
        put(COLUMN_MONDAY_HOUR, mondayHour)
        put(COLUMN_MONDAY_MINUTE, mondayMinute)
        put(COLUMN_TUESDAY_HOUR, tuesdayHour)
        put(COLUMN_TUESDAY_MINUTE, tuesdayMinute)
        put(COLUMN_WEDNESDAY_HOUR, wednesdayHour)
        put(COLUMN_WEDNESDAY_MINUTE, wednesdayMinute)
        put(COLUMN_THURSDAY_HOUR, thursdayHour)
        put(COLUMN_THURSDAY_MINUTE, thursdayMinute)
        put(COLUMN_FRIDAY_HOUR, fridayHour)
        put(COLUMN_FRIDAY_MINUTE, fridayMinute)
        put(COLUMN_SATURDAY_HOUR, saturdayHour)
        put(COLUMN_SATURDAY_MINUTE, saturdayMinute)
        put(COLUMN_CURRENT, current)
    }

    override val updateCommand get() = getUpdateCommand(TABLE_CUSTOM_TIMES, COLUMN_ID, id)

    override val insertCommand get() = getInsertCommand(TABLE_CUSTOM_TIMES)

    override val deleteCommand get() = getDeleteCommand(TABLE_CUSTOM_TIMES, COLUMN_ID, id)
}
