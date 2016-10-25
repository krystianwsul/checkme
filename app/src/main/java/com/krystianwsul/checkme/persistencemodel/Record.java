package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

abstract class Record {
    private boolean mCreated;
    boolean mChanged = false;
    private boolean mDeleted = false;

    static int getMaxId(SQLiteDatabase sqLiteDatabase, String tableName, String idColumn) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        /*
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT MAX(" + idColumn + ") FROM " + tableName, null);
        Assert.assertTrue(cursor.getColumnCount() == 1);
        cursor.moveToFirst();

        int max = (cursor.isNull(0) ? 0 : cursor.getInt(0));

        cursor.close();
        */

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT seq FROM SQLITE_SEQUENCE WHERE name='" + tableName + "'", null);
        cursor.moveToFirst();

        int max;
        if (cursor.isAfterLast()) {
            max = 0;
        } else {
            max = cursor.getInt(0);
        }

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

    @NonNull
    DeleteCommand getDeleteCommand(@NonNull String tableName, @NonNull String idColumn, int id) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        Assert.assertTrue(mDeleted);

        mDeleted = false;

        return new DeleteCommand(tableName, idColumn + " = " + id);
    }

    boolean needsInsert() {
        return !mCreated;
    }

    boolean needsUpdate() {
        Assert.assertTrue(mCreated);

        return (mChanged && !mDeleted);
    }

    boolean needsDelete() {
        Assert.assertTrue(mCreated);

        return mDeleted;
    }

    public void delete() {
        mDeleted = true;
    }

    @SuppressWarnings("unused")
    @NonNull
    abstract InsertCommand getInsertCommand();

    @SuppressWarnings("unused")
    @NonNull
    abstract UpdateCommand getUpdateCommand();

    @SuppressWarnings("unused")
    @NonNull
    abstract DeleteCommand getDeleteCommand();
}
