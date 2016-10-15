package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;

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

            TaskWrapper taskWrapper = child.getValue(TaskWrapper.class);
            Assert.assertTrue(taskWrapper != null);

            if (taskWrapper.taskRecord != null) {
                Assert.assertTrue(taskWrapper.taskHierarchyRecord == null);

                mRemoteTasks.put(key, new RemoteTask(domainFactory, key, taskWrapper));
            } else {
                Assert.assertTrue(taskWrapper.taskHierarchyRecord != null);

                mRemoteTaskHierarchies.put(key, new RemoteTaskHierarchy(domainFactory, key, taskWrapper.taskHierarchyRecord));
            }
        }
    }
}
