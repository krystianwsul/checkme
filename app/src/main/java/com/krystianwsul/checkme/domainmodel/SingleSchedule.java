package com.krystianwsul.checkme.domainmodel;

import android.content.Context;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SingleSchedule extends Schedule {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final SingleScheduleRecord mSingleScheduleRecord;

    SingleSchedule(ScheduleRecord scheduleRecord, Task rootTask, DomainFactory domainFactory, SingleScheduleRecord singleScheduleRecord) {
        super(scheduleRecord, rootTask);

        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(singleScheduleRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mSingleScheduleRecord = singleScheduleRecord;
    }

    @Override
    String getTaskText(Context context) {
        Assert.assertTrue(mSingleScheduleRecord != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        Instance instance = domainFactory.getInstance(rootTask, getDateTime());

        return instance.getInstanceDateTime().getDisplayText(context);
    }

    Instance getInstance(Task task) {
        Assert.assertTrue(mSingleScheduleRecord != null);
        Assert.assertTrue(task != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        Instance instance = domainFactory.getInstance(task, getDateTime());
        Assert.assertTrue(instance != null);

        return instance;
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(mSingleScheduleRecord != null);
        Assert.assertTrue(now != null);

        TimeStamp timeStamp = getDateTime().getTimeStamp();
        if (timeStamp.toExactTimeStamp().compareTo(now) > 0)
            return timeStamp;
        else
            return null;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        Integer customTimeId = getTime().getTimePair().CustomTimeId;
        if ((customTimeId != null) && (customTime.getId() == customTimeId))
            return true;

        return false;
    }

    @Override
    ArrayList<Instance> getInstances(Task task, ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(givenExactEndTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();

        ExactTimeStamp singleScheduleExactTimeStamp = getDateTime().getTimeStamp().toExactTimeStamp();

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp.compareTo(singleScheduleExactTimeStamp) > 0) {
            return instances;
        }

        if (givenExactEndTimeStamp.compareTo(singleScheduleExactTimeStamp) <= 0) {
            return instances;
        }

        instances.add(getInstanceInternal(task));

        return instances;
    }

    public Time getTime() {
        Integer customTimeId = mSingleScheduleRecord.getCustomTimeId();
        if (customTimeId != null) {
            DomainFactory domainFactory = mDomainFactoryReference.get();
            Assert.assertTrue(domainFactory != null);

            CustomTime customTime = domainFactory.getCustomTime(mSingleScheduleRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mSingleScheduleRecord.getHour();
            Integer minute = mSingleScheduleRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    private Date getDate() {
        return new Date(mSingleScheduleRecord.getYear(), mSingleScheduleRecord.getMonth(), mSingleScheduleRecord.getDay());
    }

    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    private Instance getInstanceInternal(Task task) {
        Assert.assertTrue(task != null);

        DateTime scheduleDateTime = getDateTime();
        //Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp())); zone hack

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getInstance(task, scheduleDateTime);
    }
}
