package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/28/2015.
 */
public class SingleNormalSchedule extends SingleSchedule {
    private final NormalTime mNormalTime;

    protected SingleNormalSchedule(int singleScheduleRecordId) {
        super(singleScheduleRecordId);
        Assert.assertTrue(mSingleScheduleRecord.getTimeRecordId() == null);
        Assert.assertTrue(mSingleScheduleRecord.getHour() != null);
        Assert.assertTrue(mSingleScheduleRecord.getMinute() != null);

        mNormalTime = new NormalTime(mSingleScheduleRecord.getHour(), mSingleScheduleRecord.getMinute());
    }

    protected Time getTime() {
        return mNormalTime;
    }
}
