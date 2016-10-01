package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.List;

abstract class Schedule {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final ScheduleRecord mScheduleRecord;

    @NonNull
    abstract String getScheduleText(@NonNull Context context);

    Schedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mScheduleRecord = scheduleRecord;
    }

    @NonNull
    DomainFactory getDomainFactory() {
        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory;
    }

    @NonNull
    Task getRootTask() {
        return getDomainFactory().getTask(mScheduleRecord.getRootTaskId());
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

    void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        mScheduleRecord.setEndTime(endExactTimeStamp.getLong());
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    ScheduleType getType() {
        ScheduleType scheduleType = ScheduleType.values()[mScheduleRecord.getType()];
        Assert.assertTrue(scheduleType != null);

        return scheduleType;
    }

    @NonNull
    abstract List<Instance> getInstances(@NonNull Task task, @Nullable ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp);

    @Nullable
    protected abstract TimeStamp getNextAlarm(@NonNull ExactTimeStamp now);

    @Nullable
    public abstract Integer getCustomTimeId();
}
