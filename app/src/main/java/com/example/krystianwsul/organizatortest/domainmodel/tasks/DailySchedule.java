package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.ScheduleRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class DailySchedule extends Schedule {
    private final ArrayList<DailyScheduleTime> mDailyScheduleTimes = new ArrayList<>();

    DailySchedule(ScheduleRecord scheduleRecord, RootTask rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTimes.add(dailyScheduleTime);
    }

    ArrayList<DailyScheduleTime> getDailyScheduleTimes() {
        Assert.assertTrue(!mDailyScheduleTimes.isEmpty());
        return mDailyScheduleTimes;
    }

    public String getTaskText(Context context) {
        ArrayList<String> times = new ArrayList<>();
        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes)
            times.add(dailyScheduleTime.getTime().toString());
        return context.getString(R.string.daily) + " " + TextUtils.join(", ", times);
    }

    protected ArrayList<Instance> getInstancesInDate(Date date, HourMinute startHourMinute, HourMinute endHourMinute) {
        Assert.assertTrue(date != null);

        DayOfWeek day = date.getDayOfWeek();

        ArrayList<Instance> instances = new ArrayList<>();

        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute hourMinute = dailyScheduleTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) <= 0)
                continue;

            instances.add(dailyScheduleTime.getInstance(mRootTask, date));
        }

        return instances;
    }
}
