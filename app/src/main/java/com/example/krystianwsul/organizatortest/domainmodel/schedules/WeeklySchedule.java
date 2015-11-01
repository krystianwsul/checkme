package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstance;
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

    public ArrayList<Instance> getInstances(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        TimeStamp myStartTimeStamp = getStartTimeStamp();
        TimeStamp myEndTimeStamp = getEndTimeStamp();
        Assert.assertTrue(mWeeklyScheduleRecord.getEndTime() != null);

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

            Calendar loopStartCalendar = startTimeStamp.getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endTimeStamp.getCalendar();
            loopEndCalendar.add(Calendar.DATE, -1);

            for (Calendar calendar = loopStartCalendar; calendar.before(loopEndCalendar); calendar.add(Calendar.DATE, 1))
                instances.addAll(getInstancesInDate(new Date(calendar), null, null));

            instances.addAll(getInstancesInDate(endTimeStamp.getDate(), null, endTimeStamp.getHourMinute()));
        }

        return instances;
    }

    private ArrayList<Time> getTimes() {
        ArrayList<Time> times = new ArrayList<>();

        for (WeeklyScheduleTime weeklyScheduleTime : mWeeklyScheduleTimes) {
            times.add(weeklyScheduleTime.getTime());
        }

        Assert.assertTrue(!times.isEmpty());
        return times;
    }

    private ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        for (WeeklyScheduleTime weeklyScheduleTime : mWeeklyScheduleTimes) {
            HourMinute hourMinute = weeklyScheduleTime.getTime().getTimeByDay(day);
            if (hourMinute == null)
                continue;

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) < 0)
                continue;

            instances.add(weeklyScheduleTime.getInstance(date));
        }

        return instances;
    }

    public String getTaskText(Context context) {
        return context.getString(R.string.daily) + " " + TextUtils.join(", ", getTimes());
    }
}
