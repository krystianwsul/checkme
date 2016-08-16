package com.krystianwsul.checkme.domainmodel;

import android.content.Context;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.List;

public abstract class Schedule {
    private final ScheduleRecord mScheduleRecord;
    final WeakReference<Task> mRootTaskReference;

    abstract String getTaskText(Context context);

    Schedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        mScheduleRecord = scheduleRecord;
        mRootTaskReference = new WeakReference<>(rootTask);
    }

    public int getId() {
        return mScheduleRecord.getId();
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

    abstract List<Instance> getInstances(ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp);

    protected abstract TimeStamp getNextAlarm(ExactTimeStamp now);

    public abstract boolean usesCustomTime(CustomTime customTime);
}
