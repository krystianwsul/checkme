package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.UserInfo;
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
import com.krystianwsul.checkme.firebase.records.RemoteProjectUserRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.TaskHierarchyContainer;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.Collection;
import java.util.HashMap;
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
    private final Map<String, RemoteProjectUser> mRemoteUsers = new HashMap<>();

    RemoteProject(@NonNull DomainFactory domainFactory, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull UserInfo userInfo, @NonNull String uuid, @NonNull ExactTimeStamp now) {
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
                .map(remoteTaskRecord -> new RemoteTask(domainFactory, this, remoteTaskRecord, now))
                .collect(Collectors.toMap(RemoteTask::getId, remoteTask -> remoteTask));

        Stream.of(mRemoteProjectRecord.getRemoteTaskHierarchyRecords().values())
                .map(remoteTaskHierarchyRecord -> new RemoteTaskHierarchy(domainFactory, this, remoteTaskHierarchyRecord))
                .forEach(remoteTaskHierarchy -> mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy));

        Stream.of(mRemoteProjectRecord.getRemoteUserRecords().values())
                .map(remoteUserRecord -> new RemoteProjectUser(this, remoteUserRecord))
                .forEach(remoteUser -> mRemoteUsers.put(remoteUser.getId(), remoteUser));

        updateUserInfo(userInfo, uuid);
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
    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteProjectRecord.getStartTime());
    }

    @Nullable
    private ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteProjectRecord.getEndTime() == null)
            return null;
        else
            return new ExactTimeStamp(mRemoteProjectRecord.getEndTime());
    }

    @NonNull
    RemoteTask newRemoteTask(@NonNull TaskJson taskJson, @NonNull ExactTimeStamp now) {
        RemoteTaskRecord remoteTaskRecord = mRemoteProjectRecord.newRemoteTaskRecord(mDomainFactory, taskJson);

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, this, remoteTaskRecord, now);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));
        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        return remoteTask;
    }

    void createTaskHierarchy(@NonNull RemoteTask parentRemoteTask, @NonNull RemoteTask childRemoteTask, @NonNull ExactTimeStamp now) {
        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentRemoteTask.getId(), childRemoteTask.getId(), now.getLong(), null, null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson);

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, this, remoteTaskHierarchyRecord);

        mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy);
    }

    @NonNull
    public RemoteTask copyLocalTask(@NonNull LocalTask localTask, @NonNull Collection<LocalInstance> localInstances, @NonNull ExactTimeStamp now) {
        Long endTime = (localTask.getEndExactTimeStamp() != null ? localTask.getEndExactTimeStamp().getLong() : null);

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

            InstanceJson instanceJson = getInstanceJson(localInstance);
            ScheduleKey scheduleKey = localInstance.getScheduleKey();

            if (scheduleKey.ScheduleTimePair.mCustomTimeKey != null)
                getRemoteFactory().getRemoteCustomTimeId(scheduleKey.ScheduleTimePair.mCustomTimeKey, this);

            instanceJsons.put(RemoteInstanceRecord.scheduleKeyToString(mDomainFactory, mRemoteProjectRecord.getId(), scheduleKey), instanceJson);
        }

        TaskJson taskJson = new TaskJson(localTask.getName(), localTask.getStartExactTimeStamp().getLong(), endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, localTask.getNote(), instanceJsons);
        RemoteTaskRecord remoteTaskRecord = mRemoteProjectRecord.newRemoteTaskRecord(mDomainFactory, taskJson);

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, this, remoteTaskRecord, now);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        remoteTask.copySchedules(localTask.getSchedules());

        return remoteTask;
    }

    @NonNull
    private InstanceJson getInstanceJson(@NonNull LocalInstance localInstance) {
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

            instanceRemoteCustomTimeId = getRemoteFactory().getRemoteCustomTimeId(instanceTimePair.mCustomTimeKey, this);

            instanceHour = null;
            instanceMinute = null;
        }

        return new InstanceJson(done, instanceDate.getYear(), instanceDate.getMonth(), instanceDate.getDay(), instanceRemoteCustomTimeId, instanceHour, instanceMinute, localInstance.getHierarchyTime(), localInstance.getOrdinal());
    }

    @NonNull
    private RemoteProjectFactory getRemoteFactory() {
        RemoteProjectFactory remoteProjectFactory = mDomainFactory.getRemoteFactory();
        Assert.assertTrue(remoteProjectFactory != null);

        return remoteProjectFactory;
    }

    @NonNull
    public Collection<RemoteTask> getRemoteTasks() {
        return mRemoteTasks.values();
    }

    @NonNull
    public RemoteTaskHierarchy copyLocalTaskHierarchy(@NonNull LocalTaskHierarchy localTaskHierarchy, @NonNull String remoteParentTaskId, @NonNull String remoteChildTaskId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteParentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteChildTaskId));

        Long endTime = (localTaskHierarchy.getEndExactTimeStamp() != null ? localTaskHierarchy.getEndExactTimeStamp().getLong() : null);

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.getStartExactTimeStamp().getLong(), endTime, localTaskHierarchy.getOrdinal());
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteProjectRecord.newRemoteTaskHierarchyRecord(taskHierarchyJson);

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, this, remoteTaskHierarchyRecord);

        mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy);

        return remoteTaskHierarchy;
    }

    public void updateRecordOf(@NonNull Set<RemoteRootUser> addedFriends, @NonNull Set<String> removedFriends) {
        mRemoteProjectRecord.updateRecordOf(Stream.of(addedFriends)
                .map(RemoteRootUser::getId)
                .collect(Collectors.toSet()), removedFriends);

        for (RemoteRootUser addedFriend : addedFriends) {
            Assert.assertTrue(addedFriend != null);

            addUser(addedFriend);
        }

        for (String removedFriend : removedFriends) {
            Assert.assertTrue(mRemoteUsers.containsKey(removedFriend));

            RemoteProjectUser remoteProjectUser = mRemoteUsers.get(removedFriend);
            Assert.assertTrue(remoteProjectUser != null);

            remoteProjectUser.delete();
        }
    }

    private void addUser(@NonNull RemoteRootUser remoteRootUser) {
        String id = remoteRootUser.getId();

        Assert.assertTrue(!mRemoteUsers.containsKey(id));

        RemoteProjectUserRecord remoteProjectUserRecord = mRemoteProjectRecord.newRemoteUserRecord(remoteRootUser.getUserJson());
        RemoteProjectUser remoteProjectUser = new RemoteProjectUser(this, remoteProjectUserRecord);

        mRemoteUsers.put(id, remoteProjectUser);
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
    RemoteCustomTime newRemoteCustomTime(@NonNull CustomTimeJson customTimeJson) {
        RemoteCustomTimeRecord remoteCustomTimeRecord = mRemoteProjectRecord.newRemoteCustomTimeRecord(customTimeJson);

        RemoteCustomTime remoteCustomTime = new RemoteCustomTime(mDomainFactory, this, remoteCustomTimeRecord);

        Assert.assertTrue(!mRemoteCustomTimes.containsKey(remoteCustomTime.getId()));

        mRemoteCustomTimes.put(remoteCustomTime.getId(), remoteCustomTime);

        return remoteCustomTime;
    }

    void deleteCustomTime(@NonNull RemoteCustomTime remoteCustomTime) {
        Assert.assertTrue(mRemoteCustomTimes.containsKey(remoteCustomTime.getId()));

        mRemoteCustomTimes.remove(remoteCustomTime.getId());
    }

    void deleteUser(@NonNull RemoteProjectUser remoteProjectUser) {
        String id = remoteProjectUser.getId();
        Assert.assertTrue(mRemoteUsers.containsKey(id));

        mRemoteUsers.remove(id);
    }

    @NonNull
    public Collection<RemoteProjectUser> getUsers() {
        return mRemoteUsers.values();
    }

    void updateUserInfo(@NonNull UserInfo userInfo, @NonNull String uuid) {
        String key = userInfo.getKey();
        Assert.assertTrue(mRemoteUsers.containsKey(key));

        RemoteProjectUser remoteProjectUser = mRemoteUsers.get(key);
        Assert.assertTrue(remoteProjectUser != null);

        remoteProjectUser.setName(userInfo.mName);
        remoteProjectUser.setToken(userInfo.mToken, uuid);
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteProjectRecord.setName(name);
    }

    public void delete() {
        getRemoteFactory().deleteProject(this);

        mRemoteProjectRecord.delete();
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        Stream.of(mRemoteTasks.values())
                .filter(task -> task.current(now))
                .forEach(task -> task.setEndExactTimeStamp(now));

        mRemoteProjectRecord.setEndTime(now.getLong());
    }

    @NonNull
    public RemoteTaskHierarchy getTaskHierarchy(@NonNull String id) {
        return mRemoteTaskHierarchies.getById(id);
    }
}
