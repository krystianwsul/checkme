package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleTimeRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/29/2015.
 */
public class DailyScheduleTime {
    protected final DailyScheduleTimeRecord mDailyScheduleTimeRecord;
    protected final DailySchedule mDailySchedule;

    protected DailyScheduleTime(DailyScheduleTimeRecord dailyScheduleTimeRecord, DailySchedule dailySchedule) {
        Assert.assertTrue(dailyScheduleTimeRecord != null);
        Assert.assertTrue(dailySchedule != null);

        mDailyScheduleTimeRecord = dailyScheduleTimeRecord;
        mDailySchedule = dailySchedule;
    }

    public Time getTime() {
        Integer customTimeId = mDailyScheduleTimeRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = CustomTimeFactory.getCustomTime(mDailyScheduleTimeRecord.getCustomTimeId());
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

    public int getId() {
        return mDailyScheduleTimeRecord.getId();
    }

    public Instance getInstance(Task task, Date scheduleDate) {
        return DailyRepetitionFactory.getInstance().getDailyRepetition(this, scheduleDate).getInstance(task);
    }
}
