package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

abstract class Schedule implements MergedSchedule {
    @NonNull
    final DomainFactory mDomainFactory;

    Schedule(@NonNull DomainFactory domainFactory) {
        mDomainFactory = domainFactory;
    }

    @NonNull
    protected abstract ScheduleBridge getScheduleBridge();

    @NonNull
    ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(getScheduleBridge().getStartTime());
    }

    @Nullable
    ExactTimeStamp getEndExactTimeStamp() {
        if (getScheduleBridge().getEndTime() == null)
            return null;
        else
            return new ExactTimeStamp(getScheduleBridge().getEndTime());
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp));

        getScheduleBridge().setEndTime(endExactTimeStamp.getLong());
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
        return getScheduleBridge().getScheduleType();
    }

    @Nullable
    public abstract Integer getCustomTimeId();
}
