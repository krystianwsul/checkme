package com.krystianwsul.checkme.domainmodel;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

public abstract class RepeatingSchedule extends Schedule {
    RepeatingSchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    @Override
    ArrayList<Instance> getInstances(Task task, ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp) {
        Assert.assertTrue(task != null);
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
            return getInstancesInDate(task, startExactTimeStamp.getDate(), startExactTimeStamp.getHourMili(), endExactTimeStamp.getHourMili());
        } else {
            instances.addAll(getInstancesInDate(task, startExactTimeStamp.getDate(), startExactTimeStamp.getHourMili(), null));

            Calendar loopStartCalendar = startExactTimeStamp.getDate().getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endExactTimeStamp.getDate().getCalendar();

            for (; loopStartCalendar.before(loopEndCalendar); loopStartCalendar.add(Calendar.DATE, 1))
                instances.addAll(getInstancesInDate(task, new Date(loopStartCalendar), null, null));

            instances.addAll(getInstancesInDate(task, endExactTimeStamp.getDate(), null, endExactTimeStamp.getHourMili()));
        }

        return instances;
    }

    protected abstract ArrayList<Instance> getInstancesInDate(Task task, Date date, HourMili startHourMili, HourMili endHourMili);
}
