package com.example.krystianwsul.organizatortest.domain.instances;

import com.example.krystianwsul.organizatortest.domain.tasks.LeafTask;
import com.example.krystianwsul.organizatortest.domain.tasks.Task;

import junit.framework.Assert;

/**
 * Created by Krystian on 10/23/2015.
 */
public class LeafInstance implements ChildInstance, Instance, TipInstance {
    private final LeafTask mTask;
    private final ParentInstance mParent;

    public LeafInstance(LeafTask task, ParentInstance parent) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(parent != null);

        mTask = task;
        mParent = parent;
    }

    public ParentInstance getParent() {
        return mParent;
    }

    public Task getTask() {
        return mTask;
    }
}
