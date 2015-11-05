package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public abstract class Schedule {
    public abstract int getTaskId();
    public abstract String getTaskText(Context context);
    public abstract ArrayList<Instance> getInstances(Task task, TimeStamp startTimeStamp, TimeStamp endTimeStamp);

    public static Schedule getSchedule(int taskId) {
        SingleSchedule singleSchedule = SingleSchedule.getSingleSchedule(taskId);
        if (singleSchedule != null)
            return singleSchedule;
        DailySchedule dailySchedule = DailySchedule.getDailySchedule(taskId);
        if (dailySchedule != null)
            return dailySchedule;
        throw new IllegalArgumentException("no schedule for taskId == " + taskId);
    }
}
