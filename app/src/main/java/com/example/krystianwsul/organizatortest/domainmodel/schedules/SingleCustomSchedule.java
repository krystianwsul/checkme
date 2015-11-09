package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/28/2015.
 */
public class SingleCustomSchedule extends SingleSchedule {
    private final CustomTime mCustomTime;

    protected SingleCustomSchedule(SingleScheduleRecord singleScheduleRecord, RootTask rootTask) {
        super(singleScheduleRecord, rootTask);
        Assert.assertTrue(mSingleScheduleRecord.getTimeRecordId() != null);
        Assert.assertTrue(mSingleScheduleRecord.getHour() == null);
        Assert.assertTrue(mSingleScheduleRecord.getMinute() == null);

        mCustomTime = CustomTime.getCustomTime(mSingleScheduleRecord.getTimeRecordId());
    }

    protected Time getTime() {
        return mCustomTime;
    }
}
