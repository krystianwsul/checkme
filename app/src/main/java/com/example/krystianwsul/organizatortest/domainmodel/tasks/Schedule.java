package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.ScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

public abstract class Schedule {
    protected final ScheduleRecord mScheduleRecord;
    protected final Task mRootTask;

    public abstract String getTaskText(Context context);

    protected Schedule(ScheduleRecord scheduleRecord, Task rootTask) {
        Assert.assertTrue(scheduleRecord != null);
        Assert.assertTrue(rootTask != null);

        mScheduleRecord = scheduleRecord;
        mRootTask = rootTask;
    }

    public int getId() {
        return mScheduleRecord.getId();
    }

    public int getRootTaskId() {
        return mRootTask.getId();
    }

    public TimeStamp getStartTimeStamp() {
        return new TimeStamp(mScheduleRecord.getStartTime());
    }

    public TimeStamp getEndTimeStamp()
    {
        if (mScheduleRecord.getEndTime() == null)
            return null;
        else
            return new TimeStamp(mScheduleRecord.getEndTime());
    }

    public void setEndTimeStamp(TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);
        mScheduleRecord.setEndTime(endTimeStamp.getLong());
    }

    public boolean current(TimeStamp timeStamp) {
        TimeStamp startTimeStamp = getStartTimeStamp();
        TimeStamp endTimeStamp = getEndTimeStamp();

        return (startTimeStamp.compareTo(timeStamp) <= 0 && (endTimeStamp == null || endTimeStamp.compareTo(timeStamp) > 0));
    }

    public enum ScheduleType {
        SINGLE,
        DAILY,
        WEEKLY
    }

    public ArrayList<Instance> getInstances(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        TimeStamp myStartTimeStamp = getStartTimeStamp();
        TimeStamp myEndTimeStamp = getEndTimeStamp();

        ArrayList<Instance> instances = new ArrayList<>();

        TimeStamp startTimeStamp;
        TimeStamp endTimeStamp;

        if (givenStartTimeStamp == null || (givenStartTimeStamp.compareTo(myStartTimeStamp) < 0))
            startTimeStamp = myStartTimeStamp;
        else
            startTimeStamp = givenStartTimeStamp;

        if (myEndTimeStamp == null || (myEndTimeStamp.compareTo(givenEndTimeStamp) > 0))
            endTimeStamp = givenEndTimeStamp;
        else
            endTimeStamp = myEndTimeStamp;

        if (startTimeStamp.compareTo(endTimeStamp) >= 0)
            return instances;

        Assert.assertTrue(startTimeStamp.compareTo(endTimeStamp) < 0);

        if (startTimeStamp.getDate().equals(endTimeStamp.getDate())) {
            return getInstancesInDate(startTimeStamp.getDate(), startTimeStamp.getHourMinute(), endTimeStamp.getHourMinute());
        } else {
            instances.addAll(getInstancesInDate(startTimeStamp.getDate(), startTimeStamp.getHourMinute(), null));

            Calendar loopStartCalendar = startTimeStamp.getDate().getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endTimeStamp.getDate().getCalendar();

            for (; loopStartCalendar.before(loopEndCalendar); loopStartCalendar.add(Calendar.DATE, 1))
                instances.addAll(getInstancesInDate(new Date(loopStartCalendar), null, null));

            instances.addAll(getInstancesInDate(endTimeStamp.getDate(), null, endTimeStamp.getHourMinute()));
        }

        return instances;
    }

    protected abstract ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute);
}
