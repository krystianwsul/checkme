package com.krystianwsul.checkme.persistencemodel

import android.database.sqlite.SQLiteDatabase
import android.os.Parcelable
import android.text.TextUtils

import kotlinx.android.parcel.Parcelize

@Parcelize
class DeleteCommand(private val tableName: String, private val whereClause: String) : Parcelable {

    init {
        check(!TextUtils.isEmpty(tableName))
        check(!TextUtils.isEmpty(whereClause))
    }

    fun execute(sqLiteDatabase: SQLiteDatabase) {
        val deleted = sqLiteDatabase.delete(tableName, whereClause, null)

        if (deleted != 1)
            throw IllegalStateException("tableName == $tableName, whereClause == $whereClause, deleted == $deleted")
    }
}
