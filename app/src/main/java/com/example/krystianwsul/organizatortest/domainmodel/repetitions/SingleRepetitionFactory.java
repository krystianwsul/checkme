package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.tasks.SingleScheduleDateTime;

import junit.framework.Assert;

import java.util.HashMap;

public class SingleRepetitionFactory {
    private static SingleRepetitionFactory sInstance;

    public static SingleRepetitionFactory getInstance() {
        if (sInstance == null)
            sInstance = new SingleRepetitionFactory();
        return sInstance;
    }

    private SingleRepetitionFactory() {}

    private final HashMap<Integer, SingleRepetition> mSingleRepetitions = new HashMap<>();

    public void loadExistingSingleRepetition(SingleScheduleDateTime singleScheduleDateTime) {
        SingleRepetition singleRepetition = new SingleRepetition(singleScheduleDateTime);
        mSingleRepetitions.put(singleRepetition.getRootTaskId(), singleRepetition);
    }

    public SingleRepetition getSingleRepetition(SingleScheduleDateTime singleScheduleDateTime) {
        Assert.assertTrue(singleScheduleDateTime != null);

        if (mSingleRepetitions.containsKey(singleScheduleDateTime.getRootTaskId())) {
            return mSingleRepetitions.get(singleScheduleDateTime.getRootTaskId());
        } else {
            SingleRepetition singleRepetition = new SingleRepetition(singleScheduleDateTime);
            mSingleRepetitions.put(singleRepetition.getRootTaskId(), singleRepetition);
            return singleRepetition;
        }
    }
}
