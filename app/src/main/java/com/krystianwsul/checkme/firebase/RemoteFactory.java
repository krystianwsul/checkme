package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.firebase.json.CustomTimeJson;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.firebase.records.RemoteManager;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.TaskHierarchyContainer;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteFactory {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final UserData mUserData;

    @NonNull
    private final RemoteManager mRemoteManager;

    @NonNull
    private final Map<String, RemoteTask> mRemoteTasks;

    @NonNull
    private final TaskHierarchyContainer<String, RemoteTaskHierarchy> mRemoteTaskHierarchies = new TaskHierarchyContainer<>();

    @NonNull
    private final Map<String, RemoteCustomTime> mRemoteCustomTimes;

    public RemoteFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children, @NonNull UserData userData) {
        mDomainFactory = domainFactory;
        mUserData = userData;

        mRemoteManager = new RemoteManager(children);

        mRemoteTasks = Stream.of(mRemoteManager.mRemoteTaskRecords.values())
                .map(remoteTaskRecord -> new RemoteTask(domainFactory, remoteTaskRecord))
                .collect(Collectors.toMap(RemoteTask::getId, remoteTask -> remoteTask));

        Stream.of(mRemoteManager.mRemoteTaskHierarchyRecords.values())
                .map(remoteTaskHierarchyRecord -> new RemoteTaskHierarchy(domainFactory, remoteTaskHierarchyRecord))
                .forEach(remoteTaskHierarchy -> mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy));

        mRemoteCustomTimes = new HashMap<>();

        for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteManager.mRemoteCustomTimeRecords.values()) {
            Assert.assertTrue(remoteCustomTimeRecord != null);

            if (remoteCustomTimeRecord.getOwnerId().equals(domainFactory.getLocalFactory().getUuid())) {
                if (domainFactory.getLocalFactory().hasLocalCustomTime(remoteCustomTimeRecord.getLocalId())) {
                    LocalCustomTime localCustomTime = domainFactory.getLocalFactory().getLocalCustomTime(remoteCustomTimeRecord.getLocalId());

                    localCustomTime.setRemoteCustomTimeRecord(remoteCustomTimeRecord);
                } else {
                    // Albo jakiś syf, albo localCustomTime został usunięty gdy nie było połączenia.

                    remoteCustomTimeRecord.delete(); // faktyczne usunięcie nastąpi przy następnym zapisywaniu czegoś innego
                }
            } else {
                RemoteCustomTime remoteCustomTime = new RemoteCustomTime(domainFactory, remoteCustomTimeRecord);

                Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId));
                Assert.assertTrue(!mRemoteCustomTimes.containsKey(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId));

                mRemoteCustomTimes.put(remoteCustomTime.getCustomTimeKey().mRemoteCustomTimeId, remoteCustomTime);
            }
        }
    }

    @NonNull
    public RemoteTask createScheduleRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull Collection<String> friends) {
        RemoteTask remoteTask = createRemoteTaskHelper(now, name, note, friends);

        remoteTask.createSchedules(now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    public RemoteTask createRemoteTaskHelper(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note, @NonNull Collection<String> friends) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note, Collections.emptyMap());

        Set<String> recordOf = new HashSet<>(friends);
        recordOf.add(UserData.getKey(mUserData.email));

        RemoteTaskRecord remoteTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(recordOf, taskJson));

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));
        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        return remoteTask;
    }

    public void save(boolean causedByRemote) {
        Assert.assertTrue(!mRemoteManager.isSaved());

        mRemoteManager.save(causedByRemote);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSaved() {
        return mRemoteManager.isSaved();
    }

    @NonNull
    RemoteTask createChildTask(@NonNull RemoteTask parentTask, @NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note, Collections.emptyMap());
        RemoteTaskRecord childTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(parentTask.getRecordOf(), taskJson));

        RemoteTask childTask = new RemoteTask(mDomainFactory, childTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(childTask.getId()));

        mRemoteTasks.put(childTask.getId(), childTask);

        createTaskHierarchy(parentTask, childTask, now);

        return childTask;
    }

    void createTaskHierarchy(@NonNull RemoteTask parentRemoteTask, @NonNull RemoteTask childRemoteTask, @NonNull ExactTimeStamp now) {
        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(parentRemoteTask.getId(), childRemoteTask.getId(), now.getLong(), null);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(parentRemoteTask.getRecordOf(), taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, remoteTaskHierarchyRecord);

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

            InstanceJson instanceJson = getInstanceJson(localInstance, recordOf);
            ScheduleKey scheduleKey = localInstance.getScheduleKey();

            instanceJsons.put(RemoteInstanceRecord.scheduleKeyToString(scheduleKey), instanceJson);
        }

        TaskJson taskJson = new TaskJson(localTask.getName(), localTask.getStartExactTimeStamp().getLong(), endTime, oldestVisibleYear, oldestVisibleMonth, oldestVisibleDay, localTask.getNote(), instanceJsons);
        RemoteTaskRecord remoteTaskRecord = mRemoteManager.newRemoteTaskRecord(new JsonWrapper(recordOf, taskJson));

        RemoteTask remoteTask = new RemoteTask(mDomainFactory, remoteTaskRecord);
        Assert.assertTrue(!mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.put(remoteTask.getId(), remoteTask);

        List<CreateTaskLoader.ScheduleData> scheduleDatas = Stream.of(localTask.getSchedules())
                .map(Schedule::getScheduleData)
                .collect(Collectors.toList());

        remoteTask.createSchedules(now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    public RemoteTaskHierarchy copyLocalTaskHierarchy(@NonNull LocalTaskHierarchy localTaskHierarchy, @NonNull Set<String> recordOf, @NonNull String remoteParentTaskId, @NonNull String remoteChildTaskId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteParentTaskId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteChildTaskId));
        Assert.assertTrue(!recordOf.isEmpty());

        Long endTime = (localTaskHierarchy.getEndExactTimeStamp() != null ? localTaskHierarchy.getEndExactTimeStamp().getLong() : null);

        TaskHierarchyJson taskHierarchyJson = new TaskHierarchyJson(remoteParentTaskId, remoteChildTaskId, localTaskHierarchy.getStartExactTimeStamp().getLong(), endTime);
        RemoteTaskHierarchyRecord remoteTaskHierarchyRecord = mRemoteManager.newRemoteTaskHierarchyRecord(new JsonWrapper(recordOf, taskHierarchyJson));

        RemoteTaskHierarchy remoteTaskHierarchy = new RemoteTaskHierarchy(mDomainFactory, remoteTaskHierarchyRecord);

        mRemoteTaskHierarchies.add(remoteTaskHierarchy.getId(), remoteTaskHierarchy);

        return remoteTaskHierarchy;
    }

    @NonNull
    private InstanceJson getInstanceJson(@NonNull LocalInstance localInstance, @NonNull Set<String> recordOf) {
        Assert.assertTrue(!recordOf.isEmpty());

        Long done = (localInstance.getDone() != null ? localInstance.getDone().getLong() : null);

        Date scheduleDate = localInstance.getScheduleDate();
        TimePair scheduleTimePair = localInstance.getScheduleTimePair();

        String scheduleRemoteCustomTimeId;
        Integer scheduleHour;
        Integer scheduleMinute;
        if (scheduleTimePair.mHourMinute != null) {
            Assert.assertTrue(scheduleTimePair.mCustomTimeKey == null);

            scheduleRemoteCustomTimeId = null;

            scheduleHour = scheduleTimePair.mHourMinute.getHour();
            scheduleMinute = scheduleTimePair.mHourMinute.getMinute();
        } else {
            Assert.assertTrue(scheduleTimePair.mCustomTimeKey != null);

            scheduleRemoteCustomTimeId = getRemoteCustomTimeId(scheduleTimePair.mCustomTimeKey, recordOf);

            scheduleHour = null;
            scheduleMinute = null;
        }

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

            instanceRemoteCustomTimeId = getRemoteCustomTimeId(instanceTimePair.mCustomTimeKey, recordOf);

            instanceHour = null;
            instanceMinute = null;
        }

        return new InstanceJson(done, scheduleDate.getYear(), scheduleDate.getMonth(), scheduleDate.getDay(), scheduleRemoteCustomTimeId, scheduleHour, scheduleMinute, instanceDate.getYear(), instanceDate.getMonth(), instanceDate.getDay(), instanceRemoteCustomTimeId, instanceHour, instanceMinute, localInstance.getHierarchyTime());
    }

    void updateRecordOf(@NonNull RemoteTask startingRemoteTask, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        UpdateRecordOfData updateRecordOfData = new UpdateRecordOfData();

        updateRecordOfHelper(updateRecordOfData, startingRemoteTask);

        for (RemoteTask remoteTask : updateRecordOfData.mRemoteTasks)
            remoteTask.updateRecordOf(addedFriends, removedFriends);

        for (RemoteTaskHierarchy remoteTaskHierarchy : updateRecordOfData.mRemoteTaskHierarchies)
            remoteTaskHierarchy.updateRecordOf(addedFriends, removedFriends);
    }

    private void updateRecordOfHelper(@NonNull UpdateRecordOfData updateRecordOfData, @NonNull RemoteTask remoteTask) {
        if (updateRecordOfData.mRemoteTasks.contains(remoteTask))
            return;

        TaskKey taskKey = remoteTask.getTaskKey();

        updateRecordOfData.mRemoteTasks.add(remoteTask);

        Set<RemoteTaskHierarchy> parentRemoteTaskHierarchies = mRemoteTaskHierarchies.getByChildTaskKey(taskKey);

        updateRecordOfData.mRemoteTaskHierarchies.addAll(parentRemoteTaskHierarchies);

        Stream.of(mRemoteTaskHierarchies.getByParentTaskKey(taskKey))
                .map(RemoteTaskHierarchy::getChildTask)
                .forEach(childTask -> updateRecordOfHelper(updateRecordOfData, (RemoteTask) childTask));

        Stream.of(parentRemoteTaskHierarchies)
                .map(RemoteTaskHierarchy::getParentTask)
                .forEach(parentTask -> updateRecordOfHelper(updateRecordOfData, (RemoteTask) parentTask));
    }

    private static class UpdateRecordOfData {
        final List<RemoteTask> mRemoteTasks = new ArrayList<>();
        final List<RemoteTaskHierarchy> mRemoteTaskHierarchies = new ArrayList<>();
    }

    @NonNull
    public Collection<RemoteTask> getTasks() {
        return mRemoteTasks.values();
    }

    @NonNull
    String getRemoteCustomTimeId(@NonNull CustomTimeKey customTimeKey, @NonNull Set<String> recordOf) {
        Assert.assertTrue(customTimeKey.mLocalCustomTimeId != null);
        Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));

        int localCustomTimeId = customTimeKey.mLocalCustomTimeId;

        LocalCustomTime localCustomTime = mDomainFactory.getLocalFactory().getLocalCustomTime(localCustomTimeId);

        if (!localCustomTime.hasRemoteRecord()) {
            CustomTimeJson customTimeJson = new CustomTimeJson(mDomainFactory.getLocalFactory().getUuid(), localCustomTime.getId(), localCustomTime.getName(), localCustomTime.getHourMinute(DayOfWeek.SUNDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.SUNDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.MONDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.MONDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.TUESDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.TUESDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.THURSDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.THURSDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.FRIDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.FRIDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.SATURDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.SATURDAY).getMinute());
            JsonWrapper jsonWrapper = new JsonWrapper(recordOf, customTimeJson);

            RemoteCustomTimeRecord remoteCustomTimeRecord = mRemoteManager.newRemoteCustomTimeRecord(jsonWrapper);

            localCustomTime.setRemoteCustomTimeRecord(remoteCustomTimeRecord);
        } else {
            localCustomTime.updateRecordOf(recordOf, new HashSet<>());
        }

        return localCustomTime.getRemoteId();
    }

    @NonNull
    public RemoteCustomTime getRemoteCustomTime(@NonNull String remoteCustomTimeId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId));
        Assert.assertTrue(mRemoteCustomTimes.containsKey(remoteCustomTimeId));

        RemoteCustomTime remoteCustomTime = mRemoteCustomTimes.get(remoteCustomTimeId);
        Assert.assertTrue(remoteCustomTime != null);

        return remoteCustomTime;
    }

    @NonNull
    public Collection<RemoteCustomTime> getRemoteCustomTimes() {
        return mRemoteCustomTimes.values();
    }

    void deleteTask(@NonNull RemoteTask remoteTask) {
        Assert.assertTrue(mRemoteTasks.containsKey(remoteTask.getId()));

        mRemoteTasks.remove(remoteTask.getId());
    }

    void deleteTaskHierarchy(@NonNull RemoteTaskHierarchy remoteTasHierarchy) {
        mRemoteTaskHierarchies.removeForce(remoteTasHierarchy.getId());
    }

    public int getInstanceCount() {
        return Stream.of(mRemoteTasks.values())
                .map(remoteTask -> remoteTask.getExistingInstances().size())
                .reduce(0, (x, y) -> x + y);
    }

    @NonNull
    public List<RemoteInstance> getExistingInstances() {
        return Stream.of(mRemoteTasks.values())
                .flatMap(remoteTask -> Stream.of(remoteTask.getExistingInstances().values()))
                .collect(Collectors.toList());
    }

    @Nullable
    public RemoteInstance getExistingInstanceIfPresent(@NonNull InstanceKey instanceKey) {
        TaskKey taskKey = instanceKey.mTaskKey;

        if (TextUtils.isEmpty(taskKey.mRemoteTaskId))
            return null;

        RemoteTask remoteTask = mRemoteTasks.get(taskKey.mRemoteTaskId);
        if (remoteTask == null)
            return null;

        return remoteTask.getExistingInstanceIfPresent(instanceKey.mScheduleKey);
    }

    @NonNull
    public RemoteTask getTaskForce(@NonNull String taskId) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        RemoteTask remoteTask = mRemoteTasks.get(taskId);
        Assert.assertTrue(remoteTask != null);

        return remoteTask;
    }

    @Nullable
    public RemoteTask getTaskIfPresent(@NonNull String taskId) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        return mRemoteTasks.get(taskId);
    }

    @NonNull
    public Set<String> getTaskKeys() {
        return mRemoteTasks.keySet();
    }

    public int getTaskCount() {
        return mRemoteTasks.size();
    }

    @NonNull
    public Set<RemoteTaskHierarchy> getTaskHierarchiesByChildTaskKey(@NonNull TaskKey childTaskKey) {
        return mRemoteTaskHierarchies.getByChildTaskKey(childTaskKey);
    }

    @NonNull
    public Set<RemoteTaskHierarchy> getTaskHierarchiesByParentTaskKey(@NonNull TaskKey parentTaskKey) {
        return mRemoteTaskHierarchies.getByParentTaskKey(parentTaskKey);
    }

    @NonNull
    UserData getUserData() {
        return mUserData;
    }
}
