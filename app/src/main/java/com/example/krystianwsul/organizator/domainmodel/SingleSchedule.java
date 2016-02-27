package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;

import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;

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

    @Override
    String getTaskText(Context context) {
        Assert.assertTrue(mSingleScheduleDateTime != null);
        return mSingleScheduleDateTime.getDateTime().getDisplayText(context);
    }

    @Override
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
}
