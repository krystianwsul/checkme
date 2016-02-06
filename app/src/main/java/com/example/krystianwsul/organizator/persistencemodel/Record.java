package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import junit.framework.Assert;

abstract class Record {
    boolean mCreated;
    boolean mChanged = false;

    abstract ContentValues getContentValues();

    protected Record(boolean created) {
        mCreated = created;
    }

    protected long create(SQLiteDatabase sqLiteDatabase, String tableName) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(!TextUtils.isEmpty(tableName));

        long id = sqLiteDatabase.insert(tableName, null, getContentValues());
        Assert.assertTrue(id != -1);

        mCreated = true;
        mChanged = false;

        return id;
    }

    protected void update(SQLiteDatabase sqLiteDatabase, String tableName, String idColumn, int id) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        if (!mChanged)
            return;

        sqLiteDatabase.update(tableName, getContentValues(), idColumn + " = " + id, null);

        mChanged = false;
    }

    abstract void update(SQLiteDatabase sqLiteDatabase);

    abstract void create(SQLiteDatabase sqLiteDatabase);
}
