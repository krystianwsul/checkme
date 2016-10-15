package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class RemoteFactory {
    @NonNull
    public final Map<String, RemoteTask> mRemoteTasks = new HashMap<>();

    @NonNull
    public final Map<String, RemoteTaskHierarchy> mRemoteTaskHierarchies = new HashMap<>();

    public RemoteFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children) {
        for (DataSnapshot child : children) {
            Assert.assertTrue(child != null);

            String key = child.getKey();
            Assert.assertTrue(!TextUtils.isEmpty(key));

            JsonWrapper jsonWrapper = child.getValue(JsonWrapper.class);
            Assert.assertTrue(jsonWrapper != null);

            if (jsonWrapper.taskJson != null) {
                Assert.assertTrue(jsonWrapper.taskHierarchyJson == null);

                mRemoteTasks.put(key, new RemoteTask(domainFactory, new RemoteTaskRecord(key, jsonWrapper)));
            } else {
                Assert.assertTrue(jsonWrapper.taskHierarchyJson != null);

                mRemoteTaskHierarchies.put(key, new RemoteTaskHierarchy(domainFactory, new RemoteTaskHierarchyRecord(key, jsonWrapper)));
            }
        }
    }
}
