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

public class RemoteManager {
    private boolean mSaved = false;

    @NonNull
    public final Map<String, RemoteCustomTimeRecord> mRemoteCustomTimeRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteProjectRecord> mRemoteProjectRecords = new HashMap<>();

    public RemoteManager(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children) {
        for (DataSnapshot child : children) {
            Assert.assertTrue(child != null);

            String key = child.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(key));

            JsonWrapper jsonWrapper = child.getValue(JsonWrapper.class);
            Assert.assertTrue(jsonWrapper != null);

            if (jsonWrapper.projectJson != null) {
                Assert.assertTrue(jsonWrapper.customTimeJson == null);

                RemoteProjectRecord remoteProjectRecord = new RemoteProjectRecord(domainFactory, key, jsonWrapper);

                mRemoteProjectRecords.put(key, remoteProjectRecord);
            } else {
                Assert.assertTrue(jsonWrapper.customTimeJson != null);

                mRemoteCustomTimeRecords.put(key, new RemoteCustomTimeRecord(key, jsonWrapper));
            }
        }
    }

    public void save(boolean causedByRemote) {
        Map<String, Object> values = new HashMap<>();

        Stream.of(mRemoteCustomTimeRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteProjectRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Log.e("asdf", "RemoteManager.save values: " + values);

        if (!values.isEmpty()) {
            Assert.assertTrue(!causedByRemote); // to prevent an infinite loop

            mSaved = true;
            DatabaseWrapper.updateRecords(values);
        }
    }

    public boolean isSaved() {
        return mSaved;
    }

    @NonNull
    public RemoteCustomTimeRecord newRemoteCustomTimeRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteCustomTimeRecord remoteCustomTimeRecord = new RemoteCustomTimeRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.getId()));

        mRemoteCustomTimeRecords.put(remoteCustomTimeRecord.getId(), remoteCustomTimeRecord);
        return remoteCustomTimeRecord;
    }

    @NonNull
    public RemoteProjectRecord newRemoteProjectRecord(@NonNull DomainFactory domainFactory, @NonNull JsonWrapper jsonWrapper) {
        RemoteProjectRecord remoteProjectRecord = new RemoteProjectRecord(domainFactory, jsonWrapper);
        Assert.assertTrue(!mRemoteProjectRecords.containsKey(remoteProjectRecord.getId()));

        mRemoteProjectRecords.put(remoteProjectRecord.getId(), remoteProjectRecord);
        return remoteProjectRecord;
    }
}
