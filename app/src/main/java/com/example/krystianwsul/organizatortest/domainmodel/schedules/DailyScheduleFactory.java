package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.support.v4.util.Pair;

import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyScheduleRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class DailyScheduleFactory {
    private static DailyScheduleFactory sInstance;

    public static DailyScheduleFactory getInstance() {
        if (sInstance == null)
            sInstance = new DailyScheduleFactory();
        return sInstance;
    }

    private DailyScheduleFactory() {}

    private final HashMap<Integer, DailySchedule> mDailySchedules = new HashMap<>();

    public DailySchedule getDailySchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        if (mDailySchedules.containsKey(rootTask.getId()))
            return mDailySchedules.get(rootTask.getId());
        else
            return loadDailySchedule(rootTask);
    }

    private DailySchedule loadDailySchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        PersistenceManger persistenceManger = PersistenceManger.getInstance();

        DailyScheduleRecord dailyScheduleRecord = persistenceManger.getDailyScheduleRecord(rootTask.getId());
        if (dailyScheduleRecord == null)
            return null;

        DailySchedule dailySchedule = new DailySchedule(dailyScheduleRecord, rootTask);

        ArrayList<Integer> dailyScheduleTimeIds = persistenceManger.getDailyScheduleTimeIds(rootTask.getId());
        Assert.assertTrue(!dailyScheduleTimeIds.isEmpty());

        DailyScheduleTimeFactory dailyScheduleTimeFactory = DailyScheduleTimeFactory.getInstance();

        for (Integer dailyScheduleTimeId : dailyScheduleTimeIds)
            dailySchedule.addDailyScheduleTime(dailyScheduleTimeFactory.getDailyScheduleTime(dailyScheduleTimeId, dailySchedule));

        mDailySchedules.put(rootTask.getId(), dailySchedule);
        return dailySchedule;
    }

    public DailySchedule createDailySchedule(RootTask rootTask, ArrayList<Pair<CustomTime, HourMinute>> timePairs) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(timePairs != null);
        Assert.assertTrue(!timePairs.isEmpty());

        DailyScheduleRecord dailyScheduleRecord = PersistenceManger.getInstance().createDailyScheduleRecord(rootTask.getId());
        Assert.assertTrue(dailyScheduleRecord != null);

        DailySchedule dailySchedule = new DailySchedule(dailyScheduleRecord, rootTask);

        DailyScheduleTimeFactory dailyScheduleTimeFactory = DailyScheduleTimeFactory.getInstance();

        for (Pair<CustomTime, HourMinute> time : timePairs) {
            CustomTime customTime = time.first;
            HourMinute hourMinute = time.second;
            Assert.assertTrue((customTime == null) != (hourMinute == null));

            dailySchedule.addDailyScheduleTime(dailyScheduleTimeFactory.createDailyScheduleTime(dailySchedule, customTime, hourMinute));
        }

        mDailySchedules.put(dailySchedule.getRootTaskId(), dailySchedule);
        return dailySchedule;
    }

    DailySchedule copy(DailySchedule oldDailySchedule, RootTask newRootTask) {
        Assert.assertTrue(oldDailySchedule != null);
        Assert.assertTrue(newRootTask != null);

        oldDailySchedule.setEndTimeStamp();

        ArrayList<Pair<CustomTime, HourMinute>> timePairs = new ArrayList<>();
        Assert.assertTrue(!oldDailySchedule.getDailyScheduleTimes().isEmpty());
        for (DailyScheduleTime dailyScheduleTime : oldDailySchedule.getDailyScheduleTimes()) {
            Assert.assertTrue(dailyScheduleTime != null);
            timePairs.add(dailyScheduleTime.getTime().getPair());
        }

        return createDailySchedule(newRootTask, timePairs);
    }
}
