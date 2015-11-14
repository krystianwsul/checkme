package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleRepetitionRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/14/2015.
 */
public class RealSingleRepetition extends SingleRepetition {
    private final SingleRepetitionRecord mSingleRepetitionRecord;

    public RealSingleRepetition(SingleRepetitionRecord singleRepetitionRecord, SingleSchedule singleSchedule) {
        super(singleSchedule);

        Assert.assertTrue(singleRepetitionRecord != null);
        mSingleRepetitionRecord = singleRepetitionRecord;
    }

    public Date getRepetitionDate() {
        if (mSingleRepetitionRecord.getRepetitionYear() != null)
            return new Date(mSingleRepetitionRecord.getRepetitionYear(), mSingleRepetitionRecord.getRepetitionMonth(), mSingleRepetitionRecord.getRepetitionDay());
        else
            return getScheduleDate();
    }

    public Time getRepetitionTime() {
        if (mSingleRepetitionRecord.getCustomTimeId() != null)
            return CustomTime.getCustomTime(mSingleRepetitionRecord.getCustomTimeId());
        else if (mSingleRepetitionRecord.getHour() != null)
            return new NormalTime(mSingleRepetitionRecord.getHour(), mSingleRepetitionRecord.getMinute());
        else
            return getScheduleTime();
    }
}
