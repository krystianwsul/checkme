package com.example.krystianwsul.organizatortest.domain.tasks;

import com.example.krystianwsul.organizatortest.domain.Completion;
import com.example.krystianwsul.organizatortest.timing.DateTime;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/12/2015.
 */
public class LeafTask implements Task, ChildTask, TipTask {
    private String mName;
    private ParentTask mParent;

    private HashMap<DateTime, Completion> mEntries = new HashMap<>();

    public LeafTask(String name, ParentTask parent) {
        Assert.assertTrue(name != null);
        Assert.assertTrue(!name.isEmpty());
        Assert.assertTrue(parent != null);

        mName = name;
        mParent = parent;
    }

    public String getName() {
        return mName;
    }

    public ParentTask getParent() {
        return mParent;
    }
}
