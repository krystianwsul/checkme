package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.Map;

class TaskRelevance {
    private DomainFactory domainFactory;
    @NonNull
    private final Task task;
    private boolean relevant = false;

    TaskRelevance(DomainFactory domainFactory, @NonNull Task task) {
        this.domainFactory = domainFactory;
        this.task = task;
    }

    void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, DomainFactory.InstanceRelevance> instanceRelevances, @NonNull Map<Integer, DomainFactory.LocalCustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
        if (relevant) return;

        relevant = true;

        TaskKey taskKey = task.getTaskKey();

        // mark parents relevant
        Stream.of(task.getTaskHierarchiesByChildTaskKey(taskKey)).map(TaskHierarchy::getParentTaskKey).map(taskRelevances::get).forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        // mark children relevant
        Stream.of(task.getTaskHierarchiesByParentTaskKey(taskKey)).map(TaskHierarchy::getChildTaskKey).map(taskRelevances::get).forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Date oldestVisible = task.getOldestVisible();
        Assert.assertTrue(oldestVisible != null);

        // mark instances relevant
        Stream.of(domainFactory.getPastInstances(task, now)).filter(instance -> instance.getScheduleDate().compareTo(oldestVisible) >= 0).map(instance -> {
            InstanceKey instanceKey = instance.getInstanceKey();

            if (!instanceRelevances.containsKey(instanceKey))
                instanceRelevances.put(instanceKey, new DomainFactory.InstanceRelevance(instance));

            return instanceRelevances.get(instanceKey);
        }).forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(task.getExistingInstances().values()).filter(instance -> instance.getScheduleDate().compareTo(oldestVisible) >= 0).map(Instance::getInstanceKey).map(instanceRelevances::get).forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        // mark custom times relevant
        Stream.of(task.getSchedules()).map(Schedule::getCustomTimeKey).filter(customTimeKey -> customTimeKey != null && customTimeKey.getLocalCustomTimeId() != null).map(customTimeKey -> customTimeRelevances.get(customTimeKey.getLocalCustomTimeId())).forEach(DomainFactory.LocalCustomTimeRelevance::setRelevant);
    }

    boolean getRelevant() {
        return relevant;
    }

    @NonNull
    public Task getTask() {
        return task;
    }

    void setRemoteRelevant(@NonNull Map<kotlin.Pair<String, String>, DomainFactory.RemoteCustomTimeRelevance> remoteCustomTimeRelevances, @NonNull Map<String, DomainFactory.RemoteProjectRelevance> remoteProjectRelevances) {
        Assert.assertTrue(relevant);

        //noinspection Convert2MethodRef
        Stream.of(task.getSchedules()).map(Schedule::getRemoteCustomTimeKey).filter(pair -> pair != null).map(remoteCustomTimeRelevances::get).forEach(DomainFactory.RemoteCustomTimeRelevance::setRelevant);

        RemoteProject remoteProject = task.getRemoteNullableProject();
        if (remoteProject != null) remoteProjectRelevances.get(remoteProject.getId()).setRelevant();
    }
}
