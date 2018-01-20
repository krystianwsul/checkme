package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import junit.framework.Assert

abstract class Record(private var created: Boolean) {

    companion object {

        fun getMaxId(sqLiteDatabase: SQLiteDatabase, tableName: String, idColumn: String): Int {
            Assert.assertTrue(tableName.isNotEmpty())
            Assert.assertTrue(idColumn.isNotEmpty())

            return sqLiteDatabase.rawQuery("SELECT seq FROM SQLITE_SEQUENCE WHERE name='$tableName'", null).use {
                it.moveToFirst()

                if (it.isAfterLast) 0 else it.getInt(0)
            }
        }

        fun <T> getRecords(sqLiteDatabase: SQLiteDatabase, tableName: String, cursorToRecord: (Cursor) -> T): List<T> where T : Record = mutableListOf<T>().apply {
            sqLiteDatabase.query(tableName, null, null, null, null, null, null).use {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    add(cursorToRecord(it))
                    it.moveToNext()
                }
            }
        }
    }

    var changed = false
        protected set

    private var deleted = false

    abstract val contentValues: ContentValues
    abstract val commandTable: String
    abstract val commandIdColumn: String
    abstract val commandId: Int

    val insertCommand: InsertCommand
        get() {
            Assert.assertTrue(commandTable.isNotEmpty())

            Log.e("asdf", toString() + " created? " + created)

            Assert.assertTrue(!created)

            created = true
            changed = false

            return InsertCommand(commandTable, contentValues)
        }

    val updateCommand: UpdateCommand
        get() {
            Assert.assertTrue(commandTable.isNotEmpty())
            Assert.assertTrue(commandIdColumn.isNotEmpty())

            Assert.assertTrue(changed)

            changed = false

            return UpdateCommand(commandTable, contentValues, commandIdColumn + " = " + commandId)
        }

    val deleteCommand: DeleteCommand
        get() {
            Assert.assertTrue(commandTable.isNotEmpty())
            Assert.assertTrue(commandIdColumn.isNotEmpty())

            Assert.assertTrue(deleted)

            deleted = false

            return DeleteCommand(commandTable, commandIdColumn + " = " + commandId)
        }

    fun needsInsert() = !created

    fun needsUpdate(): Boolean {
        Assert.assertTrue(created)

        return changed && !deleted
    }

    fun needsDelete(): Boolean {
        Assert.assertTrue(created)

        return deleted
    }

    fun delete() {
        deleted = true
    }
}
