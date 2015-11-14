package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleRecord;
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
public class DailySchedule extends Schedule {
    private final DailyScheduleRecord mDailyScheduleRecord;
    private final ArrayList<DailyScheduleTime> mDailyScheduleTimes = new ArrayList<>();

    private static final HashMap<Integer, DailySchedule> sDailySchedules = new HashMap<>();

    public static DailySchedule getDailySchedule(int dailyScheduleId, RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        if (sDailySchedules.containsKey(dailyScheduleId)) {
            return sDailySchedules.get(dailyScheduleId);
        } else {
            DailySchedule dailySchedule = createDailySchedule(dailyScheduleId, rootTask);
            if (dailySchedule == null)
                return null;

            sDailySchedules.put(dailyScheduleId, dailySchedule);
            return dailySchedule;
        }
    }

    private static DailySchedule createDailySchedule(int dailyScheduleId, RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        DailyScheduleRecord dailyScheduleRecord = persistenceManger.getDailyScheduleRecord(dailyScheduleId);
        if (dailyScheduleRecord == null)
            return null;

        DailySchedule dailySchedule = new DailySchedule(dailyScheduleRecord, rootTask);

        ArrayList<Integer> dailyScheduleTimeIds = persistenceManger.getDailyScheduleTimeIds(dailyScheduleId);
        Assert.assertTrue(!dailyScheduleTimeIds.isEmpty());

        for (Integer dailyScheduleTimeId : dailyScheduleTimeIds)
            dailySchedule.addDailyScheduleTime(DailyScheduleTime.getDailyScheduleTime(dailyScheduleTimeId, dailySchedule));

        return dailySchedule;
    }

    private DailySchedule(DailyScheduleRecord dailyScheduleRecord, RootTask rootTask) {
        super(rootTask);

        Assert.assertTrue(dailyScheduleRecord != null);
        mDailyScheduleRecord = dailyScheduleRecord;
    }

    private void addDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTimes.add(dailyScheduleTime);
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
