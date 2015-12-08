package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.ScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class SingleSchedule extends Schedule {
    private SingleScheduleDateTime mSingleScheduleDateTime;

    SingleSchedule(ScheduleRecord scheduleRecord, RootTask rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addSingleScheduleDateTime(SingleScheduleDateTime singleScheduleDateTime) {
        Assert.assertTrue(singleScheduleDateTime != null);
        mSingleScheduleDateTime = singleScheduleDateTime;
    }

    SingleScheduleDateTime getSingleScheduleDateTime() {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime;
    }

    public String getTaskText(Context context) {
        return mSingleScheduleDateTime.getDateTime().getDisplayText(context);
    }

    protected ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        ArrayList<Instance> instances = new ArrayList<>();

        if (date.compareTo(mSingleScheduleDateTime.getDate()) != 0)
            return instances;

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        HourMinute hourMinute = mSingleScheduleDateTime.getTime().getHourMinute(dayOfWeek);
        Assert.assertTrue(hourMinute != null);

        if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
            return instances;

        if (endHourMinute != null && endHourMinute.compareTo(hourMinute) <= 0)
            return instances;

        instances.add(mSingleScheduleDateTime.getInstance(mRootTask, date));

        return instances;
    }
}
