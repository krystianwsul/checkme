package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.Map;

public class InstanceRelevance {
    private final Instance mInstance;
    private boolean mRelevant = false;

    InstanceRelevance(@NonNull Instance instance) {
        mInstance = instance;
    }

    void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, DomainFactory.LocalCustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
        if (mRelevant) return;

        mRelevant = true;

        // set task relevant
        TaskRelevance taskRelevance = taskRelevances.get(mInstance.getTaskKey());
        Assert.assertTrue(taskRelevance != null);

        taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now);

        // set parent instance relevant
        if (!mInstance.isRootInstance(now)) {
            Instance parentInstance = mInstance.getParentInstance(now);
            Assert.assertTrue(parentInstance != null);

            InstanceKey parentInstanceKey = parentInstance.getInstanceKey();

            if (!instanceRelevances.containsKey(parentInstanceKey))
                instanceRelevances.put(parentInstanceKey, new InstanceRelevance(parentInstance));

            InstanceRelevance parentInstanceRelevance = instanceRelevances.get(parentInstanceKey);
            Assert.assertTrue(parentInstanceRelevance != null);

            parentInstanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now);
        }

        // set child instances relevant
        Stream.of(mInstance.getChildInstances(now)).map(pair -> {
            Instance instance = pair.getFirst();
            InstanceKey instanceKey = instance.getInstanceKey();

            if (!instanceRelevances.containsKey(instanceKey))
                instanceRelevances.put(instanceKey, new InstanceRelevance(instance));

            return instanceRelevances.get(instanceKey);
        }).forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        // set custom time relevant
        CustomTimeKey scheduleCustomTimeKey = mInstance.getScheduleCustomTimeKey();
        if (scheduleCustomTimeKey != null && scheduleCustomTimeKey.getLocalCustomTimeId() != null) {
            DomainFactory.LocalCustomTimeRelevance localCustomTimeRelevance = customTimeRelevances.get(scheduleCustomTimeKey.getLocalCustomTimeId());
            Assert.assertTrue(localCustomTimeRelevance != null);

            localCustomTimeRelevance.setRelevant();
        }

        // set custom time relevant
        CustomTimeKey instanceCustomTimeId = mInstance.getInstanceCustomTimeKey();
        if (instanceCustomTimeId != null && instanceCustomTimeId.getLocalCustomTimeId() != null) {
            DomainFactory.LocalCustomTimeRelevance localCustomTimeRelevance = customTimeRelevances.get(instanceCustomTimeId.getLocalCustomTimeId());
            Assert.assertTrue(localCustomTimeRelevance != null);

            localCustomTimeRelevance.setRelevant();
        }
    }

    void setRemoteRelevant(@NonNull Map<kotlin.Pair<String, String>, DomainFactory.RemoteCustomTimeRelevance> remoteCustomTimeRelevances, @NonNull Map<String, DomainFactory.RemoteProjectRelevance> remoteProjectRelevances) {
        Assert.assertTrue(mRelevant);

        kotlin.Pair<String, String> pair = mInstance.getRemoteCustomTimeKey();
        RemoteProject remoteProject = mInstance.getRemoteNullableProject();
        if (pair != null) {
            Assert.assertTrue(remoteProject != null);

            remoteCustomTimeRelevances.get(pair).setRelevant();
        }

        if (remoteProject != null) remoteProjectRelevances.get(remoteProject.getId()).setRelevant();
    }

    boolean getRelevant() {
        return mRelevant;
    }

    public Instance getInstance() {
        return mInstance;
    }
}
