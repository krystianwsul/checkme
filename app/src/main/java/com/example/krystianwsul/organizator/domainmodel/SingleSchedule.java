package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;

import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMili;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

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
        Assert.assertTrue(task != null);

        Assert.assertTrue(mSingleScheduleDateTime != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        Instance instance = domainFactory.getInstance(task, getDateTime());
        Assert.assertTrue(instance != null);

        return instance;
    }

    @Override
    protected ArrayList<Instance> getInstancesInDate(Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        Assert.assertTrue(date != null);

        ArrayList<Instance> instances = new ArrayList<>();

        if (date.compareTo(mSingleScheduleDateTime.getDate()) != 0)
            return instances;

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        HourMinute hourMinute = mSingleScheduleDateTime.getTime().getHourMinute(dayOfWeek);
        Assert.assertTrue(hourMinute != null);

        if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
            return instances;

        if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
            return instances;

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        instances.add(mSingleScheduleDateTime.getInstance(rootTask, date));

        return instances;
    }

    public DateTime getDateTime() {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getDateTime();
    }

    public Integer getCustomTimeId() {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getCustomTimeId();
    }

    public HourMinute getHourMinute() {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getHourMinute();
    }

    public TimePair getTimePair() {
        return new TimePair(getCustomTimeId(), getHourMinute());
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        TimeStamp timeStamp = getDateTime().getTimeStamp();
        if (timeStamp.toExactTimeStamp().compareTo(now) > 0)
            return timeStamp;
        else
            return null;
    }
}
