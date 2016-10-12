package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.IgnoreExtraProperties;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
@IgnoreExtraProperties
public class TaskWrapper {
    public Map<String, Boolean> taskOf;
    public RemoteTaskRecord taskRecord;
    public RemoteTaskHierarchyRecord taskHierarchyRecord;

    public TaskWrapper() {

    }

    TaskWrapper(@NonNull UserData userData, @NonNull List<UserData> friends, @NonNull RemoteTaskRecord remoteTaskRecord) {
        Assert.assertTrue(!friends.isEmpty());
        Assert.assertTrue(!friends.contains(userData));

        taskOf = Stream.of(friends)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));
        taskOf.put(UserData.getKey(userData.email), true);

        taskRecord = remoteTaskRecord;
        taskHierarchyRecord = null;
    }

    TaskWrapper(@NonNull UserData userData, @NonNull List<UserData> friends, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        Assert.assertTrue(!friends.isEmpty());
        Assert.assertTrue(!friends.contains(userData));

        taskOf = Stream.of(friends)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));
        taskOf.put(UserData.getKey(userData.email), true);

        taskRecord = null;
        taskHierarchyRecord = remoteTaskHierarchyRecord;
    }
}
