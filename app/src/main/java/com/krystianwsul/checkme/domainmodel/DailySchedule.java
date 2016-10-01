package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.persistencemodel.DailyScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
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

class DailySchedule extends RepeatingSchedule {
    private final DailyScheduleRecord mDailyScheduleRecord;

    DailySchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord, @NonNull DailyScheduleRecord dailyScheduleRecord) {
        super(domainFactory, scheduleRecord);

        mDailyScheduleRecord = dailyScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        return context.getString(R.string.daily) + " " + getTime().toString();
    }

    @Override
    protected Instance getInstanceInDate(@NonNull Task task, @NonNull Date date, @Nullable HourMilli startHourMilli, @Nullable HourMilli endHourMilli) {
        DayOfWeek day = date.getDayOfWeek();

        HourMinute hourMinute = getTime().getHourMinute(day);
        Assert.assertTrue(hourMinute != null);

        if (startHourMilli != null && startHourMilli.compareTo(hourMinute.toHourMilli()) > 0)
            return null;

        if (endHourMilli != null && endHourMilli.compareTo(hourMinute.toHourMilli()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        return getDomainFactory().getInstance(task, scheduleDateTime);
    }

    @Override
    protected TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
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

    public Time getTime() {
        Integer customTimeId = mDailyScheduleRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = getDomainFactory().getCustomTime(mDailyScheduleRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Integer hour = mDailyScheduleRecord.getHour();
            Integer minute = mDailyScheduleRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @Override
    public Integer getCustomTimeId() {
        return mDailyScheduleRecord.getCustomTimeId();
    }
}
