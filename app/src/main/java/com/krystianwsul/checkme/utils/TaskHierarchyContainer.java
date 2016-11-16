package com.krystianwsul.checkme.utils;

import android.support.annotation.NonNull;

import com.google.common.collect.HashMultimap;
import com.krystianwsul.checkme.domainmodel.TaskHierarchy;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Set;

public class TaskHierarchyContainer<T, U extends TaskHierarchy> {
    @NonNull
    private final HashMap<T, U> mTaskHierarchiesById = new HashMap<>();

    @NonNull
    private final HashMultimap<TaskKey, U> mTaskHierarchiesByParent = HashMultimap.create();

    @NonNull
    private final HashMultimap<TaskKey, U> mTaskHierarchiesByChild = HashMultimap.create();

    public TaskHierarchyContainer() {

    }

    public void add(T id, @NonNull U taskHierarchy) {
        Assert.assertTrue(!mTaskHierarchiesById.containsKey(id));

        mTaskHierarchiesById.put(id, taskHierarchy);
        mTaskHierarchiesByChild.put(taskHierarchy.getChildTaskKey(), taskHierarchy);
        mTaskHierarchiesByParent.put(taskHierarchy.getParentTaskKey(), taskHierarchy);
    }

    public void removeForce(T id) {
        Assert.assertTrue(mTaskHierarchiesById.containsKey(id));

        U taskHierarchy = mTaskHierarchiesById.get(id);
        Assert.assertTrue(taskHierarchy != null);

        mTaskHierarchiesById.remove(id);

        TaskKey childTaskKey = taskHierarchy.getChildTaskKey();
        Assert.assertTrue(mTaskHierarchiesByChild.containsEntry(childTaskKey, taskHierarchy));

        mTaskHierarchiesByChild.remove(childTaskKey, taskHierarchy);

        TaskKey parentTaskKey = taskHierarchy.getParentTaskKey();
        Assert.assertTrue(mTaskHierarchiesByParent.containsEntry(parentTaskKey, taskHierarchy));

        mTaskHierarchiesByChild.remove(parentTaskKey, taskHierarchy);
    }

    @NonNull
    public Set<U> getByChildTaskKey(@NonNull TaskKey childTaskKey) {
        Set<U> set = mTaskHierarchiesByChild.get(childTaskKey);
        Assert.assertTrue(set != null);

        return set;
    }

    @NonNull
    public Set<U> getByParentTaskKey(@NonNull TaskKey parentTaskKey) {
        Set<U> set = mTaskHierarchiesByParent.get(parentTaskKey);
        Assert.assertTrue(set != null);

        return set;
    }
}
