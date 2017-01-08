package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteProjectManager {
    private boolean mSaved = false;

    @NonNull
    public final Map<String, RemoteProjectRecord> mRemoteProjectRecords = new HashMap<>();

    public RemoteProjectManager(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children) {
        for (DataSnapshot child : children) {
            Assert.assertTrue(child != null);

            String key = child.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(key));

            JsonWrapper jsonWrapper = child.getValue(JsonWrapper.class);
            Assert.assertTrue(jsonWrapper != null);

            RemoteProjectRecord remoteProjectRecord = new RemoteProjectRecord(domainFactory, key, jsonWrapper);

            mRemoteProjectRecords.put(key, remoteProjectRecord);
        }
    }

    public void save() {
        Map<String, Object> values = new HashMap<>();

        Stream.of(mRemoteProjectRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Log.e("asdf", "RemoteProjectManager.save values: " + values);

        if (!values.isEmpty()) {
            mSaved = true;
            DatabaseWrapper.updateRecords(values);
        }
    }

    public boolean isSaved() {
        return mSaved;
    }

    @NonNull
    public RemoteProjectRecord newRemoteProjectRecord(@NonNull DomainFactory domainFactory, @NonNull JsonWrapper jsonWrapper) {
        RemoteProjectRecord remoteProjectRecord = new RemoteProjectRecord(domainFactory, jsonWrapper);
        Assert.assertTrue(!mRemoteProjectRecords.containsKey(remoteProjectRecord.getId()));

        mRemoteProjectRecords.put(remoteProjectRecord.getId(), remoteProjectRecord);
        return remoteProjectRecord;
    }
}
