package com.example.krystianwsul.organizator.domainmodel.tasks;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizator.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizator.domainmodel.times.Time;
import com.example.krystianwsul.organizator.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;

import junit.framework.Assert;

public class WeeklyScheduleDayOfWeekTime {
    private final WeeklyScheduleDayOfWeekTimeRecord mWeeklyScheduleDayOfWeekTimeRecord;
    private final WeeklySchedule mWeeklySchedule;

    WeeklyScheduleDayOfWeekTime(WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord, WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);
        Assert.assertTrue(weeklySchedule != null);

        mWeeklyScheduleDayOfWeekTimeRecord = weeklyScheduleDayOfWeekTimeRecord;
        mWeeklySchedule = weeklySchedule;
    }

    public Time getTime() {
        Integer customTimeId = mWeeklyScheduleDayOfWeekTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = CustomTimeFactory.getInstance().getCustomTime(mWeeklyScheduleDayOfWeekTimeRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mWeeklyScheduleDayOfWeekTimeRecord.getHour();
            Integer minute = mWeeklyScheduleDayOfWeekTimeRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    public int getId() {
        return mWeeklyScheduleDayOfWeekTimeRecord.getId();
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[mWeeklyScheduleDayOfWeekTimeRecord.getDayOfWeek()];
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDate != null);

        DateTime scheduleDateTime = new DateTime(scheduleDate, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp()));

        return InstanceFactory.getInstance().getInstance(task, scheduleDateTime);
    }
}
