package com.example.krystianwsul.organizator.domainmodel.tasks;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizator.domainmodel.times.Time;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class WeeklySchedule extends Schedule {
    private final ArrayList<WeeklyScheduleDayOfWeekTime> mWeeklyScheduleDayOfWeekTimes = new ArrayList<>();

    WeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addWeeklyScheduleDayOfWeekTime(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        mWeeklyScheduleDayOfWeekTimes.add(weeklyScheduleDayOfWeekTime);
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

    public ArrayList<Pair<DayOfWeek, Time>> getDayOfWeekTimes() {
        Assert.assertTrue(!mWeeklyScheduleDayOfWeekTimes.isEmpty());

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimes = new ArrayList<>();
        for (WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime : mWeeklyScheduleDayOfWeekTimes)
            dayOfWeekTimes.add(new Pair<>(weeklyScheduleDayOfWeekTime.getDayOfWeek(), weeklyScheduleDayOfWeekTime.getTime()));

        return dayOfWeekTimes;
    }
}
