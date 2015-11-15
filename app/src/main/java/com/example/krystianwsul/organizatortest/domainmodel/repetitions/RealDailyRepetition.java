package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class RealDailyRepetition extends DailyRepetition {
    private final DailyRepetitionRecord mDailyRepetitionRecord;

    protected RealDailyRepetition(DailyRepetitionRecord dailyRepetitionRecord, DailyScheduleTime dailyScheduleTime) {
        super(dailyScheduleTime);
        Assert.assertTrue(dailyRepetitionRecord != null);
        mDailyRepetitionRecord = dailyRepetitionRecord;
    }

    public int getId() {
        return mDailyRepetitionRecord.getId();
    }

    public int getDailyScheduleTimeId() {
        return mDailyRepetitionRecord.getDailyScheduleTimeId();
    }

    public Date getScheduleDate() {
        return new Date(mDailyRepetitionRecord.getScheduleYear(), mDailyRepetitionRecord.getScheduleMonth(), mDailyRepetitionRecord.getScheduleDay());
    }

    public Date getRepetitionDate() {
        if (mDailyRepetitionRecord.getRepetitionYear() != null)
            return new Date(mDailyRepetitionRecord.getRepetitionYear(), mDailyRepetitionRecord.getRepetitionMonth(), mDailyRepetitionRecord.getRepetitionDay());
        else
            return getScheduleDate();
    }

    public Time getRepetitionTime() {
        if (mDailyRepetitionRecord.getCustomTimeId() != null)
            return CustomTimeFactory.getInstance().getCustomTime(mDailyRepetitionRecord.getCustomTimeId());
        else if (mDailyRepetitionRecord.getHour() != null)
            return new NormalTime(mDailyRepetitionRecord.getHour(), mDailyRepetitionRecord.getMinute());
        else
            return getScheduleTime();
    }
}
