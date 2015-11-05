package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.DailyScheduleTime;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.DailyRepetitionRecord;

import java.util.HashMap;

/**
 * Created by Krystian on 10/31/2015.
 */
public abstract class DailyRepetition implements Repetition {
    private static final HashMap<Integer, DailyRepetition> sDailyRepetitions = new HashMap<>();

    public static DailyRepetition getDailyRepetition(int dailyRepetitionId) {
        if (sDailyRepetitions.containsKey(dailyRepetitionId)) {
            return sDailyRepetitions.get(dailyRepetitionId);
        } else {
            DailyRepetition dailyRepetition = new RealDailyRepetition(PersistenceManger.getInstance().getDailyRepetitionRecord(dailyRepetitionId));
            sDailyRepetitions.put(dailyRepetitionId, dailyRepetition);
            return dailyRepetition;
        }
    }

    public static DailyRepetition getDailyRepetition(DailyScheduleTime dailyScheduleTime, Date scheduleDate) {
        DailyRepetition existingDailyRepetition = getExistingDailyRepetition(dailyScheduleTime.getId(), scheduleDate);
        if (existingDailyRepetition != null)
            return existingDailyRepetition;

        DailyRepetitionRecord dailyRepetitionRecord = PersistenceManger.getInstance().getDailyRepetitionRecord(dailyScheduleTime.getId(), scheduleDate);
        if (dailyRepetitionRecord != null) {
            RealDailyRepetition realDailyInstance = new RealDailyRepetition(dailyRepetitionRecord);
            sDailyRepetitions.put(existingDailyRepetition.getId(), existingDailyRepetition);
            return realDailyInstance;
        }

        VirtualDailyRepetition virtualDailyRepetition = new VirtualDailyRepetition(dailyScheduleTime, scheduleDate);
        sDailyRepetitions.put(virtualDailyRepetition.getId(), virtualDailyRepetition);
        return virtualDailyRepetition;
    }

    private static DailyRepetition getExistingDailyRepetition(int dailyScheduleTimeId, Date scheduleDate) {
        for (DailyRepetition dailyRepetition : sDailyRepetitions.values())
            if (dailyRepetition.getDailyScheduleTimeId() == dailyScheduleTimeId && dailyRepetition.getScheduleYear() == scheduleDate.getYear() && dailyRepetition.getScheduleMonth() == scheduleDate.getMonth() && dailyRepetition.getScheduleDay() == scheduleDate.getDay())
                return dailyRepetition;
        return null;
    }

    public abstract int getId();

    public abstract int getDailyScheduleTimeId();

    public abstract int getScheduleYear();

    public abstract int getScheduleMonth();

    public abstract int getScheduleDay();

    public abstract Integer getRepetitionYear();

    public abstract Integer getRepetitionMonth();

    public abstract Integer getRepetitionDay();

    public abstract Time getTime();

    public abstract Date getDate();

    public DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    public Instance getInstance(Task task) {
        return DailyInstance.getDailyInstance(task, this);
    }
}
