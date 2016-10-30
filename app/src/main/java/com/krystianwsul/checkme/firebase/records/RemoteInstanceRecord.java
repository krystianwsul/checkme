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

    public RemoteInstanceRecord(@NonNull JsonWrapper jsonWrapper) {
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
    public String getScheduleCustomTimeId() {
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
    public String getInstanceCustomTimeId() {
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

    public void setInstanceYear(int instanceYear) {
        getInstanceJson().setInstanceYear(instanceYear);
        addValue(getId() + "/instanceJson/instanceYear", instanceYear);
    }

    public void setInstanceMonth(int instanceMonth) {
        getInstanceJson().setInstanceMonth(instanceMonth);
        addValue(getId() + "/instanceJson/instanceMonth", instanceMonth);
    }

    public void setInstanceDay(int instanceDay) {
        getInstanceJson().setInstanceDay(instanceDay);
        addValue(getId() + "/instanceJson/instanceDay", instanceDay);
    }

    public void setInstanceCustomTimeId(@Nullable String instanceCustomTimeId) {
        getInstanceJson().setInstanceCustomTimeId(instanceCustomTimeId);
        addValue(getId() + "/instanceJson/instanceCustomTimeId", instanceCustomTimeId);
    }

    public void setInstanceHour(@Nullable Integer instanceHour) {
        getInstanceJson().setInstanceHour(instanceHour);
        addValue(getId() + "/instanceJson/instanceHour", instanceHour);
    }

    public void setInstanceMinute(@Nullable Integer instanceMinute) {
        getInstanceJson().setInstanceMinute(instanceMinute);
        addValue(getId() + "/instanceJson/instanceMinute", instanceMinute);
    }

    public void setDone(@Nullable Long done) {
        getInstanceJson().setDone(done);
        addValue(getId() + "/instanceJson/done", done);
    }
}
