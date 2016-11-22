package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class RootRemoteRecord extends RemoteRecord {
    private final String mId;
    final JsonWrapper mJsonWrapper;

    RootRemoteRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(false);

        mId = id;
        mJsonWrapper = jsonWrapper;
    }

    RootRemoteRecord(@NonNull JsonWrapper jsonWrapper) {
        super(true);

        mId = DatabaseWrapper.getRootRecordId();
        mJsonWrapper = jsonWrapper;
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
    @Override
    protected String getKey() {
        return getId();
    }

    @NonNull
    @Override
    protected JsonWrapper getCreateObject() {
        return mJsonWrapper;
    }

    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        Assert.assertTrue(Stream.of(addedFriends)
                .noneMatch(removedFriends::contains));

        HashSet<String> recordOf = new HashSet<>(getRecordOf());
        recordOf.addAll(addedFriends);
        recordOf.removeAll(removedFriends);

        setRecordOf(recordOf);
    }

    private void setRecordOf(@NonNull Set<String> recordOf) {
        Map<String, Boolean> mapRecordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(key -> key, key -> true));

        mJsonWrapper.setRecordOf(mapRecordOf);

        addValue(getId() + "/recordOf", mapRecordOf);
    }
}
