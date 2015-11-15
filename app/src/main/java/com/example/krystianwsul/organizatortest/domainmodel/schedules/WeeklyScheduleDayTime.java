package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyScheduleDayTimeRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyScheduleDayTime {
    protected final WeeklyScheduleDayTimeRecord mWeeklyScheduleDayTimeRecord;
    protected final WeeklySchedule mWeeklySchedule;

    protected WeeklyScheduleDayTime(WeeklyScheduleDayTimeRecord weeklyScheduleDayTimeRecord, WeeklySchedule weeklySchedule) {
        Assert.assertTrue(weeklyScheduleDayTimeRecord != null);
        Assert.assertTrue(weeklySchedule != null);

        mWeeklyScheduleDayTimeRecord = weeklyScheduleDayTimeRecord;
        mWeeklySchedule = weeklySchedule;
    }

    public Time getTime() {
        Integer customTimeId = mWeeklyScheduleDayTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = CustomTimeFactory.getCustomTime(mWeeklyScheduleDayTimeRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mWeeklyScheduleDayTimeRecord.getHour();
            Integer minute = mWeeklyScheduleDayTimeRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    public int getId() {
        return mWeeklyScheduleDayTimeRecord.getId();
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[mWeeklyScheduleDayTimeRecord.getDayOfWeek()];
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        return WeeklyRepetitionFactory.getInstance().getWeeklyRepetition(this, scheduleDate).getInstance(task);
    }
}
