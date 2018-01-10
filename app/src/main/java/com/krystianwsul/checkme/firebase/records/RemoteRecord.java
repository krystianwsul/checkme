package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

abstract public class RemoteRecord {
    boolean mDelete = false;
    boolean mDeleted = false;

    final boolean mCreate;
    boolean mCreated = false;

    final Map<String, Object> mUpdate;
    boolean mUpdated = false;

    RemoteRecord(boolean create) {
        if (create) {
            mCreate = true;
            mUpdate = null;
        } else {
            mCreate = false;
            mUpdate = new HashMap<>();
        }
    }

    @NonNull
    protected abstract String getKey();

    @NonNull
    protected abstract Object getCreateObject();

    void getValues(@NonNull Map<String, Object> values) {
        Assert.assertTrue(!mDeleted);
        Assert.assertTrue(!mCreated);
        Assert.assertTrue(!mUpdated);

        if (mDelete) {
            Log.e("asdf", "RemoteRecord.getValues deleting " + this);

            Assert.assertTrue(!mCreate);
            Assert.assertTrue(mUpdate != null);

            mDeleted = true;
            values.put(getKey(), null);
        } else if (mCreate) {
            Log.e("asdf", "RemoteRecord.getValues creating " + this);

            Assert.assertTrue(mUpdate == null);

            mCreated = true;
            values.put(getKey(), getCreateObject());
        } else {
            Assert.assertTrue(mUpdate != null);

            if (!mUpdate.isEmpty()) {
                Log.e("asdf", "RemoteRecord.getValues updating " + this);

                mUpdated = true;
                values.putAll(mUpdate);
            }
        }
    }

    void addValue(@NonNull String key, @Nullable Object object) {
        Assert.assertTrue(!mDelete);
        Assert.assertTrue(!mDeleted);
        Assert.assertTrue(!mCreated);
        Assert.assertTrue(!mUpdated);

        if (mCreate) {
            Assert.assertTrue(mUpdate == null);
        } else {
            Assert.assertTrue(mUpdate != null);

            mUpdate.put(key, object);
        }
    }

    public void delete() {
        Assert.assertTrue(!mDeleted);
        Assert.assertTrue(!mUpdated);
        Assert.assertTrue(!mCreated);
        Assert.assertTrue(!mDelete);
        Assert.assertTrue(!mCreate);
        Assert.assertTrue(mUpdate != null);

        mDelete = true;
    }
}
