package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;

import junit.framework.Assert;

public class RemoteInstanceRecord extends RemoteRecord {
    RemoteInstanceRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteInstanceRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private InstanceJson getInstanceJson() {
        InstanceJson instanceJson = mJsonWrapper.instanceJson;
        Assert.assertTrue(instanceJson != null);

        return instanceJson;
    }

    @NonNull
    public String getTaskId() {
        return getInstanceJson().getTaskId();
    }

    public Long getDone() {
        return getInstanceJson().getDone();
    }

    public int getScheduleYear() {
        return getInstanceJson().getScheduleYear();
    }

    public int getScheduleMonth() {
        return getInstanceJson().getScheduleMonth();
    }

    public int getScheduleDay() {
        return getInstanceJson().getScheduleDay();
    }

    @Nullable
    public Integer getScheduleCustomTimeId() {
        return getInstanceJson().getScheduleCustomTimeId();
    }

    @Nullable
    public Integer getScheduleHour() {
        return getInstanceJson().getScheduleHour();
    }

    @Nullable
    public Integer getScheduleMinute() {
        return getInstanceJson().getScheduleMinute();
    }

    @Nullable
    public Integer getInstanceYear() {
        return getInstanceJson().getInstanceYear();
    }

    @Nullable
    public Integer getInstanceMonth() {
        return getInstanceJson().getInstanceMonth();
    }

    @Nullable
    public Integer getInstanceDay() {
        return getInstanceJson().getInstanceDay();
    }

    @Nullable
    public Integer getInstanceCustomTimeId() {
        return getInstanceJson().getInstanceCustomTimeId();
    }

    @Nullable
    public Integer getInstanceHour() {
        return getInstanceJson().getInstanceHour();
    }

    @Nullable
    public Integer getInstanceMinute() {
        return getInstanceJson().getInstanceMinute();
    }

    public long getHierarchyTime() {
        return getInstanceJson().getHierarchyTime();
    }
}
