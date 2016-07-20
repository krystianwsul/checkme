package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
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
import java.util.Map;

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
        return Stream.of(mWeeklyScheduleDayOfWeekTimes)
                .groupBy(weeklyScheduleDayOfWeekTimes -> weeklyScheduleDayOfWeekTimes.getTime().toString())
                .sortBy(Map.Entry::getKey)
                .map(entry -> Stream.of(entry.getValue())
                    .map(WeeklyScheduleDayOfWeekTime::getDayOfWeek)
                    .sortBy(dayOfWeek -> dayOfWeek)
                    .map(DayOfWeek::toString)
                    .collect(Collectors.joining(", ")) + ": " + entry.getKey())
                .collect(Collectors.joining("; "));
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

    public List<Pair<DayOfWeek, Time>> getDayOfWeekTimes() {
        Assert.assertTrue(!mWeeklyScheduleDayOfWeekTimes.isEmpty());

        return Stream.of(mWeeklyScheduleDayOfWeekTimes)
                .map(weeklyScheduleDayOfWeekTime -> new Pair<>(weeklyScheduleDayOfWeekTime.getDayOfWeek(), weeklyScheduleDayOfWeekTime.getTime()))
                .collect(Collectors.toList());
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        return Stream.of(mWeeklyScheduleDayOfWeekTimes).anyMatch(dailyScheduleTime -> {
            Integer customTimeId = dailyScheduleTime.getTime().getTimePair().CustomTimeId;
            if ((customTimeId != null) && (customTime.getId() == customTimeId))
                return true;

            return false;
        });
    }
}
