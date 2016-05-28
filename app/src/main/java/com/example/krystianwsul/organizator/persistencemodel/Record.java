package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import junit.framework.Assert;

abstract class Record {
    private boolean mCreated;
    boolean mChanged = false;

    static int getMaxId(SQLiteDatabase sqLiteDatabase, String tableName, String idColumn) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT MAX(" + idColumn + ") FROM " + tableName, null);
        Assert.assertTrue(cursor.getColumnCount() == 1);
        cursor.moveToFirst();

        int max = (cursor.isNull(0) ? 0 : cursor.getInt(0));

        cursor.close();

        return max;
    }

    abstract ContentValues getContentValues();

    Record(boolean created) {
        mCreated = created;
    }

    InsertCommand getInsertCommand(String tableName) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));

        Assert.assertTrue(!mCreated);

        mCreated = true;
        mChanged = false;

        return new InsertCommand(tableName, getContentValues());
    }

    UpdateCommand getUpdateCommand(String tableName, String idColumn, int id) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        Assert.assertTrue(mChanged);

        mChanged = false;

        return new UpdateCommand(tableName, getContentValues(), idColumn + " = " + id);
    }

    public boolean needsInsert() {
        return !mCreated;
    }

    public boolean needsUpdate() {
        Assert.assertTrue(mCreated);
        return mChanged;
    }

    abstract InsertCommand getInsertCommand();

    abstract UpdateCommand getUpdateCommand();
}
