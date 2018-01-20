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

        fun getUuidRecord(sqLiteDatabase: SQLiteDatabase) = getRecords(sqLiteDatabase, TABLE_UUID, this::cursorToCustomTimeRecord).single()

        private fun cursorToCustomTimeRecord(cursor: Cursor): UuidRecord {
            val uuid = cursor.getString(0)
            Assert.assertTrue(uuid.isNotEmpty())

            return UuidRecord(true, uuid)
        }
    }

    init {
        Assert.assertTrue(uuid.isNotEmpty())
    }

    override val contentValues = ContentValues().apply {
        put(COLUMN_UUID, uuid)
    }

    override val updateCommand get() = throw UnsupportedOperationException()

    override val insertCommand get() = getInsertCommand(TABLE_UUID)

    override val deleteCommand get() = throw UnsupportedOperationException()
}
