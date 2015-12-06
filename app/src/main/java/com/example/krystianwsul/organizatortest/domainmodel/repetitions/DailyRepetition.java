package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

public class DailyRepetition {
    private final DailyScheduleTime mDailyScheduleTime;

    private final DailyRepetitionRecord mDailyRepetitionRecord;

    private final int mId;
    private final Date mScheduleDate;

    private static int sRepetitionCount = 0;

    DailyRepetition(DailyScheduleTime dailyScheduleTime, DailyRepetitionRecord dailyRepetitionRecord) {
        Assert.assertTrue(dailyScheduleTime != null);
        Assert.assertTrue(dailyRepetitionRecord != null);

        mDailyScheduleTime = dailyScheduleTime;

        mDailyRepetitionRecord = dailyRepetitionRecord;

        mId = dailyRepetitionRecord.getId();
        mScheduleDate = new Date(dailyRepetitionRecord.getScheduleYear(), dailyRepetitionRecord.getScheduleMonth(), dailyRepetitionRecord.getScheduleDay());
    }

    DailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        Assert.assertTrue(dailyScheduleTime != null);
        Assert.assertTrue(scheduleDate != null);

        mDailyScheduleTime = dailyScheduleTime;

        mDailyRepetitionRecord = null;

        mId = PersistenceManger.getInstance().getMaxDailyRepetitionId() + ++sRepetitionCount;
        mScheduleDate = scheduleDate;
    }

    public int getId() {
        return mId;
    }

    public int getDailyScheduleTimeId() {
        return mDailyScheduleTime.getId();
    }

    public Date getScheduleDate() {
        return mScheduleDate;
    }

    public Time getScheduleTime() {
        return mDailyScheduleTime.getTime();
    }

    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    public Date getRepetitionDate() {
        if (mDailyRepetitionRecord != null && mDailyRepetitionRecord.getRepetitionYear() != null)
            return new Date(mDailyRepetitionRecord.getRepetitionYear(), mDailyRepetitionRecord.getRepetitionMonth(), mDailyRepetitionRecord.getRepetitionDay());
        else
            return getScheduleDate();
    }

    public Time getRepetitionTime() {
        if (mDailyRepetitionRecord != null) {
            if (mDailyRepetitionRecord.getCustomTimeId() != null) {
                return CustomTimeFactory.getInstance().getCustomTime(mDailyRepetitionRecord.getCustomTimeId());
            } else {
                Assert.assertTrue(mDailyRepetitionRecord.getHour() != null);
                return new NormalTime(mDailyRepetitionRecord.getHour(), mDailyRepetitionRecord.getMinute());
            }
        } else {
            return getScheduleTime();
        }
    }

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return InstanceFactory.getInstance().getDailyInstance(task, this);
    }
}
