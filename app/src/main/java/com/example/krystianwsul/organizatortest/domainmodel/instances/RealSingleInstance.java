package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/3/2015.
 */
public class RealSingleInstance extends SingleInstance {
    private final SingleInstanceRecord mSingleInstanceRecord;
    private final SingleSchedule mSingleSchedule;

    public RealSingleInstance(Task task, SingleInstanceRecord singleInstanceRecord) {
        super(task);

        Assert.assertTrue(singleInstanceRecord != null);
        mSingleInstanceRecord = singleInstanceRecord;

        mSingleSchedule = SingleSchedule.getSingleSchedule(mSingleInstanceRecord.getSingleScheduleId());
        Assert.assertTrue(mSingleSchedule != null);
    }

    public int getId() {
        return mSingleInstanceRecord.getId();
    }

    public int getSingleScheduleId() {
        return mSingleInstanceRecord.getSingleScheduleId();
    }

    public boolean getDone() {
        return mSingleInstanceRecord.getDone();
    }

    public String getScheduleText(Context context) {
        return mSingleSchedule.getTaskText(context);
    }
}
