package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;

import junit.framework.Assert;

public class RemoteMonthlyWeekScheduleRecord extends RemoteScheduleRecord {
    RemoteMonthlyWeekScheduleRecord(@NonNull String id, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(id, remoteTaskRecord, scheduleWrapper);
    }

    RemoteMonthlyWeekScheduleRecord(@NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(remoteTaskRecord, scheduleWrapper);
    }

    @NonNull
    private MonthlyWeekScheduleJson getMonthlyWeekScheduleJson() {
        MonthlyWeekScheduleJson monthlyWeekScheduleJson = scheduleWrapper.monthlyWeekScheduleJson;
        Assert.assertTrue(monthlyWeekScheduleJson != null);

        return monthlyWeekScheduleJson;
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
    public String getCustomTimeId() {
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
        addValue(getKey() + "/monthlyWeekScheduleJson/endTime", endTime);
    }
}
