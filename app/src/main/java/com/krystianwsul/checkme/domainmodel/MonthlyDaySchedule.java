package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.Calendar;

public class MonthlyDaySchedule extends RepeatingSchedule {
    @NonNull
    private final MonthlyDayScheduleBridge mMonthlyDayScheduleBridge;

    public MonthlyDaySchedule(@NonNull DomainFactory domainFactory, @NonNull MonthlyDayScheduleBridge monthlyDayScheduleBridge) {
        super(domainFactory);

        mMonthlyDayScheduleBridge = monthlyDayScheduleBridge;
    }

    @NonNull
    @Override
    protected ScheduleBridge getScheduleBridge() {
        return mMonthlyDayScheduleBridge;
    }

    @NonNull
    @Override
    public String getScheduleText(@NonNull Context context) {
        String day = mMonthlyDayScheduleBridge.getDayOfMonth() + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mMonthlyDayScheduleBridge.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

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

        return mDomainFactory.getInstance(task, scheduleDateTime);
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

    public int getDayOfMonth() {
        return mMonthlyDayScheduleBridge.getDayOfMonth();
    }

    public boolean getBeginningOfMonth() {
        return mMonthlyDayScheduleBridge.getBeginningOfMonth();
    }

    @NonNull
    Time getTime() {
        CustomTimeKey customTimeKey = mMonthlyDayScheduleBridge.getCustomTimeKey();
        if (customTimeKey != null) {
            return mDomainFactory.getCustomTime(customTimeKey);
        } else {
            Integer hour = mMonthlyDayScheduleBridge.getHour();
            Integer minute = mMonthlyDayScheduleBridge.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @NonNull
    private Date getDate(int year, int month) {
        return Utils.getDateInMonth(year, month, mMonthlyDayScheduleBridge.getDayOfMonth(), mMonthlyDayScheduleBridge.getBeginningOfMonth());
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        return mMonthlyDayScheduleBridge.getCustomTimeKey();
    }

    @Nullable
    public HourMinute getHourMinute() {
        if (mMonthlyDayScheduleBridge.getCustomTimeKey() != null) {
            Assert.assertTrue(mMonthlyDayScheduleBridge.getHour() == null);
            Assert.assertTrue(mMonthlyDayScheduleBridge.getMinute() == null);

            return null;
        } else {
            Assert.assertTrue(mMonthlyDayScheduleBridge.getHour() != null);
            Assert.assertTrue(mMonthlyDayScheduleBridge.getMinute() != null);

            return new HourMinute(mMonthlyDayScheduleBridge.getHour(), mMonthlyDayScheduleBridge.getMinute());
        }
    }
}
