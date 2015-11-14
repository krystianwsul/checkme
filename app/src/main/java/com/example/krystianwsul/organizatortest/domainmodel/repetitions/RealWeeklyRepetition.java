package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleDayTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/9/2015.
 */
public class RealWeeklyRepetition extends WeeklyRepetition {
    private final WeeklyRepetitionRecord mWeeklyRepetitionRecord;

    protected RealWeeklyRepetition(WeeklyRepetitionRecord weeklyRepetitionRecord, WeeklyScheduleDayTime weeklyScheduleDayTime) {
        super(weeklyScheduleDayTime);
        Assert.assertTrue(weeklyRepetitionRecord != null);
        mWeeklyRepetitionRecord = weeklyRepetitionRecord;
    }

    public  int getId() {
        return mWeeklyRepetitionRecord.getId();
    }

    public int getWeeklyScheduleDayTimeId() {
        return mWeeklyRepetitionRecord.getWeeklyScheduleTimeId();
    }

    public Date getScheduleDate() {
        return new Date(mWeeklyRepetitionRecord.getScheduleYear(), mWeeklyRepetitionRecord.getScheduleMonth(), mWeeklyRepetitionRecord.getScheduleDay());
    }

    public Time getScheduleTime() {
        return mWeeklyScheduleDayTime.getTime();
    }

    public Date getRepetitionDate() {
        if (mWeeklyRepetitionRecord.getRepetitionYear() != null)
            return new Date(mWeeklyRepetitionRecord.getRepetitionYear(), mWeeklyRepetitionRecord.getRepetitionMonth(), mWeeklyRepetitionRecord.getRepetitionDay());
        else
            return getScheduleDate();
    }

    public Time getRepetitionTime() {
        if (mWeeklyRepetitionRecord.getCustomTimeId() != null)
            return CustomTimeFactory.getCustomTime(mWeeklyRepetitionRecord.getCustomTimeId());
        else if (mWeeklyRepetitionRecord.getHour() != null)
            return new NormalTime(mWeeklyRepetitionRecord.getHour(), mWeeklyRepetitionRecord.getMinute());
        else
            return mWeeklyScheduleDayTime.getTime();
    }
}
