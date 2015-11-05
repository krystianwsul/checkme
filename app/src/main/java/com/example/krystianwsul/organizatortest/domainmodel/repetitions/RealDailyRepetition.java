package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class RealDailyRepetition extends DailyRepetition {
    private final DailyRepetitionRecord mDailyRepetitionRecord;

    protected RealDailyRepetition(DailyRepetitionRecord dailyRepetitionRecord) {
        Assert.assertTrue(dailyRepetitionRecord != null);
        mDailyRepetitionRecord = dailyRepetitionRecord;
    }

    public  int getId() {
        return mDailyRepetitionRecord.getId();
    }

    public int getDailyScheduleTimeId() {
        return mDailyRepetitionRecord.getDailyScheduleTimeId();
    }

    public int getScheduleYear() {
        return mDailyRepetitionRecord.getScheduleYear();
    }

    public int getScheduleMonth() {
        return mDailyRepetitionRecord.getScheduleMonth();
    }

    public int getScheduleDay() {
        return mDailyRepetitionRecord.getScheduleDay();
    }

    public Integer getRepetitionYear() {
        return mDailyRepetitionRecord.getRepetitionYear();
    }

    public Integer getRepetitionMonth() {
        return mDailyRepetitionRecord.getRepetitionMonth();
    }

    public Integer getRepetitionDay() {
        return mDailyRepetitionRecord.getRepetitionDay();
    }

    private DailyScheduleTime getDailyScheduleTime() {
        return DailyScheduleTime.getDailyScheduleTime(mDailyRepetitionRecord.getDailyScheduleTimeId());
    }

    public Time getTime() {
        if (mDailyRepetitionRecord.getTimeId() != null)
            return CustomTime.getCustomTime(mDailyRepetitionRecord.getTimeId());
        else if (mDailyRepetitionRecord.getHour() != null)
                return new NormalTime(mDailyRepetitionRecord.getHour(), mDailyRepetitionRecord.getMinute());
        else
            return getDailyScheduleTime().getTime();
    }

    public Date getDate() {
        if (mDailyRepetitionRecord.getRepetitionYear() != null)
            return new Date(mDailyRepetitionRecord.getRepetitionYear(), mDailyRepetitionRecord.getRepetitionMonth(), mDailyRepetitionRecord.getRepetitionDay());
        else
            return new Date(mDailyRepetitionRecord.getScheduleYear(), mDailyRepetitionRecord.getScheduleMonth(), mDailyRepetitionRecord.getScheduleDay());
    }
}
