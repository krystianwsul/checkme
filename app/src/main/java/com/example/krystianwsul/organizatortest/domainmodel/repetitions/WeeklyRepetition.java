package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.WeeklyRepetitionRecord;

import java.util.HashMap;

/**
 * Created by Krystian on 10/31/2015.
 */
public abstract class WeeklyRepetition implements Repetition {
    private static final HashMap<Integer, WeeklyRepetition> sWeeklyRepetitions = new HashMap<>();

    public static WeeklyRepetition getWeeklyRepetition(int weeklyRepetitionId) {
        if (sWeeklyRepetitions.containsKey(weeklyRepetitionId)) {
            return sWeeklyRepetitions.get(weeklyRepetitionId);
        } else {
            WeeklyRepetition weeklyRepetition = new RealWeeklyRepetition(PersistenceManger.getInstance().getWeeklyRepetitionRecord(weeklyRepetitionId));
            sWeeklyRepetitions.put(weeklyRepetitionId, weeklyRepetition);
            return weeklyRepetition;
        }
    }

    public static WeeklyRepetition getWeeklyRepetition(WeeklyScheduleTime weeklyScheduleTime, Date scheduleDate) {
        WeeklyRepetition existingWeeklyRepetition = getExistingWeeklyRepetition(weeklyScheduleTime.getId(), scheduleDate);
        if (existingWeeklyRepetition != null)
            return existingWeeklyRepetition;

        WeeklyRepetitionRecord weeklyRepetitionRecord = PersistenceManger.getInstance().getWeeklyRepetitionRecord(weeklyScheduleTime.getId(), scheduleDate);
        if (weeklyRepetitionRecord != null) {
            RealWeeklyRepetition realWeeklyInstance = new RealWeeklyRepetition(weeklyRepetitionRecord);
            sWeeklyRepetitions.put(existingWeeklyRepetition.getId(), existingWeeklyRepetition);
            return realWeeklyInstance;
        }

        VirtualWeeklyRepetition virtualWeeklyRepetition = new VirtualWeeklyRepetition(weeklyScheduleTime, scheduleDate);
        sWeeklyRepetitions.put(virtualWeeklyRepetition.getId(), virtualWeeklyRepetition);
        return virtualWeeklyRepetition;
    }

    private static WeeklyRepetition getExistingWeeklyRepetition(int weeklyScheduleTimeId, Date scheduleDate) {
        for (WeeklyRepetition weeklyRepetition : sWeeklyRepetitions.values())
            if (weeklyRepetition.getWeeklyScheduleTimeId() == weeklyScheduleTimeId && weeklyRepetition.getScheduleYear() == scheduleDate.getYear() && weeklyRepetition.getScheduleMonth() == scheduleDate.getMonth() && weeklyRepetition.getScheduleDay() == scheduleDate.getDay())
                return weeklyRepetition;
        return null;
    }

    public abstract int getId();

    public abstract int getWeeklyScheduleTimeId();

    public abstract int getScheduleYear();

    public abstract int getScheduleMonth();

    public abstract int getScheduleDay();

    public Instance getInstance(Task task) {
        return WeeklyInstance.getWeeklyInstance(task.getId(), getId());
    }
}
