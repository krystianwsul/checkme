package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.IgnoreExtraProperties;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
@IgnoreExtraProperties
public class TaskWrapper {
    public Map<String, Boolean> taskOf;
    public RemoteTaskRecord taskRecord;
    public RemoteTaskHierarchyRecord taskHierarchyRecord;

    public TaskWrapper() {

    }

    TaskWrapper(@NonNull List<UserData> userDatas, @NonNull RemoteTaskRecord remoteTaskRecord) {
        Assert.assertTrue(!userDatas.isEmpty());

        taskOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskRecord = remoteTaskRecord;
        taskHierarchyRecord = null;
    }

    TaskWrapper(@NonNull List<UserData> userDatas, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        Assert.assertTrue(!userDatas.isEmpty());

        taskOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskRecord = null;
        taskHierarchyRecord = remoteTaskHierarchyRecord;
    }

    TaskWrapper(@NonNull Set<String> taskOf, @NonNull RemoteTaskRecord remoteTaskRecord) {
        Assert.assertTrue(!taskOf.isEmpty());

        this.taskOf = Stream.of(taskOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        taskRecord = remoteTaskRecord;
        taskHierarchyRecord = null;
    }

    TaskWrapper(@NonNull Set<String> taskOf, @NonNull RemoteTaskHierarchyRecord remoteTaskHierarchyRecord) {
        Assert.assertTrue(!taskOf.isEmpty());

        this.taskOf = Stream.of(taskOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        taskRecord = null;
        taskHierarchyRecord = remoteTaskHierarchyRecord;
    }
}
