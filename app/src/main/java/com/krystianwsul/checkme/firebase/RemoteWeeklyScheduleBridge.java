package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

public class RemoteWeeklyScheduleBridge implements WeeklyScheduleBridge {
    @NonNull
    private final RemoteWeeklyScheduleRecord mWeeklyScheduleRecord;

    public RemoteWeeklyScheduleBridge(@NonNull RemoteWeeklyScheduleRecord weeklyScheduleRecord) {
        mWeeklyScheduleRecord = weeklyScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mWeeklyScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mWeeklyScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mWeeklyScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mWeeklyScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.WEEKLY;
    }

    @Override
    public int getDayOfWeek() {
        return mWeeklyScheduleRecord.getDayOfWeek();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mWeeklyScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mWeeklyScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mWeeklyScheduleRecord.getMinute();
    }
}
