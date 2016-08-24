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

public class DailySchedule extends RepeatingSchedule {
    private DailyScheduleTime mDailyScheduleTime;

    DailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void setDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        Assert.assertTrue(mDailyScheduleTime == null);

        mDailyScheduleTime = dailyScheduleTime;
    }

    @Override
    String getTaskText(Context context) {
        return context.getString(R.string.daily) + " " + Stream.of(mDailyScheduleTime)
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

        HourMinute hourMinute = mDailyScheduleTime.getTime().getHourMinute(day);
        Assert.assertTrue(hourMinute != null);

        if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
            return instances;

        if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
            return instances;

        instances.add(mDailyScheduleTime.getInstance(task, date));

        return instances;
    }

    public Time getTime() {
        Assert.assertTrue(mDailyScheduleTime != null);

        return mDailyScheduleTime.getTime();
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(mDailyScheduleTime != null);

        Date today = Date.today();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = new Date(calendar);

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        HourMinute dailyScheduleHourMinute = mDailyScheduleTime.getTime().getHourMinute(dayOfWeek);

        DateTime dailyScheduleDateTime;
        if (dailyScheduleHourMinute.compareTo(nowHourMinute) > 0)
            dailyScheduleDateTime = new DateTime(today, mDailyScheduleTime.getTime());
        else
            dailyScheduleDateTime = new DateTime(tomorrow, mDailyScheduleTime.getTime());

        return dailyScheduleDateTime.getTimeStamp();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        return Stream.of(mDailyScheduleTime).anyMatch(dailyScheduleTime -> {
            Integer customTimeId = dailyScheduleTime.getTime().getTimePair().CustomTimeId;
            if ((customTimeId != null) && (customTime.getId() == customTimeId))
                return true;

            return false;
        });
    }
}
