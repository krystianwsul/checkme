package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import junit.framework.Assert
import java.util.*

class UuidRecord(created: Boolean, val uuid: String) : Record(created) {

    companion object {

        val TABLE_UUID = "uuid"

        private const val COLUMN_UUID = "uuid"

        fun newUuid() = UUID.randomUUID().toString()

        fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE $TABLE_UUID " +
                    "($COLUMN_UUID TEXT NOT NULL);")

            UuidRecord(false, newUuid()).insertCommand.execute(sqLiteDatabase)
        }

        fun getUuidRecord(sqLiteDatabase: SQLiteDatabase): UuidRecord {
            val cursor = sqLiteDatabase.query(TABLE_UUID, null, null, null, null, null, null)
            cursor.moveToFirst()

            val uuidRecord = cursorToCustomTimeRecord(cursor)

            Assert.assertTrue(cursor.isLast)

            return uuidRecord
        }

        private fun cursorToCustomTimeRecord(cursor: Cursor): UuidRecord {
            val uuid = cursor.getString(0)
            Assert.assertTrue(uuid.isNotEmpty())

            return UuidRecord(true, uuid)
        }
    }

    init {
        Assert.assertTrue(uuid.isNotEmpty())
    }

    override fun getContentValues() = ContentValues().apply {
        put(COLUMN_UUID, uuid)
    }

    override fun getUpdateCommand() = throw UnsupportedOperationException()

    override fun getInsertCommand() = getInsertCommand(TABLE_UUID)

    override fun getDeleteCommand() = throw UnsupportedOperationException()
}
