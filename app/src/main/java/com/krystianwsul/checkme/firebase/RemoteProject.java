package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.firebase.json.CustomTimeJson;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.firebase.records.RemoteProjectRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.firebase.records.RemoteUserRecord;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.TaskHierarchyContainer;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteProject {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteProjectRecord mRemoteProjectRecord;

    @NonNull
    private final Map<String, RemoteTask> mRemoteTasks;

    @NonNull
    private final TaskHierarchyContainer<String, RemoteTaskHierarchy> mRemoteTaskHierarchies = new TaskHierarchyContainer<>();

    @NonNull
    private final Map<String, RemoteCustomTime> mRemoteCustomTimes = new HashMap<>();

    @NonNull
    private final Map<String, RemoteUser> mRemoteUsers = new HashMap<>();

    RemoteProject(@NonNull DomainFactory domainFactory, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull UserData userData) {
        mDomainFactory = domainFactory;
        mRemoteProjectRecord = remoteProjectRecord;

        for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteProjectRecord.getRemoteCustomTimeRecords().values()) {
            Assert.assertTrue(remoteCustomTimeRecord != null);

            RemoteCustomTime remoteCustomTime = new RemoteCustomTime(domainFactory, this, remoteCustomTimeRecord);

            Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId));
            Assert.assertTrue(!mRemoteCustomTimes.containsKey(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId));

            mRemoteCustomTimes.put(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId, remoteCustomTime);

            if (remoteCustomTimeRecord.getOwnerId().equals(domainFactory.getLocalFactory().getUuid()) && domainFactory.getLocalFactory().hasLocalCustomTime(remoteCustomTimeRecord.getLocalId())) {
                LocalCustomTime localCustomTime = domainFactory.getLocalFactory().getLocalCustomTime(remoteCustomTimeRecord.getLocalId());

                localCustomTime.addRemoteCustomTimeRecord(remoteCustomTimeRecord);
            }
        }

        mRemoteTasks = Stream.of(mRemoteProjectRecord.getRemoteTaskRecords().values())
                .map(remoteTaskRecord -> new RemoteTask(domainFactory, this, remoteTaskRecord))
                .collect(Collectors.toMap(RemoteTask::getId, remoteTask -> remoteTask));

        Stream.of(mRemoteProjectRecord.getRemoteTaskHierarchyRecords().values())
                .map(remoteTaskHierarchyRecord -> new RemoteTaskHierarchy(domainFactory, this, remoteTaskHierarchyRecord))
                .forEach(remoteTaskHierarchy -> mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy));

        Stream.of(mRemoteProjectRecord.getRemoteUserRecords().values())
                .map(remoteUserRecord -> new RemoteUser(domainFactory, this, remoteUserRecord))
                .forEach(remoteUser -> mRemoteUsers.put(remoteUser.getId(), remoteUser));

        if (!mRemoteUsers.containsKey(userData.getKey()))  // todo user
            addUserData(userData);
    }

    @NonNull
    public String getId() {
        return mRemoteProjectRecord.getId();
    }

    @NonNull
    public String getName() {
        return mRemoteProjectRecord.getName();
    }

    @NonNull
    public Set<String> getRecordOf() {
        return mRemoteProjectRecord.getRecordOf();
    }

    @Nullable
    public ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteProjectRecord.getEndTime() == null)
            return null;
        else
            return new ExactTimeStamp(mRemoteProjectRecord.getEndTime());
    }

    @NonNull
    RemoteTask newRemoteTask(@NonNull TaskJson taskJson) {
        RemoteTaskRecord remoteTaskRecord = mRemoteProjectRecord.newRemoteTaskRecord(mDomainFactory, taskJson);

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, this, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));
        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        return remoteTask;
    }

    void createTaskHierarchy(@NonNull RemoteTask parentRemoteTask, @NonNull RemoteTask childRemoteTask, @NonNull ExactTimeStamp now) {
        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentRemoteTask.getId(), childRemoteTask.getId(), now.getLong(), null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson);

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, this, remoteTaskHierarchyRecord);

        mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy);
    }

    @NonNull
    public RemoteTask copyLocalTask(@NonNull LocalTask localTask, @NonNull Set<String> recordOf, @NonNull Collection<LocalInstance> localInstances, @NonNull ExactTimeStamp now) {
        Long endTime = (localTask.getEndExactTimeStamp() != null ? localTask.getEndExactTimeStamp().getLong() : null);
        Assert.assertTrue(!recordOf.isEmpty());

        Date oldestVisible = localTask.getOldestVisible();
        Integer oldestVisibleYear;
        Integer oldestVisibleMonth;
        Integer oldestVisibleDay;
        if (oldestVisible != null) {
            oldestVisibleYear = oldestVisible.getYear();
            oldestVisibleMonth = oldestVisible.getMonth();
            oldestVisibleDay = oldestVisible.getDay();
        } else {
            oldestVisibleYear = null;
            oldestVisibleMonth = null;
            oldestVisibleDay = null;
        }

        Map<String, InstanceJson> instanceJsons = new HashMap<>();
        for (LocalInstance localInstance : localInstances) {
            Assert.assertTrue(localInstance.getTaskId() == localTask.getId());

            InstanceJson instanceJson = getInstanceJson(localInstance, recordOf, now);
            ScheduleKey scheduleKey = localInstance.getScheduleKey();

            instanceJsons.put(RemoteInstanceRecord.scheduleKeyToString(mDomainFactory, mRemoteProjectRecord.getId(), scheduleKey), instanceJson);
        }

        TaskJson taskJson = new TaskJson(localTask.getName(), localTask.getStartExactTimeStamp().getLong(), endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, localTask.getNote(), instanceJsons);
        RemoteTaskRecord remoteTaskRecord = mRemoteProjectRecord.newRemoteTaskRecord(mDomainFactory, taskJson);

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, this, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        List<CreateTaskLoader.ScheduleData> scheduleDatas = Stream.of(localTask.getSchedules())
                .map(Schedule::getScheduleData)
                .collect(Collectors.toList());

        remoteTask.createSchedules(now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    private InstanceJson getInstanceJson(@NonNull LocalInstance localInstance, @NonNull Set<String> recordOf, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(!recordOf.isEmpty());

        Long done = (localInstance.getDone() != null ? localInstance.getDone().getLong() : null);

        Date instanceDate = localInstance.getInstanceDate();
        TimePair instanceTimePair = localInstance.getInstanceTimePair();

        String instanceRemoteCustomTimeId;
        Integer instanceHour;
        Integer instanceMinute;
        if (instanceTimePair.mHourMinute != null) {
            Assert.assertTrue(instanceTimePair.mCustomTimeKey == null);

            instanceRemoteCustomTimeId = null;

            instanceHour = instanceTimePair.mHourMinute.getHour();
            instanceMinute = instanceTimePair.mHourMinute.getMinute();
        } else {
            Assert.assertTrue(instanceTimePair.mCustomTimeKey != null);

            instanceRemoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(instanceTimePair.mCustomTimeKey, recordOf, now);

            instanceHour = null;
            instanceMinute = null;
        }

        return new InstanceJson(done, instanceDate.getYear(), instanceDate.getMonth(), instanceDate.getDay(), instanceRemoteCustomTimeId, instanceHour, instanceMinute, localInstance.getHierarchyTime());
    }

    @NonNull
    private RemoteFactory getRemoteFactory() {
        RemoteFactory remoteFactory = mDomainFactory.getRemoteFactory();
        Assert.assertTrue(remoteFactory != null);

        return remoteFactory;
    }

    @NonNull
    Collection<RemoteTask> getRemoteTasks() {
        return mRemoteTasks.values();
    }

    @NonNull
    public RemoteTaskHierarchy copyLocalTaskHierarchy(@NonNull LocalTaskHierarchy localTaskHierarchy, @NonNull Set<String> recordOf, @NonNull String remoteParentTaskId, @NonNull String remoteChildTaskId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteParentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteChildTaskId));
        Assert.assertTrue(!recordOf.isEmpty());

        Long endTime = (localTaskHierarchy.getEndExactTimeStamp() != null ? localTaskHierarchy.getEndExactTimeStamp().getLong() : null);

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.getStartExactTimeStamp().getLong(), endTime);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson);

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, this, remoteTaskHierarchyRecord);

        mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy);

        return remoteTaskHierarchy;
    }

    void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        mRemoteProjectRecord.updateRecordOf(addedFriends, removedFriends);

        // todo user

        Map<String, UserData> friends = mDomainFactory.getFriends();
        Assert.assertTrue(friends != null);

        for (String addedFriend : addedFriends) {
            Assert.assertTrue(!mRemoteUsers.containsKey(addedFriend));

            if (friends.containsKey(addedFriend)) {
                UserData userData = friends.get(addedFriend);
                Assert.assertTrue(userData != null);

                addUserData(userData);
            }
        }

        for (String removedFriend : removedFriends) {
            if (mRemoteUsers.containsKey(removedFriend)) {
                RemoteUser remoteUser = mRemoteUsers.get(removedFriend);
                Assert.assertTrue(remoteUser != null);

                remoteUser.delete();
            }
        }
    }

    private void addUserData(@NonNull UserData userData) {
        String id = userData.getKey();

        Assert.assertTrue(!mRemoteUsers.containsKey(id));

        RemoteUserRecord remoteUserRecord = mRemoteProjectRecord.newRemoteUserRecord(userData.toUserJson());
        RemoteUser remoteUser = new RemoteUser(mDomainFactory, this, remoteUserRecord);

        mRemoteUsers.put(id, remoteUser);
    }

    void deleteTask(@NonNull RemoteTask remoteTask) {
        Assert.assertTrue(mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.remove(remoteTask.getId());
    }

    void deleteTaskHierarchy(@NonNull RemoteTaskHierarchy remoteTasHierarchy) {
        mRemoteTaskHierarchies.removeForce(remoteTasHierarchy.getId());
    }

    @Nullable
    RemoteTask getRemoteTaskIfPresent(@NonNull String taskId) {
        return mRemoteTasks.get(taskId);
    }

    @NonNull
    Set<String> getTaskIds() {
        return mRemoteTasks.keySet();
    }

    @NonNull
    RemoteTask getRemoteTaskForce(@NonNull String taskId) {
        RemoteTask remoteTask = mRemoteTasks.get(taskId);
        Assert.assertTrue(remoteTask != null);

        return remoteTask;
    }

    @NonNull
    Set<RemoteTaskHierarchy> getTaskHierarchiesByChildTaskKey(@NonNull TaskKey childTaskKey) {
        Assert.assertTrue(!TextUtils.isEmpty(childTaskKey.mRemoteTaskId));

        return mRemoteTaskHierarchies.getByChildTaskKey(childTaskKey);
    }

    @NonNull
    Set<RemoteTaskHierarchy> getTaskHierarchiesByParentTaskKey(@NonNull TaskKey parentTaskKey) {
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskKey.mRemoteTaskId));

        return mRemoteTaskHierarchies.getByParentTaskKey(parentTaskKey);
    }

    @NonNull
    RemoteCustomTime getRemoteCustomTime(@NonNull String remoteCustomTimeId) {
        Assert.assertTrue(mRemoteCustomTimes.containsKey(remoteCustomTimeId));

        RemoteCustomTime remoteCustomTime = mRemoteCustomTimes.get(remoteCustomTimeId);
        Assert.assertTrue(remoteCustomTime != null);

        return remoteCustomTime;
    }

    @NonNull
    Collection<RemoteCustomTime> getRemoteCustomTimes() {
        return mRemoteCustomTimes.values();
    }

    @NonNull
    RemoteCustomTimeRecord newRemoteCustomTimeRecord(@NonNull CustomTimeJson customTimeJson) {
        return mRemoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson);
    }

    void deleteCustomTime(@NonNull RemoteCustomTime remoteCustomTime) {
        Assert.assertTrue(mRemoteCustomTimes.containsKey(remoteCustomTime.getId()));

        mRemoteCustomTimes.remove(remoteCustomTime.getId());
    }

    void deleteUser(@NonNull RemoteUser remoteUser) {
        String id = remoteUser.getId();
        Assert.assertTrue(mRemoteUsers.containsKey(id));

        mRemoteUsers.remove(id);
    }

    @NonNull
    public Collection<RemoteUser> getUsers() {
        return mRemoteUsers.values();
    }
}
