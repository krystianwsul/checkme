package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/2/2015.
 */
public abstract class SingleInstance implements Instance {
    protected final Task mTask;
    protected final SingleRepetition mSingleRepetition;

    public static final String INTENT_KEY = "singleInstanceId";

    protected SingleInstance(Task task, SingleRepetition singleRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        mTask = task;
        mSingleRepetition = singleRepetition;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public String getName() {
        return mTask.getName();
    }

    public ArrayList<Instance> getChildInstances() {
        ArrayList<Instance> childInstances = new ArrayList<>();
        for (ChildTask childTask : mTask.getChildTasks())
            childInstances.add(SingleInstanceFactory.getInstance().getSingleInstance(childTask, mSingleRepetition));
        return childInstances;
    }

    public String getIntentKey() {
        return INTENT_KEY;
    }

    public int getIntentValue() {
        return mTask.getId();
    }

    public String getScheduleText(Context context) {
        if (mTask.getParentTask() == null)
            return getDateTime().getDisplayText(context);
        else
            return null;
    }

    public DateTime getDateTime() {
        return mSingleRepetition.getRepetitionDateTime();
    }
}