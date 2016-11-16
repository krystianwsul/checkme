package com.krystianwsul.checkme.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.Instance;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.List;

public class InstanceMap<T extends Instance> {
    private final HashMap<TaskKey, HashMap<InstanceKey, T>> mInstances = new HashMap<>();

    public InstanceMap() {

    }

    public void add(@NonNull T instance) {
        TaskKey taskKey = instance.getTaskKey();

        HashMap<InstanceKey, T> innerMap = mInstances.get(taskKey);
        if (innerMap == null) {
            innerMap = new HashMap<>();
            mInstances.put(taskKey, innerMap);
        }

        InstanceKey instanceKey = instance.getInstanceKey();

        Assert.assertTrue(!innerMap.containsKey(instanceKey));

        innerMap.put(instanceKey, instance);
    }

    @Deprecated
    public boolean contains(@NonNull T instance) {
        TaskKey taskKey = instance.getTaskKey();

        HashMap<InstanceKey, T> innerMap = mInstances.get(taskKey);
        if (innerMap == null)
            return false;

        InstanceKey instanceKey = instance.getInstanceKey();

        T innerInstance = innerMap.get(instanceKey);
        if (innerInstance == null)
            return false;

        Assert.assertTrue(instance.equals(innerInstance));

        return true;
    }

    public void removeIfPresent(@NonNull Instance instance) {
        TaskKey taskKey = instance.getTaskKey();

        HashMap<InstanceKey, T> innerMap = mInstances.get(taskKey);
        if (innerMap == null)
            return;

        InstanceKey instanceKey = instance.getInstanceKey();

        innerMap.remove(instanceKey);
    }

    public void removeForce(@NonNull Instance instance) {
        TaskKey taskKey = instance.getTaskKey();

        HashMap<InstanceKey, T> innerMap = mInstances.get(taskKey);
        Assert.assertTrue(innerMap != null);

        InstanceKey instanceKey = instance.getInstanceKey();

        T innerInstance = innerMap.get(instanceKey);
        Assert.assertTrue(instance.equals(innerInstance));

        innerMap.remove(instanceKey);
    }

    @NonNull
    public HashMap<InstanceKey, T> get(@NonNull TaskKey taskKey) {
        HashMap<InstanceKey, T> innerMap = mInstances.get(taskKey);
        if (innerMap == null)
            return new HashMap<>();
        else
            return innerMap;
    }

    @Nullable
    public T getIfPresent(@NonNull InstanceKey instanceKey) {
        HashMap<InstanceKey, T> innerMap = mInstances.get(instanceKey.mTaskKey);
        if (innerMap == null)
            return null;

        return innerMap.get(instanceKey);
    }

    @NonNull
    public T getForce(@NonNull InstanceKey instanceKey) {
        HashMap<InstanceKey, T> innerMap = mInstances.get(instanceKey.mTaskKey);
        Assert.assertTrue(innerMap != null);

        T instance = innerMap.get(instanceKey);
        Assert.assertTrue(instance != null);

        return instance;
    }

    public int size() {
        return Stream.of(mInstances.values())
                .map(HashMap::size)
                .reduce(0, (x, y) -> x + y);
    }

    @Deprecated
    @NonNull
    public List<T> values() {
        return Stream.of(mInstances.values())
                .flatMap(innerMap -> Stream.of(innerMap.values()))
                .collect(Collectors.toList());
    }
}
