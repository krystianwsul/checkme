package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Parcelable
import android.text.TextUtils

import kotlinx.parcelize.Parcelize

@Parcelize
class UpdateCommand(private val tableName: String, private val contentValues: ContentValues, private val whereClause: String) : Parcelable {

    init {
        check(!TextUtils.isEmpty(tableName))
        check(contentValues.size() > 0)
        check(!TextUtils.isEmpty(whereClause))
    }

    fun execute(sqLiteDatabase: SQLiteDatabase) {
        val updated = sqLiteDatabase.update(tableName, contentValues, whereClause, null)

        if (updated != 1)
            throw IllegalStateException("tableName == $tableName, whereClause == $whereClause, updated == $updated contentValues: " + contentValues.keySet().joinToString(", ") { it + ": " + contentValues.get(it) })
    }
}
