package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.WeeklyScheduleDayOfWeekTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

public class WeeklyRepetition {
    private final WeeklyScheduleDayOfWeekTime mWeeklyScheduleDayOfWeekTime;

    private final WeeklyRepetitionRecord mWeeklyRepetitionRecord;

    private final int mId;
    private final Date mScheduleDate;

    private static int sRepetitionCount = 0;

    WeeklyRepetition(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime, WeeklyRepetitionRecord weeklyRepetitionRecord) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        Assert.assertTrue(weeklyRepetitionRecord != null);

        mWeeklyScheduleDayOfWeekTime = weeklyScheduleDayOfWeekTime;

        mWeeklyRepetitionRecord = weeklyRepetitionRecord;

        mId = weeklyRepetitionRecord.getId();
        mScheduleDate = new Date(weeklyRepetitionRecord.getScheduleYear(), weeklyRepetitionRecord.getScheduleMonth(), weeklyRepetitionRecord.getScheduleDay());
    }

    WeeklyRepetition(WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime, Date scheduleDate) {
        Assert.assertTrue(weeklyScheduleDayOfWeekTime != null);
        Assert.assertTrue(scheduleDate != null);

        mWeeklyScheduleDayOfWeekTime = weeklyScheduleDayOfWeekTime;

        mWeeklyRepetitionRecord = null;

        mId = PersistenceManger.getInstance().getMaxWeeklyRepetitionId() + ++sRepetitionCount;
        mScheduleDate = scheduleDate;
    }

    public int getId() {
        return mId;
    }

    public int getWeeklyScheduleDayOfWeekTimeId() {
        return mWeeklyScheduleDayOfWeekTime.getId();
    }

    public Date getScheduleDate() {
        return mScheduleDate;
    }

    public Time getScheduleTime() {
        return mWeeklyScheduleDayOfWeekTime.getTime();
    }

    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    public Date getRepetitionDate() {
        if (mWeeklyRepetitionRecord != null && mWeeklyRepetitionRecord.getRepetitionYear() != null)
            return new Date(mWeeklyRepetitionRecord.getRepetitionYear(), mWeeklyRepetitionRecord.getRepetitionMonth(), mWeeklyRepetitionRecord.getRepetitionDay());
        else
            return getScheduleDate();
    }

    public Time getRepetitionTime() {
        if (mWeeklyRepetitionRecord != null) {
            if (mWeeklyRepetitionRecord.getCustomTimeId() != null) {
                return CustomTimeFactory.getInstance().getCustomTime(mWeeklyRepetitionRecord.getCustomTimeId());
            } else {
                Assert.assertTrue(mWeeklyRepetitionRecord.getHour() != null);
                return new NormalTime(mWeeklyRepetitionRecord.getHour(), mWeeklyRepetitionRecord.getMinute());
            }
        } else {
            return mWeeklyScheduleDayOfWeekTime.getTime();
        }
    }

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return InstanceFactory.getInstance().getWeeklyInstance(task, this);
    }
}
