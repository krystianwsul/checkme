package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

import java.util.Set;

public class RemoteMonthlyDayScheduleBridge implements MonthlyDayScheduleBridge {
    @NonNull
    private final RemoteMonthlyDayScheduleRecord mRemoteMonthlyDayScheduleRecord;

    public RemoteMonthlyDayScheduleBridge(@NonNull RemoteMonthlyDayScheduleRecord monthlyDayScheduleRecord) {
        mRemoteMonthlyDayScheduleRecord = monthlyDayScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mRemoteMonthlyDayScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mRemoteMonthlyDayScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mRemoteMonthlyDayScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mRemoteMonthlyDayScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.MONTHLY_DAY;
    }

    @Override
    public int getDayOfMonth() {
        return mRemoteMonthlyDayScheduleRecord.getDayOfMonth();
    }

    @Override
    public boolean getBeginningOfMonth() {
        return mRemoteMonthlyDayScheduleRecord.getBeginningOfMonth();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mRemoteMonthlyDayScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mRemoteMonthlyDayScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mRemoteMonthlyDayScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mRemoteMonthlyDayScheduleRecord.delete();
    }

    @Override
    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        mRemoteMonthlyDayScheduleRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
