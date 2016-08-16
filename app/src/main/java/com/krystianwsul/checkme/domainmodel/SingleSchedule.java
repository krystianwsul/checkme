package com.krystianwsul.checkme.domainmodel;

import android.content.Context;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SingleSchedule extends Schedule {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private SingleScheduleDateTime mSingleScheduleDateTime;

    SingleSchedule(ScheduleRecord scheduleRecord, Task rootTask, DomainFactory domainFactory) {
        super(scheduleRecord, rootTask);

        Assert.assertTrue(domainFactory != null);
        mDomainFactoryReference = new WeakReference<>(domainFactory);
    }

    void setSingleScheduleDateTime(SingleScheduleDateTime singleScheduleDateTime) {
        Assert.assertTrue(singleScheduleDateTime != null);
        Assert.assertTrue(mSingleScheduleDateTime == null);

        mSingleScheduleDateTime = singleScheduleDateTime;
    }

    @Override
    String getTaskText(Context context) {
        Assert.assertTrue(mSingleScheduleDateTime != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        Instance instance = domainFactory.getInstance(rootTask, mSingleScheduleDateTime.getDateTime());

        return instance.getInstanceDateTime().getDisplayText(context);
    }

    Instance getInstance(Task task) {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        Assert.assertTrue(task != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        Instance instance = domainFactory.getInstance(task, mSingleScheduleDateTime.getDateTime());
        Assert.assertTrue(instance != null);

        return instance;
    }

    public Time getTime() {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getTime();
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        Assert.assertTrue(now != null);

        TimeStamp timeStamp = mSingleScheduleDateTime.getDateTime().getTimeStamp();
        if (timeStamp.toExactTimeStamp().compareTo(now) > 0)
            return timeStamp;
        else
            return null;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        Integer customTimeId = mSingleScheduleDateTime.getTime().getTimePair().CustomTimeId;
        if ((customTimeId != null) && (customTime.getId() == customTimeId))
            return true;

        return false;
    }

    @Override
    ArrayList<Instance> getInstances(ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp) {
        Assert.assertTrue(givenExactEndTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();

        ExactTimeStamp singleScheduleExactTimeStamp = mSingleScheduleDateTime.getDateTime().getTimeStamp().toExactTimeStamp();

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp.compareTo(singleScheduleExactTimeStamp) > 0)
            return instances;

        if (givenExactEndTimeStamp.compareTo(singleScheduleExactTimeStamp) <= 0)
            return instances;

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        instances.add(mSingleScheduleDateTime.getInstance(rootTask));

        return instances;
    }
}
