package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;

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

    public void loadExistingSingleRepetition(SingleSchedule singleSchedule) {
        SingleRepetition singleRepetition = new SingleRepetition(singleSchedule);
        mSingleRepetitions.put(singleRepetition.getRootTaskId(), singleRepetition);
    }

    public SingleRepetition getSingleRepetition(SingleSchedule singleSchedule) {
        Assert.assertTrue(singleSchedule != null);

        if (mSingleRepetitions.containsKey(singleSchedule.getRootTaskId())) {
            return mSingleRepetitions.get(singleSchedule.getRootTaskId());
        } else {
            SingleRepetition singleRepetition = new SingleRepetition(singleSchedule);
            mSingleRepetitions.put(singleRepetition.getRootTaskId(), singleRepetition);
            return singleRepetition;
        }
    }
}
