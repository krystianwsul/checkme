package com.example.krystianwsul.organizatortest.domain.instances;

import com.example.krystianwsul.organizatortest.domain.tasks.Task;
import com.example.krystianwsul.organizatortest.domain.tasks.TrunkTask;
import com.example.krystianwsul.organizatortest.timing.DateTime;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class TrunkInstance implements Instance, ParentInstance, TopInstance {
    private final TrunkTask mTask;
    private final DateTime mDateTime;
    private final ArrayList<ChildInstance> mChildren = new ArrayList<>();

    public TrunkInstance(TrunkTask task, DateTime dateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dateTime != null);

        mTask = task;
        mDateTime = dateTime;
    }

    public ArrayList<ChildInstance> getChildren() {
        return mChildren;
    }

    public DateTime getDateTime() {
        return mDateTime;
    }

    public Task getTask() {
        return mTask;
    }

    public int compareTo(TopInstance instance) {
        int dateTimeComparison = mDateTime.compareTo(instance.getDateTime());
        if (dateTimeComparison != 0)
            return dateTimeComparison;

        return mTask.getName().compareTo(instance.getTask().getName());
    }

    public boolean hasChildren() {
        return true;
    }
}
