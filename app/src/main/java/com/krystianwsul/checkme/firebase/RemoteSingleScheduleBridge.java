package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.SingleScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

import java.util.Set;

public class RemoteSingleScheduleBridge implements SingleScheduleBridge {
    @NonNull
    private final RemoteSingleScheduleRecord mRemoteSingleScheduleRecord;

    public RemoteSingleScheduleBridge(@NonNull RemoteSingleScheduleRecord remoteSingleScheduleRecord) {
        mRemoteSingleScheduleRecord = remoteSingleScheduleRecord;
    }

    @Override
    public int getYear() {
        return mRemoteSingleScheduleRecord.getYear();
    }

    @Override
    public int getMonth() {
        return mRemoteSingleScheduleRecord.getMonth();
    }

    @Override
    public int getDay() {
        return mRemoteSingleScheduleRecord.getDay();
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mRemoteSingleScheduleRecord.getCustomTimeId();
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mRemoteSingleScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mRemoteSingleScheduleRecord.getMinute();
    }

    @Override
    public long getStartTime() {
        return mRemoteSingleScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mRemoteSingleScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mRemoteSingleScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mRemoteSingleScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.SINGLE;
    }

    @Override
    public void delete() {
        mRemoteSingleScheduleRecord.delete();
    }

    @Override
    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        mRemoteSingleScheduleRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
