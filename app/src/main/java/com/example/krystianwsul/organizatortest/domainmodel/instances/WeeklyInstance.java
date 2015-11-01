package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleTime;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyInstanceRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/31/2015.
 */
public abstract class WeeklyInstance implements Instance {
    private static final HashMap<Integer, WeeklyInstance> sWeeklyInstances = new HashMap<>();

    public static WeeklyInstance getWeeklyInstance(int weeklyInstanceId) {
        if (sWeeklyInstances.containsKey(weeklyInstanceId)) {
            return sWeeklyInstances.get(weeklyInstanceId);
        } else {
            WeeklyInstance weeklyInstance = new RealWeeklyInstance(PersistenceManger.getInstance().getWeeklyInstanceRecord(weeklyInstanceId));
            sWeeklyInstances.put(weeklyInstanceId, weeklyInstance);
            return weeklyInstance;
        }
    }

    public static WeeklyInstance getWeeklyInstance(WeeklyScheduleTime weeklyScheduleTime, Date scheduleDate) {
        WeeklyInstance existingWeeklyInstance = getExistingWeeklyInstance(weeklyScheduleTime.getId(), scheduleDate);
        if (existingWeeklyInstance != null)
            return existingWeeklyInstance;

        WeeklyInstanceRecord weeklyInstanceRecord = PersistenceManger.getInstance().getWeeklyInstanceRecord(weeklyScheduleTime.getId(), scheduleDate);
        if (weeklyInstanceRecord != null) {
            RealWeeklyInstance realWeeklyInstance = new RealWeeklyInstance(weeklyInstanceRecord);
            sWeeklyInstances.put(existingWeeklyInstance.getId(), existingWeeklyInstance);
            return realWeeklyInstance;
        }

        VirtualWeeklyInstance virtualWeeklyInstance = new VirtualWeeklyInstance(weeklyScheduleTime, scheduleDate);
        sWeeklyInstances.put(virtualWeeklyInstance.getId(), virtualWeeklyInstance);
        return virtualWeeklyInstance;
    }

    private static WeeklyInstance getExistingWeeklyInstance(int weeklyScheduleTimeId, Date scheduleDate) {
        for (WeeklyInstance weeklyInstance : sWeeklyInstances.values()) {
            if (weeklyInstance.getWeeklyScheduleTimeId() == weeklyScheduleTimeId && weeklyInstance.getScheduleYear() == scheduleDate.getYear() && weeklyInstance.getScheduleMonth() == scheduleDate.getMonth() && weeklyInstance.getScheduleDay() == scheduleDate.getDay())
                return weeklyInstance;
        }
        return null;
    }

    public abstract int getId();

    public abstract int getWeeklyScheduleTimeId();

    public abstract int getScheduleYear();

    public abstract int getScheduleMonth();

    public abstract  int getScheduleDay();
}
