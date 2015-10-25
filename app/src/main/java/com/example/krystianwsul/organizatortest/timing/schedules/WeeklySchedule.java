package com.example.krystianwsul.organizatortest.timing.schedules;

import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.Date;
import com.example.krystianwsul.organizatortest.timing.DayOfWeek;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;
import com.example.krystianwsul.organizatortest.timing.times.HourMinute;
import com.example.krystianwsul.organizatortest.timing.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Krystian on 10/17/2015.
 */
public class WeeklySchedule implements Schedule {
    private TimeStamp mStartTimeStamp;
    private TimeStamp mEndTimeStamp = null;

    private ArrayList<Time> mTimes;

    public WeeklySchedule(TimeStamp startTimeStamp, TimeStamp endTimeStamp, ArrayList<Time> times) {
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(endTimeStamp == null || startTimeStamp.compareTo(endTimeStamp) < 0);
        Assert.assertTrue(times != null);
        Assert.assertTrue(!times.isEmpty());

        mStartTimeStamp = startTimeStamp;
        mEndTimeStamp = endTimeStamp;
        mTimes = times;
    }

    public WeeklySchedule(TimeStamp startTimeStamp, TimeStamp endTimeStamp, Time time) {
        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(endTimeStamp == null || startTimeStamp.compareTo(endTimeStamp) < 0);
        Assert.assertTrue(time != null);

        mStartTimeStamp = startTimeStamp;
        mEndTimeStamp = endTimeStamp;
        mTimes = new ArrayList<>();
        mTimes.add(time);
    }

    public ArrayList<DateTime> getDateTimes(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);
        Assert.assertTrue(mStartTimeStamp != null);

        ArrayList<DateTime> dateTimes = new ArrayList<>();

        TimeStamp startTimeStamp = null;
        TimeStamp endTimeStamp = null;

        if (givenStartTimeStamp == null || (givenStartTimeStamp.compareTo(mStartTimeStamp) < 0))
            startTimeStamp = mStartTimeStamp;
        else
            startTimeStamp = givenStartTimeStamp;

        if (mEndTimeStamp == null || (mEndTimeStamp.compareTo(givenEndTimeStamp) > 0))
            endTimeStamp = givenEndTimeStamp;
        else
            endTimeStamp = mEndTimeStamp;

        if (startTimeStamp.compareTo(endTimeStamp) >= 0)
            return dateTimes;

        Assert.assertTrue(startTimeStamp != null);
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(startTimeStamp.compareTo(endTimeStamp) < 0);

        if (startTimeStamp.getDate().compareTo(endTimeStamp.getDate()) == 0) {
            return getTimesInDate(startTimeStamp.getDate(), startTimeStamp.getHourMinute(), endTimeStamp.getHourMinute());
        } else {
            dateTimes.addAll(getTimesInDate(startTimeStamp.getDate(), startTimeStamp.getHourMinute(), null));

            Calendar loopStartCalendar = startTimeStamp.getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endTimeStamp.getCalendar();
            loopEndCalendar.add(Calendar.DATE, -1);

            for (Calendar calendar = loopStartCalendar; calendar.before(loopEndCalendar); calendar.add(Calendar.DATE, 1))
                dateTimes.addAll(getTimesInDate(new Date(calendar), null, null));

            dateTimes.addAll(getTimesInDate(endTimeStamp.getDate(), null, endTimeStamp.getHourMinute()));
        }

        return dateTimes;
    }

    private ArrayList<DateTime> getTimesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<DateTime> ret = new ArrayList<>();

        for (Time time : mTimes) {
            HourMinute hourMinute = time.getTimeByDay(day);
            if (hourMinute == null)
                continue;

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) < 0)
                continue;

            ret.add(new DateTime(date, time));
        }

        return ret;
    }
}
