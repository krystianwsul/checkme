package com.krystianwsul.checkme.domainmodel;

import android.content.Context;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DailySchedule extends RepeatingSchedule {
    private final ArrayList<DailyScheduleTime> mDailyScheduleTimes = new ArrayList<>();

    DailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTimes.add(dailyScheduleTime);
    }

    @Override
    String getTaskText(Context context) {
        return context.getString(R.string.daily) + " " + Stream.of(mDailyScheduleTimes)
                .map(DailyScheduleTime::getTime)
                .map(Time::toString)
                .collect(Collectors.joining(", "));
    }

    @Override
    protected ArrayList<Instance> getInstancesInDate(Task task, Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute hourMinute = dailyScheduleTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
                continue;

            if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
                continue;

            instances.add(dailyScheduleTime.getInstance(task, date));
        }

        return instances;
    }

    public List<Time> getTimes() {
        Assert.assertTrue(!mDailyScheduleTimes.isEmpty());

        return Stream.of(mDailyScheduleTimes)
                .map(DailyScheduleTime::getTime)
                .collect(Collectors.toList());
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(!mDailyScheduleTimes.isEmpty());

        Date today = Date.today();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = new Date(calendar);

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        TimeStamp nextAlarm = null;
        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute dailyScheduleHourMinute = dailyScheduleTime.getTime().getHourMinute(dayOfWeek);
            DateTime dailyScheduleDateTime;
            if (dailyScheduleHourMinute.compareTo(nowHourMinute) > 0)
                dailyScheduleDateTime = new DateTime(today, dailyScheduleTime.getTime());
            else
                dailyScheduleDateTime = new DateTime(tomorrow, dailyScheduleTime.getTime());
            if (nextAlarm == null || dailyScheduleDateTime.getTimeStamp().compareTo(nextAlarm) < 0)
                nextAlarm = dailyScheduleDateTime.getTimeStamp();
        }

        return nextAlarm;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        return Stream.of(mDailyScheduleTimes).anyMatch(dailyScheduleTime -> {
            Integer customTimeId = dailyScheduleTime.getTime().getTimePair().CustomTimeId;
            if ((customTimeId != null) && (customTime.getId() == customTimeId))
                return true;

            return false;
        });
    }
}
