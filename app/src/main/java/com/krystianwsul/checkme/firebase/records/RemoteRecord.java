package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class RemoteRecord {
    private final String mId;
    final JsonWrapper mJsonWrapper;

    private boolean mDelete = false;
    private boolean mDeleted = false;

    private final boolean mCreate;
    private boolean mCreated = false;

    private final Map<String, Object> mUpdate;
    private boolean mUpdated = false;

    RemoteRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        mId = id;
        mJsonWrapper = jsonWrapper;

        mCreate = false;
        mUpdate = new HashMap<>();
    }

    RemoteRecord(@NonNull JsonWrapper jsonWrapper) {
        mId = DatabaseWrapper.getRecordId();
        mJsonWrapper = jsonWrapper;

        mCreate = true;
        mUpdate = null;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public Set<String> getRecordOf() {
        return mJsonWrapper.recordOf.keySet();
    }

    @NonNull
    public JsonWrapper getJsonWrapper() {
        return mJsonWrapper;
    }

    void getValues(@NonNull Map<String, Object> values) {
        Assert.assertTrue(!mUpdated);

        if (mDelete) {
            Log.e("asdf", "RemoteRecord.getValues deleting " + mJsonWrapper);

            Assert.assertTrue(!mDeleted);
            Assert.assertTrue(!mCreate);
            Assert.assertTrue(!mCreated);
            Assert.assertTrue(mUpdate != null);
            Assert.assertTrue(mUpdate.isEmpty());
            Assert.assertTrue(!mUpdated);

            mDeleted = true;
            values.put(getId(), null);
        } else if (mCreate) {
            Log.e("asdf", "RemoteRecord.getValues creating " + mJsonWrapper);

            Assert.assertTrue(!mDeleted);
            Assert.assertTrue(!mCreated);
            Assert.assertTrue(mUpdate == null);
            Assert.assertTrue(!mUpdated);

            mCreated = true;
            values.put(getId(), mJsonWrapper);
        } else if (mUpdate != null) {
            Log.e("asdf", "RemoteRecord.getValues updating " + mJsonWrapper);

            Assert.assertTrue(!mDeleted);
            Assert.assertTrue(!mCreated);
            Assert.assertTrue(!mUpdated);

            mUpdated = true;
            values.putAll(mUpdate);
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
        Assert.assertTrue(!mCreate);
        Assert.assertTrue(mUpdate != null);
        Assert.assertTrue(mUpdate.isEmpty());
        Assert.assertTrue(!mUpdated);

        mDelete = true;
    }
}
