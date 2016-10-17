package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.TaskJson;

import junit.framework.Assert;

public class RemoteTaskRecord extends RemoteRecord {
    RemoteTaskRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteTaskRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private TaskJson getTaskJson() {
        TaskJson taskJson = mJsonWrapper.taskJson;
        Assert.assertTrue(taskJson != null);

        return taskJson;
    }

    @NonNull
    public String getName() {
        return getTaskJson().getName();
    }

    public long getStartTime() {
        return getTaskJson().getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return getTaskJson().getEndTime();
    }

    @Nullable
    public String getNote() {
        return getTaskJson().getNote();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        addValue(getId() + "/taskJson/endTime", endTime);
    }
}
