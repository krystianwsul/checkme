package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import junit.framework.Assert;

abstract class Record {
    boolean mChanged = false;

    abstract ContentValues getContentValues();

    void update(SQLiteDatabase sqLiteDatabase, String tableName, String idColumn, int id) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        if (!mChanged)
            return;

        sqLiteDatabase.update(tableName, getContentValues(), idColumn + " = " + id, null);
    }

    abstract void update(SQLiteDatabase sqLiteDatabase);
}
