package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import junit.framework.Assert;

abstract public class Record {
    private boolean mCreated;
    boolean changed = false;
    private boolean mDeleted = false;

    static int getMaxId(@NonNull SQLiteDatabase sqLiteDatabase, @NonNull String tableName, @NonNull String idColumn) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

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

    @NonNull
    abstract ContentValues getContentValues();

    Record(boolean created) {
        mCreated = created;
    }

    @NonNull
    InsertCommand getInsertCommand(@NonNull String tableName) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));

        Log.e("asdf", toString() + " created? " + mCreated);

        Assert.assertTrue(!mCreated);

        mCreated = true;
        changed = false;

        return new InsertCommand(tableName, getContentValues());
    }

    @NonNull
    UpdateCommand getUpdateCommand(@NonNull String tableName, @NonNull String idColumn, int id) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(idColumn));

        Assert.assertTrue(changed);

        changed = false;

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

        return (changed && !mDeleted);
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
