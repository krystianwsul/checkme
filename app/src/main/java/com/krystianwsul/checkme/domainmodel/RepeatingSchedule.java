package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

abstract class RepeatingSchedule extends Schedule {
    RepeatingSchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord) {
        super(domainFactory, scheduleRecord);
    }

    @Override
    List<Instance> getInstances(Task task, ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp) {
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
            instances.add(getInstanceInDate(task, startExactTimeStamp.getDate(), startExactTimeStamp.getHourMilli(), endExactTimeStamp.getHourMilli()));
        } else {
            instances.add(getInstanceInDate(task, startExactTimeStamp.getDate(), startExactTimeStamp.getHourMilli(), null));

            Calendar loopStartCalendar = startExactTimeStamp.getDate().getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endExactTimeStamp.getDate().getCalendar();

            for (; loopStartCalendar.before(loopEndCalendar); loopStartCalendar.add(Calendar.DATE, 1))
                instances.add(getInstanceInDate(task, new Date(loopStartCalendar), null, null));

            instances.add(getInstanceInDate(task, endExactTimeStamp.getDate(), null, endExactTimeStamp.getHourMilli()));
        }

        return Stream.of(instances)
                .filter(instance -> instance != null)
                .collect(Collectors.toList());
    }

    protected abstract Instance getInstanceInDate(Task task, Date date, HourMilli startHourMilli, HourMilli endHourMilli);
}
