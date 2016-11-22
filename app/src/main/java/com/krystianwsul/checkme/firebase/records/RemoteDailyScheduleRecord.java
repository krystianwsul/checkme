package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;

import junit.framework.Assert;

public class RemoteDailyScheduleRecord extends RemoteScheduleRecord {
    RemoteDailyScheduleRecord(@NonNull String id, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(id, remoteTaskRecord, scheduleWrapper);
    }

    RemoteDailyScheduleRecord(@NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(remoteTaskRecord, scheduleWrapper);
    }

    @NonNull
    private DailyScheduleJson getDailyScheduleJson() {
        DailyScheduleJson dailyScheduleJson = mScheduleWrapper.dailyScheduleJson;
        Assert.assertTrue(dailyScheduleJson != null);

        return dailyScheduleJson;
    }

    @NonNull
    @Override
    public String getTaskId() {
        return getDailyScheduleJson().getTaskId();
    }

    @Override
    public long getStartTime() {
        return getDailyScheduleJson().getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return getDailyScheduleJson().getEndTime();
    }

    @Nullable
    public String getCustomTimeId() {
        return getDailyScheduleJson().getCustomTimeId();
    }

    @Nullable
    public Integer getHour() {
        return getDailyScheduleJson().getHour();
    }

    @Nullable
    public Integer getMinute() {
        return getDailyScheduleJson().getMinute();
    }

    public void setEndTime(long endTime) {
        Assert.assertTrue(getEndTime() == null);

        getDailyScheduleJson().setEndTime(endTime);
        addValue(getKey() + "/dailyScheduleJson/endTime", endTime);
    }
}
