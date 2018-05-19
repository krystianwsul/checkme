package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.SingleScheduleBridge;
import com.krystianwsul.checkme.firebase.records.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.TaskKey;

class RemoteSingleScheduleBridge implements SingleScheduleBridge {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteSingleScheduleRecord mRemoteSingleScheduleRecord;

    RemoteSingleScheduleBridge(@NonNull DomainFactory domainFactory, @NonNull RemoteSingleScheduleRecord remoteSingleScheduleRecord) {
        mDomainFactory = domainFactory;
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
    public CustomTimeKey getCustomTimeKey() {
        if (!TextUtils.isEmpty(mRemoteSingleScheduleRecord.getCustomTimeId()))
            return mDomainFactory.getCustomTimeKey(mRemoteSingleScheduleRecord.getProjectId(), mRemoteSingleScheduleRecord.getCustomTimeId());
        else
            return null;
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
        return new TaskKey(mRemoteSingleScheduleRecord.getProjectId(), mRemoteSingleScheduleRecord.getTaskId());
    }

    @Override
    public void delete() {
        mRemoteSingleScheduleRecord.delete();
    }

    @Nullable
    @Override
    public kotlin.Pair<String, String> getRemoteCustomTimeKey() {
        if (TextUtils.isEmpty(mRemoteSingleScheduleRecord.getCustomTimeId())) {
            return null;
        } else {
            return new kotlin.Pair<>(mRemoteSingleScheduleRecord.getProjectId(), mRemoteSingleScheduleRecord.getCustomTimeId());
        }
    }
}
