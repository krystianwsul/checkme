package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.ScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class WeeklySchedule extends Schedule {
    private final ArrayList<WeeklyScheduleDayOfWeekTime> mWeeklyScheduleDayOfWeekTimes = new ArrayList<>();

    WeeklySchedule(ScheduleRecord scheduleRecord, RootTask rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addWeeklyScheduleDayOfWeekTime(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        mWeeklyScheduleDayOfWeekTimes.add(weeklyScheduleDayOfWeekTime);
    }

    ArrayList<WeeklyScheduleDayOfWeekTime> getWeeklyScheduleDayOfWeekTimes() {
        Assert.assertTrue(!mWeeklyScheduleDayOfWeekTimes.isEmpty());
        return mWeeklyScheduleDayOfWeekTimes;
    }

    public String getTaskText(Context context) {
        ArrayList<String> dayOfWeekTimes = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes)
            dayOfWeekTimes.add(weeklyScheduleDayOfWeekTime.getDayOfWeek().toString() + ", " + weeklyScheduleDayOfWeekTime.getTime().toString());
        return TextUtils.join("; ", dayOfWeekTimes);
    }

    protected ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes) {
            if (weeklyScheduleDayOfWeekTime.getDayOfWeek() != day)
                continue;

            HourMinute hourMinute = weeklyScheduleDayOfWeekTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) <= 0)
                continue;

            instances.add(weeklyScheduleDayOfWeekTime.getInstance(mRootTask, date));
        }

        return instances;
    }
}
