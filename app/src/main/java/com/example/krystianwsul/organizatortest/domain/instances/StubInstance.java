package com.example.krystianwsul.organizatortest.domain.instances;

import com.example.krystianwsul.organizatortest.domain.tasks.StubTask;
import com.example.krystianwsul.organizatortest.domain.tasks.Task;
import com.example.krystianwsul.organizatortest.timing.DateTime;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/23/2015.
 */
public class StubInstance implements Instance, TipInstance, TopInstance {
    private final StubTask mTask;
    private final DateTime mDateTime;

    public StubInstance(StubTask task, DateTime dateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(dateTime != null);

        mTask = task;
        mDateTime = dateTime;
    }

    public DateTime getDateTime() {
        return mDateTime;
    }

    public int compareTo(TopInstance instance) {
        int dateTimeComparison = mDateTime.compareTo(instance.getDateTime());
        if (dateTimeComparison != 0)
            return dateTimeComparison;

        return mTask.getName().compareTo(instance.getTask().getName());
    }

    public Task getTask() {
        return mTask;
    }
}
