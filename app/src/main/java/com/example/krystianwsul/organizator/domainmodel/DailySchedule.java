package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMili;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

public class DailySchedule extends Schedule {
    private final ArrayList<DailyScheduleTime> mDailyScheduleTimes = new ArrayList<>();

    DailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTimes.add(dailyScheduleTime);
    }

    @Override
    String getTaskText(Context context) {
        ArrayList<String> times = new ArrayList<>();
        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes)
            times.add(dailyScheduleTime.getTime().toString());
        return context.getString(R.string.daily) + " " + TextUtils.join(", ", times);
    }

    @Override
    protected ArrayList<Instance> getInstancesInDate(Date date, HourMili startHourMili, HourMili endHourMili) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute hourMinute = dailyScheduleTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMili != null && startHourMili.compareTo(hourMinute.toHourMili()) > 0)
                continue;

            if (endHourMili != null && endHourMili.compareTo(hourMinute.toHourMili()) <= 0)
                continue;

            instances.add(dailyScheduleTime.getInstance(rootTask, date));
        }

        return instances;
    }

    public ArrayList<Time> getTimes() {
        Assert.assertTrue(!mDailyScheduleTimes.isEmpty());

        ArrayList<Time> times = new ArrayList<>();
        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes)
            times.add(dailyScheduleTime.getTime());

        return times;
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(!mDailyScheduleTimes.isEmpty());

        Date today = Date.today();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = new Date(calendar);

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        TimeStamp nextAlarm = null;
        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute dailyScheduleHourMinute = dailyScheduleTime.getTime().getHourMinute(dayOfWeek);
            DateTime dailyScheduleDateTime;
            if (dailyScheduleHourMinute.compareTo(nowHourMinute) > 0)
                dailyScheduleDateTime = new DateTime(today, dailyScheduleTime.getTime());
            else
                dailyScheduleDateTime = new DateTime(tomorrow, dailyScheduleTime.getTime());
            if (nextAlarm == null || dailyScheduleDateTime.getTimeStamp().compareTo(nextAlarm) < 0)
                nextAlarm = dailyScheduleDateTime.getTimeStamp();
        }

        return nextAlarm;
    }
}
