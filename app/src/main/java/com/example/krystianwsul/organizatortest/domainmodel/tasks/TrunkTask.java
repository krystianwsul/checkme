package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.Repetition;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.WeeklySchedule;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/13/2015.
 */
public class TrunkTask extends Task {
    private final ArrayList<Task> mChildrenTasks = new ArrayList<>();

    private final Schedule mSchedule;

    protected TrunkTask(int taskId, ArrayList<Integer> childTaskIds) {
        super(taskId);

        Assert.assertTrue(mTaskRecord.getParentTaskId() == null);

        Assert.assertTrue(!childTaskIds.isEmpty());
        for (Integer childTaskId : childTaskIds)
            mChildrenTasks.add(Task.getTask(childTaskId));

        Assert.assertTrue(mTaskRecord.getSingleScheduleId() == null || mTaskRecord.getWeeklyScheduleId() == null);
        Assert.assertTrue(mTaskRecord.getSingleScheduleId() != null || mTaskRecord.getWeeklyScheduleId() != null);

        if (mTaskRecord.getSingleScheduleId() != null)
            mSchedule = SingleSchedule.getSingleSchedule(mTaskRecord.getSingleScheduleId());
        else
            mSchedule = WeeklySchedule.getWeeklySchedule(mTaskRecord.getWeeklyScheduleId());
        Assert.assertTrue(mSchedule != null);
    }

    public ArrayList<Task> getChildTasks() {
        return mChildrenTasks;
    }

    public String getScheduleText(Context context) {
        return mSchedule.getTaskText(context);
    }
}
