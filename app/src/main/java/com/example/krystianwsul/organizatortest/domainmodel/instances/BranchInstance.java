package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.tasks.BranchTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class BranchInstance implements ChildInstance, Instance, ParentInstance {
    private final BranchTask mTask;
    private final ParentInstance mParent;
    private final ArrayList<ChildInstance> mChildren = new ArrayList<>();

    public BranchInstance(BranchTask task, ParentInstance parent) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(parent != null);

        mTask = task;
        mParent = parent;
    }

    public ParentInstance getParent() {
        return mParent;
    }

    public ArrayList<ChildInstance> getChildren() {
        return mChildren;
    }

    public Task getTask() {
        return mTask;
    }
}
