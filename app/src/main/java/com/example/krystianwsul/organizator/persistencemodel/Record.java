package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.text.TextUtils;

import junit.framework.Assert;

abstract class Record {
    private boolean mCreated;
    boolean mChanged = false;

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
