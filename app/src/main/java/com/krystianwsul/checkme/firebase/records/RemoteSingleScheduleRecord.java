package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;

import junit.framework.Assert;

public class RemoteSingleScheduleRecord extends RemoteScheduleRecord {
    RemoteSingleScheduleRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteSingleScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private SingleScheduleJson getSingleScheduleJson() {
        SingleScheduleJson singleScheduleJson = mJsonWrapper.singleScheduleJson;
        Assert.assertTrue(singleScheduleJson != null);

        return singleScheduleJson;
    }

    @NonNull
    @Override
    public String getTaskId() {
        return getSingleScheduleJson().getTaskId();
    }

    @Override
    public long getStartTime() {
        return getSingleScheduleJson().getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return getSingleScheduleJson().getEndTime();
    }

    public int getYear() {
        return getSingleScheduleJson().getYear();
    }

    public int getMonth() {
        return getSingleScheduleJson().getMonth();
    }

    public int getDay() {
        return getSingleScheduleJson().getDay();
    }

    @Nullable
    public Integer getCustomTimeId() {
        return getSingleScheduleJson().getCustomTimeId();
    }

    @Nullable
    public Integer getHour() {
        return getSingleScheduleJson().getHour();
    }

    @Nullable
    public Integer getMinute() {
        return getSingleScheduleJson().getMinute();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getSingleScheduleJson().setEndTime(endTime);
        addValue(getId() + "/singleScheduleJson/endTime", endTime);
    }
}
