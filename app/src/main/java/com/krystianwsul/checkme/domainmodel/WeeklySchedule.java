package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.Calendar;

public class WeeklySchedule extends RepeatingSchedule {
    @NonNull
    private final WeeklyScheduleBridge mWeeklyScheduleBridge;

    public WeeklySchedule(@NonNull DomainFactory domainFactory, @NonNull WeeklyScheduleBridge weeklyScheduleBridge) {
        super(domainFactory);

        mWeeklyScheduleBridge = weeklyScheduleBridge;
    }

    @NonNull
    @Override
    protected ScheduleBridge getScheduleBridge() {
        return mWeeklyScheduleBridge;
    }

    @NonNull
    @Override
    public String getScheduleText(@NonNull Context context) {
        return getDayOfWeek() + ": " + getTime();
    }

    @Nullable
    @Override
    protected Instance getInstanceInDate(@NonNull Task task, @NonNull Date date, @Nullable HourMilli startHourMilli, @Nullable HourMilli endHourMilli) {
        DayOfWeek day = date.getDayOfWeek();

        if (getDayOfWeek() != day)
            return null;

        HourMinute hourMinute = getTime().getHourMinute(day);

        if (startHourMilli != null && startHourMilli.compareTo(hourMinute.toHourMilli()) > 0)
            return null;

        if (endHourMilli != null && endHourMilli.compareTo(hourMinute.toHourMilli()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        return mDomainFactory.getInstance(task.getTaskKey(), scheduleDateTime);
    }

    @NonNull
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
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

    @NonNull
    private Time getTime() {
        CustomTimeKey customTimeKey = mWeeklyScheduleBridge.getCustomTimeKey();
        if (customTimeKey != null) {
            return mDomainFactory.getCustomTime(customTimeKey);
        } else {
            Integer hour = mWeeklyScheduleBridge.getHour();
            Integer minute = mWeeklyScheduleBridge.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @NonNull
    public TimePair getTimePair() {
        CustomTimeKey customTimeKey = mWeeklyScheduleBridge.getCustomTimeKey();
        Integer hour = mWeeklyScheduleBridge.getHour();
        Integer minute = mWeeklyScheduleBridge.getMinute();

        if (customTimeKey != null) {
            Assert.assertTrue(hour == null);
            Assert.assertTrue(minute == null);

            return new TimePair(customTimeKey);
        } else {
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);

            return new TimePair(new HourMinute(hour, minute));
        }
    }

    @NonNull
    public DayOfWeek getDayOfWeek() {
        DayOfWeek dayOfWeek = DayOfWeek.values()[mWeeklyScheduleBridge.getDayOfWeek()];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    @Override
    public CustomTimeKey getCustomTimeKey() {
        return mWeeklyScheduleBridge.getCustomTimeKey();
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.WEEKLY;
    }
}
