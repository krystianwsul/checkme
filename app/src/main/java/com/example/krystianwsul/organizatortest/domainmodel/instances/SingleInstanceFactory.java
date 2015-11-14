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
        SingleInstance existingSingleInstance = mSingleInstances.get(task.getId());
        if (existingSingleInstance != null)
            return existingSingleInstance;

        SingleInstanceRecord singleInstanceRecord = PersistenceManger.getInstance().getSingleInstanceRecord(task.getId());
        if (singleInstanceRecord != null) {
            RealSingleInstance realSingleInstance = new RealSingleInstance(task, singleInstanceRecord, singleRepetition);
            mSingleInstances.put(realSingleInstance.getTaskId(), realSingleInstance);
            return realSingleInstance;
        }

        VirtualSingleInstance virtualSingleInstance = new VirtualSingleInstance(task, singleRepetition);
        mSingleInstances.put(virtualSingleInstance.getTaskId(), virtualSingleInstance);
        return virtualSingleInstance;
    }
}
