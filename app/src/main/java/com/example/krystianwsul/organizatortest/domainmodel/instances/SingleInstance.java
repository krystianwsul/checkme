package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleInstanceRecord;

import java.util.HashMap;

/**
 * Created by Krystian on 11/2/2015.
 */
public abstract class SingleInstance implements Instance {
    private static final HashMap<Integer, SingleInstance> sSingleInstances = new HashMap<>();

    public static SingleInstance getSingleInstance(int singleInstanceId) {
        if (sSingleInstances.containsKey(singleInstanceId)) {
            return sSingleInstances.get(singleInstanceId);
        } else {
            SingleInstance singleInstance = new RealSingleInstance(PersistenceManger.getInstance().getSingleInstanceRecord(singleInstanceId));
            sSingleInstances.put(singleInstanceId, singleInstance);
            return singleInstance;
        }
    }

    public static SingleInstance getSingleInstance(int taskId, int singleScheduleId) {
        SingleInstance existingSingleInstance = getExistingSingleInstance(taskId, singleScheduleId);
        if (existingSingleInstance != null)
            return existingSingleInstance;

        SingleInstanceRecord singleInstanceRecord = PersistenceManger.getInstance().getSingleInstanceRecord(taskId, singleScheduleId);
        if (singleInstanceRecord != null) {
            RealSingleInstance realSingleInstance = new RealSingleInstance(singleInstanceRecord);
            sSingleInstances.put(realSingleInstance.getId(), realSingleInstance);
            return realSingleInstance;
        }

        VirtualSingleInstance virtualSingleInstance = new VirtualSingleInstance(taskId, singleScheduleId);
        sSingleInstances.put(virtualSingleInstance.getId(), virtualSingleInstance);
        return virtualSingleInstance;
    }

    private static SingleInstance getExistingSingleInstance(int taskId, int singleScheduleId) {
        for (SingleInstance singleInstance : sSingleInstances.values())
            if (singleInstance.getTaskId() == taskId && singleInstance.getSingleScheduleId() == singleScheduleId)
                return singleInstance;
        return null;
    }

    public abstract int getId();

    public abstract int getTaskId();

    public abstract int getSingleScheduleId();

    public abstract boolean getDone();
}
