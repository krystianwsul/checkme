package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/31/2015.
 */
public class WeeklyInstance implements Instance {
    private final WeeklyInstanceRecord mWeeklyInstanceRecord;

    private static final HashMap<Integer, WeeklyInstance> sWeeklyInstances = new HashMap<>();

    public static WeeklyInstance getWeeklyInstance(int weeklyInstanceId) {
        if (sWeeklyInstances.containsKey(weeklyInstanceId)) {
            return sWeeklyInstances.get(weeklyInstanceId);
        } else {
            WeeklyInstance weeklyInstance = new WeeklyInstance(weeklyInstanceId);
            sWeeklyInstances.put(weeklyInstanceId, weeklyInstance);
            return weeklyInstance;
        }
    }

    private WeeklyInstance(int weeklyInstanceId) {
        mWeeklyInstanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(weeklyInstanceId);
        Assert.assertTrue(mWeeklyInstanceRecord != null);
    }
}
