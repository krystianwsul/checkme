package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.Calendar;

public class DailySchedule extends RepeatingSchedule {
    @NonNull
    private final DailyScheduleBridge mDailyScheduleBridge;

    public DailySchedule(@NonNull DomainFactory domainFactory, @NonNull DailyScheduleBridge dailyScheduleBridge) {
        super(domainFactory);

        mDailyScheduleBridge = dailyScheduleBridge;
    }

    @NonNull
    @Override
    protected ScheduleBridge getScheduleBridge() {
        return mDailyScheduleBridge;
    }

    @NonNull
    @Override
    public String getScheduleText(@NonNull Context context) {
        return context.getString(R.string.daily) + " " + getTime().toString();
    }

    @Nullable
    @Override
    protected Instance getInstanceInDate(@NonNull Task task, @NonNull Date date, @Nullable HourMilli startHourMilli, @Nullable HourMilli endHourMilli) {
        DayOfWeek day = date.getDayOfWeek();

        HourMinute hourMinute = getTime().getHourMinute(day);

        if (startHourMilli != null && startHourMilli.compareTo(hourMinute.toHourMilli()) > 0)
            return null;

        if (endHourMilli != null && endHourMilli.compareTo(hourMinute.toHourMilli()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        return mDomainFactory.getInstance(task, scheduleDateTime);
    }

    @Nullable
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        Date today = Date.today();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = new Date(calendar);

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        HourMinute dailyScheduleHourMinute = getTime().getHourMinute(dayOfWeek);

        DateTime dailyScheduleDateTime;
        if (dailyScheduleHourMinute.compareTo(nowHourMinute) > 0)
            dailyScheduleDateTime = new DateTime(today, getTime());
        else
            dailyScheduleDateTime = new DateTime(tomorrow, getTime());

        return dailyScheduleDateTime.getTimeStamp();
    }

    @NonNull
    public Time getTime() {
        CustomTimeKey customTimeKey = mDailyScheduleBridge.getCustomTimeKey();
        if (customTimeKey != null) {
            return mDomainFactory.getCustomTime(mDailyScheduleBridge.getCustomTimeKey());
        } else {
            Integer hour = mDailyScheduleBridge.getHour();
            Integer minute = mDailyScheduleBridge.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @Override
    public CustomTimeKey getCustomTimeKey() {
        return mDailyScheduleBridge.getCustomTimeKey();
    }

    @Nullable
    public HourMinute getHourMinute() {
        if (mDailyScheduleBridge.getCustomTimeKey() != null) {
            Assert.assertTrue(mDailyScheduleBridge.getHour() == null);
            Assert.assertTrue(mDailyScheduleBridge.getMinute() == null);

            return null;
        } else {
            Assert.assertTrue(mDailyScheduleBridge.getHour() != null);
            Assert.assertTrue(mDailyScheduleBridge.getMinute() != null);

            return new HourMinute(mDailyScheduleBridge.getHour(), mDailyScheduleBridge.getMinute());
        }
    }

    @NonNull
    @Override
    public CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.DailyScheduleData(getTime().getTimePair());
    }
}
