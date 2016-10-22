package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.List;

public abstract class Schedule {
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

    public void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp));

        getScheduleBridge().setEndTime(endExactTimeStamp.getLong());
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    public ScheduleType getType() {
        return getScheduleBridge().getScheduleType();
    }

    @Nullable
    public abstract Integer getCustomTimeId();

    @NonNull
    public abstract List<MergedInstance> getInstances(@NonNull Task task, ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp);

    public abstract boolean isVisible(@NonNull Task task, @NonNull ExactTimeStamp now);

    @NonNull
    public abstract String getScheduleText(@NonNull Context context);

    @Nullable
    public abstract TimeStamp getNextAlarm(@NonNull ExactTimeStamp now);
}
