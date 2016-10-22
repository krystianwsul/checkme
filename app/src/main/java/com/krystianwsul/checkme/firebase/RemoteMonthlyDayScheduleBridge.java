package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

public class RemoteMonthlyDayScheduleBridge implements MonthlyDayScheduleBridge {
    @NonNull
    private final RemoteMonthlyDayScheduleRecord mMonthlyDayScheduleRecord;

    public RemoteMonthlyDayScheduleBridge(@NonNull RemoteMonthlyDayScheduleRecord monthlyDayScheduleRecord) {
        mMonthlyDayScheduleRecord = monthlyDayScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mMonthlyDayScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mMonthlyDayScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mMonthlyDayScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mMonthlyDayScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.MONTHLY_DAY;
    }

    @Override
    public int getDayOfMonth() {
        return mMonthlyDayScheduleRecord.getDayOfMonth();
    }

    @Override
    public boolean getBeginningOfMonth() {
        return mMonthlyDayScheduleRecord.getBeginningOfMonth();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mMonthlyDayScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mMonthlyDayScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mMonthlyDayScheduleRecord.getMinute();
    }
}
