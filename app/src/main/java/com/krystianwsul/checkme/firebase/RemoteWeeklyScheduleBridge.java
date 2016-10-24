package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

import java.util.Set;

public class RemoteWeeklyScheduleBridge implements WeeklyScheduleBridge {
    @NonNull
    private final RemoteWeeklyScheduleRecord mRemoteWeeklyScheduleRecord;

    public RemoteWeeklyScheduleBridge(@NonNull RemoteWeeklyScheduleRecord weeklyScheduleRecord) {
        mRemoteWeeklyScheduleRecord = weeklyScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mRemoteWeeklyScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mRemoteWeeklyScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mRemoteWeeklyScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mRemoteWeeklyScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.WEEKLY;
    }

    @Override
    public int getDayOfWeek() {
        return mRemoteWeeklyScheduleRecord.getDayOfWeek();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mRemoteWeeklyScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mRemoteWeeklyScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mRemoteWeeklyScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mRemoteWeeklyScheduleRecord.delete();
    }

    @Override
    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        mRemoteWeeklyScheduleRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
