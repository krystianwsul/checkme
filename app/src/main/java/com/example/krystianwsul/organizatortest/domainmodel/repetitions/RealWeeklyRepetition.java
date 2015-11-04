package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/1/2015.
 */
public class RealWeeklyRepetition extends WeeklyRepetition {
    private final WeeklyRepetitionRecord mWeeklyRepetitionRecord;

    protected RealWeeklyRepetition(WeeklyRepetitionRecord weeklyRepetitionRecord) {
        Assert.assertTrue(weeklyRepetitionRecord != null);
        mWeeklyRepetitionRecord = weeklyRepetitionRecord;
    }

    public  int getId() {
        return mWeeklyRepetitionRecord.getId();
    }

    public int getWeeklyScheduleTimeId() {
        return mWeeklyRepetitionRecord.getWeeklyScheduleTimeId();
    }

    public int getScheduleYear() {
        return mWeeklyRepetitionRecord.getScheduleYear();
    }

    public int getScheduleMonth() {
        return mWeeklyRepetitionRecord.getScheduleMonth();
    }

    public int getScheduleDay() {
        return mWeeklyRepetitionRecord.getScheduleDay();
    }

    public Integer getRepetitionYear() {
        return mWeeklyRepetitionRecord.getRepetitionYear();
    }

    public Integer getRepetitionMonth() {
        return mWeeklyRepetitionRecord.getRepetitionMonth();
    }

    public Integer getRepetitionDay() {
        return mWeeklyRepetitionRecord.getRepetitionDay();
    }

    private WeeklyScheduleTime getWeeklyScheduleTime() {
        return WeeklyScheduleTime.getWeeklyScheduleTime(mWeeklyRepetitionRecord.getWeeklyScheduleTimeId());
    }

    public Time getTime() {
        if (mWeeklyRepetitionRecord.getTimeId() != null)
            return CustomTime.getCustomTime(mWeeklyRepetitionRecord.getTimeId());
        else if (mWeeklyRepetitionRecord.getHour() != null)
                return new NormalTime(mWeeklyRepetitionRecord.getHour(), mWeeklyRepetitionRecord.getMinute());
        else
            return getWeeklyScheduleTime().getTime();
    }

    public Date getDate() {
        if (mWeeklyRepetitionRecord.getRepetitionYear() != null)
            return new Date(mWeeklyRepetitionRecord.getRepetitionYear(), mWeeklyRepetitionRecord.getRepetitionMonth(), mWeeklyRepetitionRecord.getRepetitionDay());
        else
            return new Date(mWeeklyRepetitionRecord.getScheduleYear(), mWeeklyRepetitionRecord.getScheduleMonth(), mWeeklyRepetitionRecord.getScheduleDay());
    }
}
