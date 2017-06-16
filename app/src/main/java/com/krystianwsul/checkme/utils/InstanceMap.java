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
    private final HashMap<TaskKey, HashMap<ScheduleKey, T>> mInstances = new HashMap<>();

    public InstanceMap() {

    }

    public void add(@NonNull T instance) {
        TaskKey taskKey = instance.getTaskKey();

        HashMap<ScheduleKey, T> innerMap = mInstances.get(taskKey);
        if (innerMap == null) {
            innerMap = new HashMap<>();
            mInstances.put(taskKey, innerMap);
        }

        InstanceKey instanceKey = instance.getInstanceKey();

        Assert.assertTrue(!innerMap.containsKey(instanceKey.mScheduleKey));

        innerMap.put(instanceKey.mScheduleKey, instance);
    }

    public void removeForce(@NonNull Instance instance) {
        TaskKey taskKey = instance.getTaskKey();

        HashMap<ScheduleKey, T> innerMap = mInstances.get(taskKey);
        Assert.assertTrue(innerMap != null);

        InstanceKey instanceKey = instance.getInstanceKey();

        T innerInstance = innerMap.get(instanceKey.mScheduleKey);
        Assert.assertTrue(instance.equals(innerInstance));

        innerMap.remove(instanceKey.mScheduleKey);
    }

    @NonNull
    public HashMap<ScheduleKey, T> get(@NonNull TaskKey taskKey) {
        HashMap<ScheduleKey, T> innerMap = mInstances.get(taskKey);
        if (innerMap == null)
            return new HashMap<>();
        else
            return innerMap;
    }

    @Nullable
    public T getIfPresent(@NonNull InstanceKey instanceKey) {
        HashMap<ScheduleKey, T> innerMap = mInstances.get(instanceKey.mTaskKey);
        if (innerMap == null)
            return null;

        return innerMap.get(instanceKey.mScheduleKey);
    }

    public int size() {
        return Stream.of(mInstances.values())
                .map(HashMap::size)
                .reduce(0, (x, y) -> x + y);
    }

    @NonNull
    public List<T> values() {
        return Stream.of(mInstances.values())
                .flatMap(innerMap -> Stream.of(innerMap.values()))
                .collect(Collectors.toList());
    }
}
