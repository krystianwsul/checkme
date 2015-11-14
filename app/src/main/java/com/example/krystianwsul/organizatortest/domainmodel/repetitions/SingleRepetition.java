package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleRepetitionRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public abstract class SingleRepetition {
    protected final SingleSchedule mSingleSchedule;

    private static final HashMap<Integer, SingleRepetition> sSingleRepetitions = new HashMap<>();

    public static SingleRepetition getSingleRepetition(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);

        if (sSingleRepetitions.containsKey(singleSchedule.getRootTaskId()))
            return sSingleRepetitions.get(singleSchedule.getRootTaskId());

        SingleRepetitionRecord singleRepetitionRecord = PersistenceManger.getInstance().getSingleRepetitionRecord(singleSchedule.getRootTaskId());
        if (singleRepetitionRecord != null) {
            RealSingleRepetition realSingleRepetition = new RealSingleRepetition(singleRepetitionRecord, singleSchedule);
            sSingleRepetitions.put(realSingleRepetition.getRootTaskId(), realSingleRepetition);
            return realSingleRepetition;
        }

        VirtualSingleRepetition virtualSingleRepetition = new VirtualSingleRepetition(singleSchedule);
        sSingleRepetitions.put(virtualSingleRepetition.getRootTaskId(), virtualSingleRepetition);
        return virtualSingleRepetition;
    }

    protected SingleRepetition(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);
        mSingleSchedule = singleSchedule;
    }

    public int getRootTaskId() {
        return mSingleSchedule.getRootTaskId();
    }

    public Date getScheduleDate() {
        return mSingleSchedule.getDate();
    }

    public Time getScheduleTime() {
        return mSingleSchedule.getTime();
    }

    public DateTime getScheduleDateTime() {
        return mSingleSchedule.getDateTime();
    }

    public abstract Date getRepetitionDate();

    public abstract Time getRepetitionTime();

    public DateTime getRepetitionDateTime() {
        return new DateTime(getRepetitionDate(), getRepetitionTime());
    }

    public Instance getInstance(Task task) {
        return SingleInstance.getSingleInstance(task, this);
    }
}
