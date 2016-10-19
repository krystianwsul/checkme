package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;

import junit.framework.Assert;

public class RemoteMonthlyWeekScheduleRecord extends RemoteScheduleRecord {
    RemoteMonthlyWeekScheduleRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteMonthlyWeekScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private MonthlyWeekScheduleJson getMonthlyWeekScheduleJson() {
        MonthlyWeekScheduleJson monthlyWeekScheduleJson = mJsonWrapper.monthlyWeekScheduleJson;
        Assert.assertTrue(monthlyWeekScheduleJson != null);

        return monthlyWeekScheduleJson;
    }

    @NonNull
    @Override
    public String getTaskId() {
        return getMonthlyWeekScheduleJson().getTaskId();
    }

    @Override
    public long getStartTime() {
        return getMonthlyWeekScheduleJson().getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return getMonthlyWeekScheduleJson().getEndTime();
    }

    public int getDayOfMonth() {
        return getMonthlyWeekScheduleJson().getDayOfMonth();
    }

    public int getDayOfWeek() {
        return getMonthlyWeekScheduleJson().getDayOfWeek();
    }

    public boolean getBeginningOfMonth() {
        return getMonthlyWeekScheduleJson().getBeginningOfMonth();
    }

    @Nullable
    public Integer getCustomTimeId() {
        return getMonthlyWeekScheduleJson().getCustomTimeId();
    }

    @Nullable
    public Integer getHour() {
        return getMonthlyWeekScheduleJson().getHour();
    }

    @Nullable
    public Integer getMinute() {
        return getMonthlyWeekScheduleJson().getMinute();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getMonthlyWeekScheduleJson().setEndTime(endTime);
        addValue(getId() + "/monthlyWeekScheduleJson/endTime", endTime);
    }
}
