package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;

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

public class WeeklySchedule extends RepeatingSchedule {
    private WeeklyScheduleDayOfWeekTime mWeeklyScheduleDayOfWeekTime;

    WeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void setWeeklyScheduleDayOfWeekTime(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        Assert.assertTrue(mWeeklyScheduleDayOfWeekTime == null);

        mWeeklyScheduleDayOfWeekTime = weeklyScheduleDayOfWeekTime;
    }

    @Override
    String getTaskText(Context context) {
        return mWeeklyScheduleDayOfWeekTime.getDayOfWeek() + ": " + mWeeklyScheduleDayOfWeekTime.getTime();

        /*
        return Stream.of(mWeeklyScheduleDayOfWeekTime)
                .groupBy(weeklyScheduleDayOfWeekTimes -> weeklyScheduleDayOfWeekTimes.getTime().toString())
                .sortBy(Map.Entry::getKey)
                .map(entry -> Stream.of(entry.getValue())
                    .map(WeeklyScheduleDayOfWeekTime::getDayOfWeek)
                    .sortBy(dayOfWeek -> dayOfWeek)
                    .map(DayOfWeek::toString)
                    .collect(Collectors.joining(", ")) + ": " + entry.getKey())
                .collect(Collectors.joining("; "));
                */
    }

    @Override
    protected ArrayList<Instance> getInstancesInDate(Task task, Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        if (mWeeklyScheduleDayOfWeekTime.getDayOfWeek() != day)
            return instances;

        HourMinute hourMinute = mWeeklyScheduleDayOfWeekTime.getTime().getHourMinute(day);
        Assert.assertTrue(hourMinute != null);

        if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
            return instances;

        if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
            return instances;

        instances.add(mWeeklyScheduleDayOfWeekTime.getInstance(task, date));

        return instances;
    }

    public Pair<DayOfWeek, Time> getDayOfWeekTimes() {
        return new Pair<>(mWeeklyScheduleDayOfWeekTime.getDayOfWeek(), mWeeklyScheduleDayOfWeekTime.getTime());
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Date today = Date.today();

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        int ordinalDifference = (mWeeklyScheduleDayOfWeekTime.getDayOfWeek().ordinal() - dayOfWeek.ordinal());
        Calendar thisCalendar = today.getCalendar();
        if ((ordinalDifference > 0) || ((ordinalDifference == 0) && mWeeklyScheduleDayOfWeekTime.getTime().getHourMinute(dayOfWeek).compareTo(nowHourMinute) > 0))
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference);
        else
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference + 7);
        Date thisDate = new Date(thisCalendar);

        return (new DateTime(thisDate, mWeeklyScheduleDayOfWeekTime.getTime())).getTimeStamp();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        Integer customTimeId = mWeeklyScheduleDayOfWeekTime.getTime().getTimePair().CustomTimeId;
        if ((customTimeId != null) && (customTime.getId() == customTimeId))
            return true;

        return false;
    }
}
