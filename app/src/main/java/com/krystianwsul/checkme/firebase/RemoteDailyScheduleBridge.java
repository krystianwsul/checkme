package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DailyScheduleBridge;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

class RemoteDailyScheduleBridge implements DailyScheduleBridge {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteDailyScheduleRecord mRemoteDailyScheduleRecord;

    RemoteDailyScheduleBridge(@NonNull DomainFactory domainFactory, @NonNull RemoteDailyScheduleRecord dailyScheduleRecord) {
        mDomainFactory = domainFactory;
        mRemoteDailyScheduleRecord = dailyScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mRemoteDailyScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mRemoteDailyScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mRemoteDailyScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mRemoteDailyScheduleRecord.getProjectId(), mRemoteDailyScheduleRecord.getTaskId());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.DAILY;
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        if (!TextUtils.isEmpty(mRemoteDailyScheduleRecord.getCustomTimeId()))
            return mDomainFactory.getCustomTimeKey(mRemoteDailyScheduleRecord.getProjectId(), mRemoteDailyScheduleRecord.getCustomTimeId());
        else
            return null;
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mRemoteDailyScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mRemoteDailyScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mRemoteDailyScheduleRecord.delete();
    }

    @Nullable
    @Override
    public Pair<String, String> getRemoteCustomTimeKey() {
        if (TextUtils.isEmpty(mRemoteDailyScheduleRecord.getCustomTimeId())) {
            return null;
        } else {
            return Pair.create(mRemoteDailyScheduleRecord.getProjectId(), mRemoteDailyScheduleRecord.getCustomTimeId());
        }
    }
}
