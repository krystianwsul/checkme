package com.example.krystianwsul.organizatortest.domain.tasks;

import com.example.krystianwsul.organizatortest.domain.Completion;
import com.example.krystianwsul.organizatortest.timing.DateTime;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/12/2015.
 */
public class BranchTask implements Task, ChildTask, ParentTask {
    private String mName;
    private ParentTask mParent;

    private ArrayList<ChildTask> mChildren = new ArrayList<>();

    private HashMap<DateTime, Completion> mEntries = new HashMap<>();

    public BranchTask(String name, ParentTask parent) {
        Assert.assertTrue(name != null);
        Assert.assertTrue(!name.isEmpty());
        Assert.assertTrue(parent != null);

        mName = name;
        mParent = parent;
    }

    public String getName() {
        return mName;
    }

    public void addChild(ChildTask child) {
        Assert.assertTrue(child != null);
        mChildren.add(child);
    }

    public ParentTask getParent() {
        return mParent;
    }

    public ArrayList<ChildTask> getChildren() {
        return mChildren;
    }
}
