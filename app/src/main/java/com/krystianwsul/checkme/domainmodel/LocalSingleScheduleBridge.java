package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord;

public class LocalSingleScheduleBridge extends LocalScheduleBridge implements SingleScheduleBridge {
    @NonNull
    private final SingleScheduleRecord mSingleScheduleRecord;

    public LocalSingleScheduleBridge(@NonNull ScheduleRecord scheduleRecord, @NonNull SingleScheduleRecord singleScheduleRecord) {
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
    public Integer getCustomTimeId() {
        return mSingleScheduleRecord.getCustomTimeId();
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
        mSingleScheduleRecord.delete();
    }
}
