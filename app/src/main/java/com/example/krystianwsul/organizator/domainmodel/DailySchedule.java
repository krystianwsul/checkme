package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

import java.util.ArrayList;

public class DailySchedule extends Schedule {
    private final ArrayList<DailyScheduleTime> mDailyScheduleTimes = new ArrayList<>();

    DailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
        super(scheduleRecord, rootTask);
    }

    void addDailyScheduleTime(DailyScheduleTime dailyScheduleTime) {
        Assert.assertTrue(dailyScheduleTime != null);
        mDailyScheduleTimes.add(dailyScheduleTime);
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

        Task rootTask = mRootTaskReference.get();
        Assert.assertTrue(rootTask != null);

        for (DailyScheduleTime dailyScheduleTime : mDailyScheduleTimes) {
            HourMinute hourMinute = dailyScheduleTime.getTime().getHourMinute(day);
            Assert.assertTrue(hourMinute != null);

            if (startHourMinute != null && startHourMinute.compareTo(hourMinute) > 0)
                continue;

            if (endHourMinute != null && endHourMinute.compareTo(hourMinute) <= 0)
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
}
