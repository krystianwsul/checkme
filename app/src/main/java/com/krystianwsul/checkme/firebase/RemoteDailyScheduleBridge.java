package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.DailyScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

import java.util.Set;

public class RemoteDailyScheduleBridge implements DailyScheduleBridge {
    @NonNull
    private final RemoteDailyScheduleRecord mDailyScheduleRecord;

    public RemoteDailyScheduleBridge(@NonNull RemoteDailyScheduleRecord dailyScheduleRecord) {
        mDailyScheduleRecord = dailyScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mDailyScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mDailyScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mDailyScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mDailyScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.DAILY;
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mDailyScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mDailyScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mDailyScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mDailyScheduleRecord.delete();
    }

    @Override
    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        mDailyScheduleRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
