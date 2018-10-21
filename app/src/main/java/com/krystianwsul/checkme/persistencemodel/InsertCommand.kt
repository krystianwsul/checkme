package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Parcelable
import android.text.TextUtils

import kotlinx.android.parcel.Parcelize

@Parcelize
class InsertCommand(private val tableName: String, private val contentValues: ContentValues) : Parcelable {

    init {
        check(!TextUtils.isEmpty(tableName))
        check(contentValues.size() > 0)
    }

    internal fun execute(sqLiteDatabase: SQLiteDatabase?) {
        check(sqLiteDatabase != null)

        val id = sqLiteDatabase!!.insert(tableName, null, contentValues)
        check(id != -1L)
    }
}
