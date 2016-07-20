package com.krystianwsul.checkme.domainmodel;

import android.content.Context;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

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

    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mScheduleRecord.getStartTime());
    }

    private ExactTimeStamp getEndExactTimeStamp() {
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

    ArrayList<Instance> getInstances(ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp) {
        Assert.assertTrue(givenExactEndTimeStamp != null);

        ExactTimeStamp myStartTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp myEndTimeStamp = getEndExactTimeStamp();

        ArrayList<Instance> instances = new ArrayList<>();

        ExactTimeStamp startExactTimeStamp;
        ExactTimeStamp endExactTimeStamp;

        if (givenStartExactTimeStamp == null || (givenStartExactTimeStamp.compareTo(myStartTimeStamp) < 0))
            startExactTimeStamp = myStartTimeStamp;
        else
            startExactTimeStamp = givenStartExactTimeStamp;

        if (myEndTimeStamp == null || (myEndTimeStamp.compareTo(givenExactEndTimeStamp) > 0))
            endExactTimeStamp = givenExactEndTimeStamp;
        else
            endExactTimeStamp = myEndTimeStamp;

        if (startExactTimeStamp.compareTo(endExactTimeStamp) >= 0)
            return instances;

        Assert.assertTrue(startExactTimeStamp.compareTo(endExactTimeStamp) < 0);

        if (startExactTimeStamp.getDate().equals(endExactTimeStamp.getDate())) {
            return getInstancesInDate(startExactTimeStamp.getDate(), startExactTimeStamp.getHourMili(), endExactTimeStamp.getHourMili());
        } else {
            instances.addAll(getInstancesInDate(startExactTimeStamp.getDate(), startExactTimeStamp.getHourMili(), null));

            Calendar loopStartCalendar = startExactTimeStamp.getDate().getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endExactTimeStamp.getDate().getCalendar();

            for (; loopStartCalendar.before(loopEndCalendar); loopStartCalendar.add(Calendar.DATE, 1))
                instances.addAll(getInstancesInDate(new Date(loopStartCalendar), null, null));

            instances.addAll(getInstancesInDate(endExactTimeStamp.getDate(), null, endExactTimeStamp.getHourMili()));
        }

        return instances;
    }

    protected abstract ArrayList<Instance> getInstancesInDate(Date date, HourMili startHourMili, HourMili endHourMili);

    protected abstract TimeStamp getNextAlarm(ExactTimeStamp now);

    public abstract boolean usesCustomTime(CustomTime customTime);
}
