package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MonthlyDayScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;

import java.util.Set;

class RemoteMonthlyDayScheduleBridge implements MonthlyDayScheduleBridge {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteMonthlyDayScheduleRecord mRemoteMonthlyDayScheduleRecord;

    RemoteMonthlyDayScheduleBridge(@NonNull DomainFactory domainFactory, @NonNull RemoteMonthlyDayScheduleRecord monthlyDayScheduleRecord) {
        mDomainFactory = domainFactory;
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
    public CustomTimeKey getCustomTimeKey() {
        if (!TextUtils.isEmpty(mRemoteMonthlyDayScheduleRecord.getCustomTimeId()))
            return mDomainFactory.getCustomTimeKey(mRemoteMonthlyDayScheduleRecord.getCustomTimeId());
        else
            return null;
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

        CustomTimeKey customTimeKey = getCustomTimeKey();
        if (customTimeKey != null)
            mDomainFactory.getCustomTime(customTimeKey).updateRecordOf(addedFriends, removedFriends);
    }
}
