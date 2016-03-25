package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMili;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

public class WeeklySchedule extends Schedule {
    private final ArrayList<WeeklyScheduleDayOfWeekTime> mWeeklyScheduleDayOfWeekTimes = new ArrayList<>();

    WeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addWeeklyScheduleDayOfWeekTime(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        mWeeklyScheduleDayOfWeekTimes.add(weeklyScheduleDayOfWeekTime);
    }

    @Override
    String getTaskText(Context context) {
        ArrayList<String> dayOfWeekTimes = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes)
            dayOfWeekTimes.add(weeklyScheduleDayOfWeekTime.getDayOfWeek().toString() + ", " + weeklyScheduleDayOfWeekTime.getTime().toString());
        return TextUtils.join("; ", dayOfWeekTimes);
    }

    @Override
    protected ArrayList<Instance> getInstancesInDate(Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes) {
            if (weeklyScheduleDayOfWeekTime.getDayOfWeek() != day)
                continue;

            HourMinute hourMinute = weeklyScheduleDayOfWeekTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
                continue;

            if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
                continue;

            instances.add(weeklyScheduleDayOfWeekTime.getInstance(rootTask, date));
        }

        return instances;
    }

    public ArrayList<Pair<DayOfWeek, Time>> getDayOfWeekTimes() {
        Assert.assertTrue(!mWeeklyScheduleDayOfWeekTimes.isEmpty());

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimes = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes)
            dayOfWeekTimes.add(new Pair<>(weeklyScheduleDayOfWeekTime.getDayOfWeek(), weeklyScheduleDayOfWeekTime.getTime()));

        return dayOfWeekTimes;
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(!mWeeklyScheduleDayOfWeekTimes.isEmpty());

        Date today = Date.today();

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        TimeStamp nextAlarm = null;
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes) {
            int ordinalDifference = (weeklyScheduleDayOfWeekTime.getDayOfWeek().ordinal() - dayOfWeek.ordinal());
            Calendar thisCalendar = today.getCalendar();
            if ((ordinalDifference > 0) || ((ordinalDifference == 0) && weeklyScheduleDayOfWeekTime.getTime().getHourMinute(dayOfWeek).compareTo(nowHourMinute) > 0))
                thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference);
            else
                thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference + 7);
            Date thisDate = new Date(thisCalendar);

            TimeStamp timeStamp = (new DateTime(thisDate, weeklyScheduleDayOfWeekTime.getTime())).getTimeStamp();
            Assert.assertTrue(timeStamp.toExactTimeStamp().compareTo(now) > 0);

            if (nextAlarm == null || timeStamp.compareTo(nextAlarm) < 0)
                nextAlarm = timeStamp;
        }

        return nextAlarm;
    }
}
