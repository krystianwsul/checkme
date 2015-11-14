package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklySchedule extends Schedule {
    private final WeeklyScheduleRecord mWeelyScheduleRecord;
    private final ArrayList<WeeklyScheduleDayTime> mWeeklyScheduleDayTimes = new ArrayList<>();

    WeeklySchedule(WeeklyScheduleRecord weeklyScheduleRecord, RootTask rootTask) {
        super(rootTask);

        Assert.assertTrue(weeklyScheduleRecord != null);
        mWeelyScheduleRecord = weeklyScheduleRecord;
    }

    void addWeeklyScheduleDayTime(WeeklyScheduleDayTime weeklyScheduleDayTime) {
        Assert.assertTrue(weeklyScheduleDayTime != null);
        mWeeklyScheduleDayTimes.add(weeklyScheduleDayTime);
    }

    private TimeStamp getStartTimeStamp() {
        return new TimeStamp(mWeelyScheduleRecord.getStartTime());
    }

    public TimeStamp getEndTimeStamp() {
        if (mWeelyScheduleRecord.getEndTime() == null)
            return null;
        else
            return new TimeStamp(mWeelyScheduleRecord.getEndTime());
    }

    public ArrayList<Instance> getInstances(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        TimeStamp myStartTimeStamp = getStartTimeStamp();
        TimeStamp myEndTimeStamp = getEndTimeStamp();

        ArrayList<Instance> instances = new ArrayList<>();

        TimeStamp startTimeStamp = null;
        TimeStamp endTimeStamp = null;

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

        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(startTimeStamp.compareTo(endTimeStamp) < 0);

        if (startTimeStamp.getDate().compareTo(endTimeStamp.getDate()) == 0) {
            return getInstancesInDate(startTimeStamp.getDate(), startTimeStamp.getHourMinute(), endTimeStamp.getHourMinute());
        } else {
            instances.addAll(getInstancesInDate(startTimeStamp.getDate(), startTimeStamp.getHourMinute(), null));

            Calendar loopStartCalendar = startTimeStamp.getDate().getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endTimeStamp.getDate().getCalendar();

            for (Calendar calendar = loopStartCalendar; calendar.before(loopEndCalendar); calendar.add(Calendar.DATE, 1))
                instances.addAll(getInstancesInDate(new Date(calendar), null, null));

            instances.addAll(getInstancesInDate(endTimeStamp.getDate(), null, endTimeStamp.getHourMinute()));
        }

        return instances;
    }

    private ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        for (WeeklyScheduleDayTime weeklyScheduleDayTime : mWeeklyScheduleDayTimes) {
            if (weeklyScheduleDayTime.getDayOfWeek() != day)
                continue;

            HourMinute hourMinute = weeklyScheduleDayTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) <= 0)
                continue;

            instances.add(weeklyScheduleDayTime.getInstance(mRootTask, date));
        }

        return instances;
    }

    public String getTaskText(Context context) {
        ArrayList<String> ret = new ArrayList<>();
        for (WeeklyScheduleDayTime weeklyScheduleDayTime : mWeeklyScheduleDayTimes)
            ret.add(weeklyScheduleDayTime.getDayOfWeek().toString() + ", " + weeklyScheduleDayTime.getTime().toString());
        return TextUtils.join("; ", ret);
    }
}
