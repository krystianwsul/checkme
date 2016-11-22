package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.WeeklyScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

class RemoteWeeklyScheduleBridge implements WeeklyScheduleBridge {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteWeeklyScheduleRecord mRemoteWeeklyScheduleRecord;

    RemoteWeeklyScheduleBridge(@NonNull DomainFactory domainFactory, @NonNull RemoteWeeklyScheduleRecord weeklyScheduleRecord) {
        mDomainFactory = domainFactory;
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
    public CustomTimeKey getCustomTimeKey() {
        if (!TextUtils.isEmpty(mRemoteWeeklyScheduleRecord.getCustomTimeId()))
            return mDomainFactory.getCustomTimeKey(mRemoteWeeklyScheduleRecord.getCustomTimeId());
        else
            return null;
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
}
