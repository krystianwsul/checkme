package com.krystianwsul.checkme.persistencemodel

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Parcelable
import android.text.TextUtils
import junit.framework.Assert
import kotlinx.android.parcel.Parcelize

@Parcelize
class InsertCommand(private val tableName: String, private val contentValues: ContentValues) : Parcelable {

    init {
        Assert.assertTrue(!TextUtils.isEmpty(tableName))
        Assert.assertTrue(contentValues.size() > 0)
    }

    internal fun execute(sqLiteDatabase: SQLiteDatabase?) {
        Assert.assertTrue(sqLiteDatabase != null)

        val id = sqLiteDatabase!!.insert(tableName, null, contentValues)
        Assert.assertTrue(id != -1L)
    }
}
