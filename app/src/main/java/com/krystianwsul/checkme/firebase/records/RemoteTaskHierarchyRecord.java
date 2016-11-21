package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;

import junit.framework.Assert;

public class RemoteTaskHierarchyRecord extends RootRemoteRecord {
    RemoteTaskHierarchyRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteTaskHierarchyRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private TaskHierarchyJson getTaskHierarchyJson() {
        TaskHierarchyJson taskHierarchyJson = mJsonWrapper.taskHierarchyJson;
        Assert.assertTrue(taskHierarchyJson != null);

        return taskHierarchyJson;
    }

    public long getStartTime() {
        return getTaskHierarchyJson().getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return getTaskHierarchyJson().getEndTime();
    }

    @NonNull
    public String getParentTaskId() {
        return getTaskHierarchyJson().getParentTaskId();
    }

    @NonNull
    public String getChildTaskId() {
        return getTaskHierarchyJson().getChildTaskId();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getTaskHierarchyJson().setEndTime(endTime);
        addValue(getId() + "/taskHierarchyJson/endTime", endTime);
    }
}
