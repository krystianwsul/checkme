package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;

import junit.framework.Assert;

public class RemoteMonthlyDayScheduleRecord extends RemoteScheduleRecord {
    RemoteMonthlyDayScheduleRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteMonthlyDayScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private MonthlyDayScheduleJson getMonthlyDayScheduleJson() {
        MonthlyDayScheduleJson monthlyDayScheduleJson = mJsonWrapper.monthlyDayScheduleJson;
        Assert.assertTrue(monthlyDayScheduleJson != null);

        return monthlyDayScheduleJson;
    }

    @NonNull
    @Override
    public String getTaskId() {
        return getMonthlyDayScheduleJson().getTaskId();
    }

    @Override
    public long getStartTime() {
        return getMonthlyDayScheduleJson().getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return getMonthlyDayScheduleJson().getEndTime();
    }

    public int getDayOfMonth() {
        return getMonthlyDayScheduleJson().getDayOfMonth();
    }

    public boolean getBeginningOfMonth() {
        return getMonthlyDayScheduleJson().getBeginningOfMonth();
    }

    @Nullable
    public Integer getCustomTimeId() {
        return getMonthlyDayScheduleJson().getCustomTimeId();
    }

    @Nullable
    public Integer getHour() {
        return getMonthlyDayScheduleJson().getHour();
    }

    @Nullable
    public Integer getMinute() {
        return getMonthlyDayScheduleJson().getMinute();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        addValue(getId() + "/monthlyDayScheduleJson/endTime", endTime);
    }
}
