package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.SingleScheduleBridge;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;

class LocalSingleScheduleBridge extends LocalScheduleBridge implements SingleScheduleBridge {
    @NonNull
    private final SingleScheduleRecord mSingleScheduleRecord;

    LocalSingleScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull SingleScheduleRecord singleScheduleRecord) {
        super(scheduleRecord);

        mSingleScheduleRecord = singleScheduleRecord;
    }

    @Override
    public int getYear() {
        return mSingleScheduleRecord.getYear();
    }

    @Override
    public int getMonth() {
        return mSingleScheduleRecord.getMonth();
    }

    @Override
    public int getDay() {
        return mSingleScheduleRecord.getDay();
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        if (mSingleScheduleRecord.getCustomTimeId() != null)
            return new CustomTimeKey(mSingleScheduleRecord.getCustomTimeId());
        else
            return null;
    }

    @Nullable
    @Override
    public Integer getHour() {
        return mSingleScheduleRecord.getHour();
    }

    @Nullable
    @Override
    public Integer getMinute() {
        return mSingleScheduleRecord.getMinute();
    }

    @Override
    public void delete() {
        mScheduleRecord.delete();
        mSingleScheduleRecord.delete();
    }
}
