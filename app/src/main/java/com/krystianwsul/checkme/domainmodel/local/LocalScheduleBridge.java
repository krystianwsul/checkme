package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.domainmodel.ScheduleBridge;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.TaskKey;

abstract class LocalScheduleBridge implements ScheduleBridge {
    @NonNull
    final ScheduleRecord mScheduleRecord;

    LocalScheduleBridge(@NonNull ScheduleRecord scheduleRecord) {
        mScheduleRecord = scheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mScheduleRecord.getRootTaskId());
    }

    @Nullable
    @Override
    public Pair<String, String> getRemoteCustomTimeKey() {
        return null;
    }
}
