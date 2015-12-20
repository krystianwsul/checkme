package com.example.krystianwsul.organizator.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class SingleSchedule extends Schedule {
    private SingleScheduleDateTime mSingleScheduleDateTime;

    SingleSchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void setSingleScheduleDateTime(SingleScheduleDateTime singleScheduleDateTime) {
        Assert.assertTrue(singleScheduleDateTime != null);
        Assert.assertTrue(mSingleScheduleDateTime == null);

        mSingleScheduleDateTime = singleScheduleDateTime;
    }

    public String getTaskText(Context context) {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getDateTime().getDisplayText(context);
    }

    protected ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(mSingleScheduleDateTime != null);
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

    public DateTime getDateTime() {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getDateTime();
    }
}
