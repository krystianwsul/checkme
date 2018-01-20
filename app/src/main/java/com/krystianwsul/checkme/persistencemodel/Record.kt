package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
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
    }

    var changed = false
        protected set

    private var deleted = false

    abstract val contentValues: ContentValues

    abstract val insertCommand: InsertCommand

    abstract val updateCommand: UpdateCommand

    abstract val deleteCommand: DeleteCommand

    fun getInsertCommand(tableName: String): InsertCommand {
        Assert.assertTrue(tableName.isNotEmpty())

        Log.e("asdf", toString() + " created? " + created)

        Assert.assertTrue(!created)

        created = true
        changed = false

        return InsertCommand(tableName, contentValues)
    }

    fun getUpdateCommand(tableName: String, idColumn: String, id: Int): UpdateCommand {
        Assert.assertTrue(tableName.isNotEmpty())
        Assert.assertTrue(idColumn.isNotEmpty())

        Assert.assertTrue(changed)

        changed = false

        return UpdateCommand(tableName, contentValues, idColumn + " = " + id)
    }

    fun getDeleteCommand(tableName: String, idColumn: String, id: Int): DeleteCommand {
        Assert.assertTrue(tableName.isNotEmpty())
        Assert.assertTrue(idColumn.isNotEmpty())

        Assert.assertTrue(deleted)

        deleted = false

        return DeleteCommand(tableName, idColumn + " = " + id)
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
