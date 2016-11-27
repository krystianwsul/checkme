package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteManager {
    private boolean mSaved = false;

    @NonNull
    public final Map<String, RemoteTaskRecord> mRemoteTaskRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteTaskHierarchyRecord> mRemoteTaskHierarchyRecords = new HashMap<>();

    @NonNull
    public final Map<String, RemoteCustomTimeRecord> mRemoteCustomTimeRecords = new HashMap<>();

    public RemoteManager(@NonNull Iterable<DataSnapshot> children) {
        for (DataSnapshot child : children) {
            Assert.assertTrue(child != null);

            String key = child.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(key));

            JsonWrapper jsonWrapper = child.getValue(JsonWrapper.class);
            Assert.assertTrue(jsonWrapper != null);

            if (jsonWrapper.taskJson != null) {
                Assert.assertTrue(jsonWrapper.taskHierarchyJson == null);
                Assert.assertTrue(jsonWrapper.customTimeJson == null);

                mRemoteTaskRecords.put(key, new RemoteTaskRecord(key, jsonWrapper));
            } else if (jsonWrapper.taskHierarchyJson != null) {
                Assert.assertTrue(jsonWrapper.customTimeJson == null);

                mRemoteTaskHierarchyRecords.put(key, new RemoteTaskHierarchyRecord(key, jsonWrapper));
            } else {
                Assert.assertTrue(jsonWrapper.customTimeJson != null);

                mRemoteCustomTimeRecords.put(key, new RemoteCustomTimeRecord(key, jsonWrapper));
            }
        }
    }

    public void save(boolean causedByRemote) {
        Map<String, Object> values = new HashMap<>();

        Stream.of(mRemoteTaskRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteTaskHierarchyRecords.values())
                .forEach(remoteRecord -> remoteRecord.getValues(values));

        Stream.of(mRemoteCustomTimeRecords.values())
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
    public RemoteTaskRecord newRemoteTaskRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteTaskRecord remoteTaskRecord = new RemoteTaskRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteTaskRecords.containsKey(remoteTaskRecord.getId()));

        mRemoteTaskRecords.put(remoteTaskRecord.getId(), remoteTaskRecord);
        return remoteTaskRecord;
    }

    @NonNull
    public RemoteTaskHierarchyRecord newRemoteTaskHierarchyRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = new RemoteTaskHierarchyRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteTaskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.getId()));

        mRemoteTaskHierarchyRecords.put(remoteTaskHierarchyRecord.getId(), remoteTaskHierarchyRecord);
        return remoteTaskHierarchyRecord;
    }

    @NonNull
    public RemoteCustomTimeRecord newRemoteCustomTimeRecord(@NonNull JsonWrapper jsonWrapper) {
        RemoteCustomTimeRecord remoteCustomTimeRecord = new RemoteCustomTimeRecord(jsonWrapper);
        Assert.assertTrue(!mRemoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.getId()));

        mRemoteCustomTimeRecords.put(remoteCustomTimeRecord.getId(), remoteCustomTimeRecord);
        return remoteCustomTimeRecord;
    }
}
