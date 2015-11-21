package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.DailyRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class DailyScheduleFactory {
    private static DailyScheduleFactory sInstance;

    public static DailyScheduleFactory getInstance() {
        if (sInstance == null)
            sInstance = new DailyScheduleFactory();
        return sInstance;
    }

    private DailyScheduleFactory() {}

    private final HashMap<Integer, DailySchedule> mDailySchedules = new HashMap<>();

    public DailySchedule getDailySchedule(int dailyScheduleId, RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        if (mDailySchedules.containsKey(dailyScheduleId))
            return mDailySchedules.get(dailyScheduleId);
        else
            return createDailySchedule(dailyScheduleId, rootTask);
    }

    private DailySchedule createDailySchedule(int dailyScheduleId, RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        DailyScheduleRecord dailyScheduleRecord = persistenceManger.getDailyScheduleRecord(dailyScheduleId);
        if (dailyScheduleRecord == null)
            return null;

        DailySchedule dailySchedule = new DailySchedule(dailyScheduleRecord, rootTask);

        ArrayList<Integer> dailyScheduleTimeIds = persistenceManger.getDailyScheduleTimeIds(dailyScheduleId);
        Assert.assertTrue(!dailyScheduleTimeIds.isEmpty());

        for (Integer dailyScheduleTimeId : dailyScheduleTimeIds)
            dailySchedule.addDailyScheduleTime(DailyScheduleTimeFactory.getInstance().getDailyScheduleTime(dailyScheduleTimeId, dailySchedule));

        mDailySchedules.put(dailyScheduleId, dailySchedule);
        return dailySchedule;
    }
}
