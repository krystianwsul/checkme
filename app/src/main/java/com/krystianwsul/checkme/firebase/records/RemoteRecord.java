package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class RemoteRecord {
    private final String mId;
    protected final JsonWrapper mJsonWrapper;
    private final Map<String, Object> mUpdate;
    private final boolean mCreate;
    private boolean mCreated;

    public RemoteRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        mId = id;
        mJsonWrapper = jsonWrapper;
        mUpdate = new HashMap<>();
        mCreate = false;
        mCreated = true;
    }

    public RemoteRecord(@NonNull JsonWrapper jsonWrapper) {
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
}
