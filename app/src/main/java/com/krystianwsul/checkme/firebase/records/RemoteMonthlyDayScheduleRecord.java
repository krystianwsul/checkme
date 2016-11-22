package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;

import junit.framework.Assert;

public class RemoteMonthlyDayScheduleRecord extends RemoteScheduleRecord {
    RemoteMonthlyDayScheduleRecord(@NonNull String id, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(id, remoteTaskRecord, scheduleWrapper);
    }

    RemoteMonthlyDayScheduleRecord(@NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(remoteTaskRecord, scheduleWrapper);
    }

    @NonNull
    private MonthlyDayScheduleJson getMonthlyDayScheduleJson() {
        MonthlyDayScheduleJson monthlyDayScheduleJson = mScheduleWrapper.monthlyDayScheduleJson;
        Assert.assertTrue(monthlyDayScheduleJson != null);

        return monthlyDayScheduleJson;
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
    public String getCustomTimeId() {
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

        getMonthlyDayScheduleJson().setEndTime(endTime);
        addValue(getKey() + "/monthlyDayScheduleJson/endTime", endTime);
    }
}
