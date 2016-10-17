package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class RemoteRecord {
    private final String mId;
    final JsonWrapper mJsonWrapper;
    private final Map<String, Object> mUpdate;
    private final boolean mCreate;
    private boolean mCreated;
    private boolean mUpdated = false;

    RemoteRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        mId = id;
        mJsonWrapper = jsonWrapper;
        mUpdate = new HashMap<>();
        mCreate = false;
        mCreated = true;
    }

    RemoteRecord(@NonNull JsonWrapper jsonWrapper) {
        mId = DatabaseWrapper.getRecordId();
        mJsonWrapper = jsonWrapper;
        mUpdate = null;
        mCreate = true;
        mCreated = false;
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

    void addValues(@NonNull Map<String, Object> values) {
        Assert.assertTrue(!mUpdated);

        if (mUpdate != null) {
            Assert.assertTrue(mCreated);
            Assert.assertTrue(!mCreate);

            mUpdated = true;
            values.putAll(mUpdate);
        } else if (mCreate) {
            Assert.assertTrue(!mCreated);
            mCreated = true;

            values.put(getId(), mJsonWrapper);
        }
    }

    void addValue(@NonNull String key, @NonNull Object object) {
        Assert.assertTrue(!mCreate);
        Assert.assertTrue(!mUpdated);
        Assert.assertTrue(mUpdate != null);

        mUpdate.put(key, object);
    }
}
