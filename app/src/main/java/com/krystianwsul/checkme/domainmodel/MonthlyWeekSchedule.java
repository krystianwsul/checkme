package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
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

public class MonthlyWeekSchedule extends RepeatingSchedule {
    @NonNull
    private final MonthlyWeekScheduleBridge mMonthlyWeekScheduleBridge;

    public MonthlyWeekSchedule(@NonNull DomainFactory domainFactory, @NonNull MonthlyWeekScheduleBridge monthlyWeekScheduleBridge) {
        super(domainFactory);

        mMonthlyWeekScheduleBridge = monthlyWeekScheduleBridge;
    }

    @NonNull
    @Override
    protected ScheduleBridge getScheduleBridge() {
        return mMonthlyWeekScheduleBridge;
    }

    @NonNull
    @Override
    public String getScheduleText(@NonNull Context context) {
        String day = mMonthlyWeekScheduleBridge.getDayOfMonth() + " " + getDayOfWeek() + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mMonthlyWeekScheduleBridge.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        return day + ": " + getTime();
    }

    @Nullable
    @Override
    protected Instance getInstanceInDate(@NonNull Task task, @NonNull Date date, @Nullable HourMilli startHourMilli, @Nullable HourMilli endHourMilli) {
        Date dateThisMonth = getDate(date.getYear(), date.getMonth());

        if (!dateThisMonth.equals(date))
            return null;

        HourMinute hourMinute = getTime().getHourMinute(date.getDayOfWeek());

        if (startHourMilli != null && startHourMilli.compareTo(hourMinute.toHourMilli()) > 0)
            return null;

        if (endHourMilli != null && endHourMilli.compareTo(hourMinute.toHourMilli()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        return mDomainFactory.getInstance(task.getTaskKey(), scheduleDateTime);
    }

    @Nullable
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        Date today = now.getDate();

        Date dateThisMonth = getDate(today.getYear(), today.getMonth());
        Time time = getTime();
        TimeStamp thisMonth = new DateTime(dateThisMonth, time).getTimeStamp();

        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        if (thisMonth.toExactTimeStamp().compareTo(now) > 0) {
            if (endExactTimeStamp != null && endExactTimeStamp.compareTo(thisMonth.toExactTimeStamp()) <= 0)
                return null;
            else
                return thisMonth;
        } else {
            Calendar calendar = now.getCalendar();
            calendar.add(Calendar.MONTH, 1);

            Date dateNextMonth = new Date(calendar);

            TimeStamp nextMonth = new DateTime(dateNextMonth, getTime()).getTimeStamp();

            if (endExactTimeStamp != null && endExactTimeStamp.compareTo(nextMonth.toExactTimeStamp()) <= 0)
                return null;
            else
                return nextMonth;
        }
    }

    @NonNull
    private Time getTime() {
        CustomTimeKey customTimeKey = mMonthlyWeekScheduleBridge.getCustomTimeKey();
        if (customTimeKey != null) {
            return mDomainFactory.getCustomTime(customTimeKey);
        } else {
            Integer hour = mMonthlyWeekScheduleBridge.getHour();
            Integer minute = mMonthlyWeekScheduleBridge.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @NonNull
    public TimePair getTimePair() {
        CustomTimeKey customTimeKey = mMonthlyWeekScheduleBridge.getCustomTimeKey();
        Integer hour = mMonthlyWeekScheduleBridge.getHour();
        Integer minute = mMonthlyWeekScheduleBridge.getMinute();

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

    public int getDayOfMonth() {
        return mMonthlyWeekScheduleBridge.getDayOfMonth();
    }

    @NonNull
    public DayOfWeek getDayOfWeek() {
        DayOfWeek dayOfWeek = DayOfWeek.values()[mMonthlyWeekScheduleBridge.getDayOfWeek()];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    public boolean getBeginningOfMonth() {
        return mMonthlyWeekScheduleBridge.getBeginningOfMonth();
    }

    @NonNull
    private Date getDate(int year, int month) {
        return Utils.getDateInMonth(year, month, mMonthlyWeekScheduleBridge.getDayOfMonth(), getDayOfWeek(), mMonthlyWeekScheduleBridge.getBeginningOfMonth());
    }

    @Override
    public CustomTimeKey getCustomTimeKey() {
        return mMonthlyWeekScheduleBridge.getCustomTimeKey();
    }

    @NonNull
    @Override
    public CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.MonthlyWeekScheduleData(getDayOfMonth(), getDayOfWeek(), getBeginningOfMonth(), getTimePair());
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.MONTHLY_WEEK;
    }
}
