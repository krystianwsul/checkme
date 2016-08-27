package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.WeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.Calendar;

public class WeeklySchedule extends RepeatingSchedule {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final WeeklyScheduleRecord mWeeklyScheduleRecord;

    WeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask, DomainFactory domainFactory, WeeklyScheduleRecord weeklyScheduleRecord) {
        super(scheduleRecord, rootTask);

        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(weeklyScheduleRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mWeeklyScheduleRecord = weeklyScheduleRecord;
    }

    @Override
    String getTaskText(Context context) {
        return getDayOfWeek() + ": " + getTime();

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
    protected Instance getInstanceInDate(Task task, Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        if (getDayOfWeek() != day)
            return null;

        HourMinute hourMinute = getTime().getHourMinute(day);
        Assert.assertTrue(hourMinute != null);

        if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
            return null;

        if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getInstance(task, scheduleDateTime);
    }

    public Pair<DayOfWeek, Time> getDayOfWeekTime() {
        return new Pair<>(getDayOfWeek(), getTime());
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Date today = Date.today();

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        int ordinalDifference = (getDayOfWeek().ordinal() - dayOfWeek.ordinal());
        Calendar thisCalendar = today.getCalendar();
        if ((ordinalDifference > 0) || ((ordinalDifference == 0) && getTime().getHourMinute(dayOfWeek).compareTo(nowHourMinute) > 0))
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference);
        else
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference + 7);
        Date thisDate = new Date(thisCalendar);

        return (new DateTime(thisDate, getTime())).getTimeStamp();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        Integer customTimeId = getTime().getTimePair().CustomTimeId;
        if ((customTimeId != null) && (customTime.getId() == customTimeId))
            return true;

        return false;
    }

    Time getTime() {
        Integer customTimeId = mWeeklyScheduleRecord.getCustomTimeId();
        if (customTimeId != null) {
            DomainFactory domainFactory = mDomainFactoryReference.get();
            Assert.assertTrue(domainFactory != null);

            CustomTime customTime = domainFactory.getCustomTime(mWeeklyScheduleRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mWeeklyScheduleRecord.getHour();
            Integer minute = mWeeklyScheduleRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[mWeeklyScheduleRecord.getDayOfWeek()];
    }
}
