package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public abstract class WeeklyInstance implements Instance {
    private final Task mTask;
    private final WeeklyRepetition mWeeklyRepetition;

    public static final String INTENT_KEY = "weeklyInstanceId";

    public WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(weeklyRepetition != null);
        mTask = task;
        mWeeklyRepetition = weeklyRepetition;
    }

    public abstract int getId();

    public int getTaskId() {
        return mTask.getId();
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyRepetition.getId();
    }

    public abstract boolean getDone();

    public String getName() {
        return mTask.getName();
    }

    public String getScheduleText(Context context) {
        if (mTask.getParentTask() == null)
            return mWeeklyRepetition.getRepetitionDateTime().getDisplayText(context);
        else
            return null;
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(WeeklyInstanceFactory.getInstance().getWeeklyInstance(childTask, mWeeklyRepetition));
        return childInstances;
    }

    public String getIntentKey() {
        return INTENT_KEY;
    }

    public int getIntentValue() {
        return getId();
    }

    public DateTime getDateTime() {
        return mWeeklyRepetition.getRepetitionDateTime();
    }
}
