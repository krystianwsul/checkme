package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

abstract class Schedule implements MergedSchedule {
    @NonNull
    final DomainFactory mDomainFactory;

    @NonNull
    private final ScheduleRecord mScheduleRecord;

    Schedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        mDomainFactory = domainFactory;
        mScheduleRecord = scheduleRecord;
    }

    @NonNull
    ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mScheduleRecord.getStartTime());
    }

    @Nullable
    ExactTimeStamp getEndExactTimeStamp() {
        if (mScheduleRecord.getEndTime() == null)
            return null;
        else
            return new ExactTimeStamp(mScheduleRecord.getEndTime());
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp));

        mScheduleRecord.setEndTime(endExactTimeStamp.getLong());
    }

    @Override
    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    @Override
    public ScheduleType getType() {
        ScheduleType scheduleType = ScheduleType.values()[mScheduleRecord.getType()];
        Assert.assertTrue(scheduleType != null);

        return scheduleType;
    }

    @Nullable
    public abstract Integer getCustomTimeId();
}
