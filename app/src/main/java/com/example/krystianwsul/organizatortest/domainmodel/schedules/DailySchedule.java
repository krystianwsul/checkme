package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

public class DailySchedule extends Schedule {
    private final DailyScheduleRecord mDailyScheduleRecord;
    private final ArrayList<DailyScheduleTime> mDailyScheduleTimes = new ArrayList<>();

    DailySchedule(DailyScheduleRecord dailyScheduleRecord, RootTask rootTask) {
        super(rootTask);

        Assert.assertTrue(dailyScheduleRecord != null);
        mDailyScheduleRecord = dailyScheduleRecord;
    }

    void addDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTimes.add(dailyScheduleTime);
    }

    public int getId() {
        return mDailyScheduleRecord.getId();
    }

    private TimeStamp getStartTimeStamp() {
        return new TimeStamp(mDailyScheduleRecord.getStartTime());
    }

    public TimeStamp getEndTimeStamp() {
        if (mDailyScheduleRecord.getEndTime() == null)
            return null;
        else
            return new TimeStamp(mDailyScheduleRecord.getEndTime());
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

    private ArrayList<Time> getTimes() {
        ArrayList<Time> times = new ArrayList<>();

        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            times.add(dailyScheduleTime.getTime());
        }

        Assert.assertTrue(!times.isEmpty());
        return times;
    }

    private ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute hourMinute = dailyScheduleTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) <= 0)
                continue;

            instances.add(dailyScheduleTime.getInstance(mRootTask, date));
        }

        return instances;
    }

    public String getTaskText(Context context) {
        return context.getString(R.string.daily) + " " + TextUtils.join(", ", getTimes());
    }
}
