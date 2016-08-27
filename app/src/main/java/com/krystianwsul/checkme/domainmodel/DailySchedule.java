package com.krystianwsul.checkme.domainmodel;

import android.content.Context;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.persistencemodel.DailyScheduleTimeRecord;
import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
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
import java.util.ArrayList;
import java.util.Calendar;

public class DailySchedule extends RepeatingSchedule {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final DailyScheduleTimeRecord mDailyScheduleTimeRecord;

    DailySchedule(ScheduleRecord scheduleRecord, Task rootTask, DomainFactory domainFactory, DailyScheduleTimeRecord dailyScheduleTimeRecord) {
        super(scheduleRecord, rootTask);

        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(dailyScheduleTimeRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mDailyScheduleTimeRecord = dailyScheduleTimeRecord;
    }

    @Override
    String getTaskText(Context context) {
        return context.getString(R.string.daily) + " " + getTime().toString();
    }

    @Override
    protected ArrayList<Instance> getInstancesInDate(Task task, Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        HourMinute hourMinute = getTime().getHourMinute(day);
        Assert.assertTrue(hourMinute != null);

        if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
            return instances;

        if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
            return instances;

        instances.add(getInstance(task, date));

        return instances;
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        Integer customTimeId = getTime().getTimePair().CustomTimeId;
        if ((customTimeId != null) && (customTime.getId() == customTimeId))
            return true;

        return false;
    }

    public Time getTime() {
        Integer customTimeId = mDailyScheduleTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            DomainFactory domainFactory = mDomainFactoryReference.get();
            Assert.assertTrue(domainFactory != null);

            CustomTime customTime = domainFactory.getCustomTime(mDailyScheduleTimeRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);

            return customTime;
        } else {
            Integer hour = mDailyScheduleTimeRecord.getHour();
            Integer minute = mDailyScheduleTimeRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getInstance(task, scheduleDateTime);
    }
}
