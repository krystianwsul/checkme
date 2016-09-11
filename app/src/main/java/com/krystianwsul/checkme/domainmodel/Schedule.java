package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.List;

public abstract class Schedule {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final ScheduleRecord mScheduleRecord;

    abstract String getTaskText(Context context);

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

    ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mScheduleRecord.getStartTime());
    }

    ExactTimeStamp getEndExactTimeStamp() {
        if (mScheduleRecord.getEndTime() == null)
            return null;
        else
            return new ExactTimeStamp(mScheduleRecord.getEndTime());
    }

    void setEndExactTimeStamp(ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(endExactTimeStamp != null);
        mScheduleRecord.setEndTime(endExactTimeStamp.getLong());
    }

    public boolean current(ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    public ScheduleType getType() {
        return ScheduleType.values()[mScheduleRecord.getType()];
    }

    abstract List<Instance> getInstances(Task task, ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp);

    protected abstract TimeStamp getNextAlarm(ExactTimeStamp now);

    public abstract Integer getCustomTimeId();
}
