package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.WeeklyScheduleJson;

import junit.framework.Assert;

public class RemoteWeeklyScheduleRecord extends RemoteScheduleRecord {
    RemoteWeeklyScheduleRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteWeeklyScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private WeeklyScheduleJson getWeeklyScheduleJson() {
        WeeklyScheduleJson weeklyScheduleJson = mJsonWrapper.weeklyScheduleJson;
        Assert.assertTrue(weeklyScheduleJson != null);

        return weeklyScheduleJson;
    }

    @NonNull
    @Override
    public String getTaskId() {
        return getWeeklyScheduleJson().getTaskId();
    }

    @Override
    public long getStartTime() {
        return getWeeklyScheduleJson().getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return getWeeklyScheduleJson().getEndTime();
    }

    public int getDayOfWeek() {
        return getWeeklyScheduleJson().getDayOfWeek();
    }

    @Nullable
    public String getCustomTimeId() {
        return getWeeklyScheduleJson().getCustomTimeId();
    }

    @Nullable
    public Integer getHour() {
        return getWeeklyScheduleJson().getHour();
    }

    @Nullable
    public Integer getMinute() {
        return getWeeklyScheduleJson().getMinute();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getWeeklyScheduleJson().setEndTime(endTime);
        addValue(getId() + "/weeklyScheduleJson/endTime", endTime);
    }
}
