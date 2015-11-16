package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 11/14/2015.
 */
public class SingleInstanceFactory {
    private static SingleInstanceFactory sInstance;

    public static SingleInstanceFactory getInstance() {
        if (sInstance == null)
            sInstance = new SingleInstanceFactory();
        return sInstance;
    }

    private SingleInstanceFactory() {}

    private final HashMap<Integer, SingleInstance> mSingleInstances = new HashMap<>();

    public SingleInstance getSingleInstance(int taskId) {
        Assert.assertTrue(mSingleInstances.containsKey(taskId));
        return mSingleInstances.get(taskId);
    }

    public SingleInstance getSingleInstance(Task task, SingleRepetition singleRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        SingleInstance existingSingleInstance = mSingleInstances.get(task.getId());
        if (existingSingleInstance != null)
            return existingSingleInstance;

        SingleInstance singleInstance = createSingleInstance(task, singleRepetition);
        Assert.assertTrue(singleInstance != null);
        mSingleInstances.put(singleInstance.getTaskId(), singleInstance);
        return singleInstance;
    }

    private SingleInstance createSingleInstance(Task task, SingleRepetition singleRepetition) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(singleRepetition != null);

        SingleInstanceRecord singleInstanceRecord = PersistenceManger.getInstance().getSingleInstanceRecord(task.getId());
        if (singleInstanceRecord != null)
            return new SingleInstance(task, singleRepetition, singleInstanceRecord);
        else
            return new SingleInstance(task, singleRepetition);

    }
}
