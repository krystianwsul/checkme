package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;

import junit.framework.Assert;

public class RemoteSingleScheduleRecord extends RemoteScheduleRecord {
    RemoteSingleScheduleRecord(@NonNull String id, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(id, remoteTaskRecord, scheduleWrapper);
    }

    RemoteSingleScheduleRecord(@NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(remoteTaskRecord, scheduleWrapper);
    }

    @NonNull
    private SingleScheduleJson getSingleScheduleJson() {
        SingleScheduleJson singleScheduleJson = getCreateObject().singleScheduleJson;
        Assert.assertTrue(singleScheduleJson != null);

        return singleScheduleJson;
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
    public String getCustomTimeId() {
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
        addValue(getKey() + "/singleScheduleJson/endTime", endTime);
    }
}
