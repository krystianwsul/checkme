package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class SingleScheduleFactory {
    private static SingleScheduleFactory sInstance;

    public static SingleScheduleFactory getInstance() {
        if (sInstance == null)
            sInstance = new SingleScheduleFactory();
        return sInstance;
    }

    private SingleScheduleFactory() {}

    private final HashMap<Integer, SingleSchedule> mSingleSchedules = new HashMap<>();

    public SingleSchedule getSingleSchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);
        if (mSingleSchedules.containsKey(rootTask.getId()))
            return mSingleSchedules.get(rootTask.getId());
        else
            return createSingleSchedule(rootTask);
    }

    private SingleSchedule createSingleSchedule(RootTask rootTask) {
        SingleScheduleRecord singleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(rootTask.getId());
        if (singleScheduleRecord == null)
            return null;

        SingleSchedule singleSchedule = new SingleSchedule(singleScheduleRecord, rootTask);
        SingleRepetitionFactory.getInstance().loadExistingSingleRepetition(singleSchedule);

        mSingleSchedules.put(rootTask.getId(), singleSchedule);
        return singleSchedule;
    }
}
