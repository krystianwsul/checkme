package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MonthlyWeekScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.TaskKey;

class RemoteMonthlyWeekScheduleBridge implements MonthlyWeekScheduleBridge {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteMonthlyWeekScheduleRecord mRemoteMonthlyWeekScheduleRecord;

    RemoteMonthlyWeekScheduleBridge(@NonNull DomainFactory domainFactory, @NonNull RemoteMonthlyWeekScheduleRecord remoteMonthlyWeekScheduleRecord) {
        mDomainFactory = domainFactory;
        mRemoteMonthlyWeekScheduleRecord = remoteMonthlyWeekScheduleRecord;
    }

    @Override
    public long getStartTime() {
        return mRemoteMonthlyWeekScheduleRecord.getStartTime();
    }

    @Nullable
    @Override
    public Long getEndTime() {
        return mRemoteMonthlyWeekScheduleRecord.getEndTime();
    }

    @Override
    public void setEndTime(long endTime) {
        mRemoteMonthlyWeekScheduleRecord.setEndTime(endTime);
    }

    @NonNull
    @Override
    public TaskKey getRootTaskKey() {
        return new TaskKey(mRemoteMonthlyWeekScheduleRecord.getProjectId(), mRemoteMonthlyWeekScheduleRecord.getTaskId());
    }

    @Override
    public int getDayOfMonth() {
        return mRemoteMonthlyWeekScheduleRecord.getDayOfMonth();
    }

    @Override
    public int getDayOfWeek() {
        return mRemoteMonthlyWeekScheduleRecord.getDayOfWeek();
    }

    @Override
    public boolean getBeginningOfMonth() {
        return mRemoteMonthlyWeekScheduleRecord.getBeginningOfMonth();
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        if (!TextUtils.isEmpty(mRemoteMonthlyWeekScheduleRecord.getCustomTimeId()))
            return mDomainFactory.getCustomTimeKey(mRemoteMonthlyWeekScheduleRecord.getProjectId(), mRemoteMonthlyWeekScheduleRecord.getCustomTimeId());
        else
            return null;
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mRemoteMonthlyWeekScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mRemoteMonthlyWeekScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mRemoteMonthlyWeekScheduleRecord.delete();
    }

    @Nullable
    @Override
    public Pair<String, String> getRemoteCustomTimeKey() {
        if (TextUtils.isEmpty(mRemoteMonthlyWeekScheduleRecord.getCustomTimeId())) {
            return null;
        } else {
            return Pair.create(mRemoteMonthlyWeekScheduleRecord.getProjectId(), mRemoteMonthlyWeekScheduleRecord.getCustomTimeId());
        }
    }
}
