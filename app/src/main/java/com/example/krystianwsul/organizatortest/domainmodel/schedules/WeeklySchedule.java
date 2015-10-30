package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Krystian on 10/17/2015.
 */
public class WeeklySchedule implements Schedule {
    private final WeeklyScheduleRecord mWeeklyScheduleRecord;
    private final ArrayList<WeeklyScheduleTime> mWeeklyScheduleTimes = new ArrayList<>();

    private static final HashMap<Integer, WeeklySchedule> sWeeklySchedules = new HashMap<>();

    public static WeeklySchedule getWeeklySchedule(int weeklyScheduleId) {
        if (sWeeklySchedules.containsKey(weeklyScheduleId)) {
            return sWeeklySchedules.get(weeklyScheduleId);
        } else {
            WeeklySchedule weeklySchedule = new WeeklySchedule(weeklyScheduleId);
            sWeeklySchedules.put(weeklyScheduleId, weeklySchedule);
            return weeklySchedule;
        }
    }

    private WeeklySchedule(int weeklyScheduleId) {
        PersistenceManger persistenceManger = PersistenceManger.getInstance();
        mWeeklyScheduleRecord = persistenceManger.getWeeklyScheduleRecord(weeklyScheduleId);
        Assert.assertTrue(mWeeklyScheduleRecord != null);

        ArrayList<Integer> weeklyScheduleTimeIds = persistenceManger.getWeeklyScheduleTimeIds(mWeeklyScheduleRecord.getId());
        Assert.assertTrue(!weeklyScheduleTimeIds.isEmpty());

        for (Integer weeklyScheduleTimeRecordId : weeklyScheduleTimeIds)
            mWeeklyScheduleTimes.add(WeeklyScheduleTime.getWeeklyScheduleTime(weeklyScheduleTimeRecordId));
    }

    private TimeStamp getStartTimeStamp() {
        return new TimeStamp(mWeeklyScheduleRecord.getStartTime());
    }

    private TimeStamp getEndTimeStamp() {
        if (mWeeklyScheduleRecord.getEndTime() == null)
            return null;
        else
            return new TimeStamp(mWeeklyScheduleRecord.getEndTime());
    }

    public ArrayList<DateTime> getDateTimes(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        TimeStamp myStartTimeStamp = getStartTimeStamp();
        TimeStamp myEndTimeStamp = getEndTimeStamp();
        Assert.assertTrue(mWeeklyScheduleRecord.getEndTime() != null);

        ArrayList<DateTime> dateTimes = new ArrayList<>();

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

    private ArrayList<Time> getTimes() {
        ArrayList<Time> times = new ArrayList<>();

        for (WeeklyScheduleTime weeklyScheduleTime : mWeeklyScheduleTimes) {
            times.add(weeklyScheduleTime.getTime());
        }

        Assert.assertTrue(!times.isEmpty());
        return times;
    }

    private ArrayList<DateTime> getTimesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<DateTime> ret = new ArrayList<>();

        for (Time time : getTimes()) {
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

    public String getTaskText(Context context) {
        return context.getString(R.string.daily) + " " + TextUtils.join(", ", getTimes());
    }
}
