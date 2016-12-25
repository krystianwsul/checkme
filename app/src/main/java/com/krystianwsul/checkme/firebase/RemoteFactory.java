package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.firebase.json.CustomTimeJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.ProjectJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.firebase.records.RemoteManager;
import com.krystianwsul.checkme.firebase.records.RemoteProjectRecord;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

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
    private final Map<String, RemoteProject> mRemoteProjects;

    public RemoteFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children, @NonNull UserData userData) {
        mDomainFactory = domainFactory;
        mUserData = userData;

        mRemoteManager = new RemoteManager(domainFactory, children);

        mRemoteProjects = Stream.of(mRemoteManager.mRemoteProjectRecords.values())
                .map(remoteProjectRecord -> new RemoteProject(domainFactory, remoteProjectRecord, mUserData))
                .collect(Collectors.toMap(RemoteProject::getId, remoteProject -> remoteProject));
    }

    @NonNull
    public RemoteTask createScheduleRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull String projectId) {
        RemoteTask remoteTask = createRemoteTaskHelper(now, name, note, projectId);

        remoteTask.createSchedules(now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    public RemoteTask createRemoteTaskHelper(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note, @NonNull String projectId) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note, Collections.emptyMap());

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson);
    }

    public void createRemoteProject(@NonNull String name, @NonNull ExactTimeStamp now, @NonNull Set<String> recordOf) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Map<String, UserData> friends = mDomainFactory.getFriends();
        Assert.assertTrue(friends != null);

        Set<UserData> userDatas = new HashSet<>();
        for (String id : recordOf) {
            Assert.assertTrue(!TextUtils.isEmpty(id));

            if (id.equals(mUserData.getKey()))
                continue;

            Assert.assertTrue(friends.containsKey(id));
            UserData userData = friends.get(id);
            Assert.assertTrue(userData != null);

            userDatas.add(userData);
        }

        userDatas.add(mUserData);

        Map<String, UserJson> userJsons = Stream.of(userDatas)
                .map(UserData::toUserJson)
                .collect(Collectors.toMap(userJson -> UserData.getKey(userJson.getEmail()), userJson -> userJson));

        ProjectJson projectJson = new ProjectJson(name, now.getLong(), null, new HashMap<>(), new HashMap<>(), new HashMap<>(), userJsons);

        RemoteProjectRecord remoteProjectRecord = mRemoteManager.newRemoteProjectRecord(mDomainFactory, new JsonWrapper(recordOf, projectJson));

        RemoteProject remoteProject = new RemoteProject(mDomainFactory, remoteProjectRecord, mUserData);

        Assert.assertTrue(!mRemoteProjects.containsKey(remoteProject.getId()));

        mRemoteProjects.put(remoteProject.getId(), remoteProject);
    }

    public void save() {
        Assert.assertTrue(!mRemoteManager.isSaved());

        mRemoteManager.save();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSaved() {
        return mRemoteManager.isSaved();
    }

    @NonNull
    public Stream<RemoteTask> getTasks() {
        return Stream.of(mRemoteProjects.values())
                .map(RemoteProject::getRemoteTasks)
                .flatMap(Stream::of);
    }

    @NonNull
    String getRemoteCustomTimeId(@NonNull CustomTimeKey customTimeKey, @NonNull RemoteProject remoteProject) {
        Assert.assertTrue(customTimeKey.mLocalCustomTimeId != null);
        Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));

        int localCustomTimeId = customTimeKey.mLocalCustomTimeId;

        LocalCustomTime localCustomTime = mDomainFactory.getLocalFactory().getLocalCustomTime(localCustomTimeId);

        if (!localCustomTime.hasRemoteRecord(remoteProject.getId())) {
            CustomTimeJson customTimeJson = new CustomTimeJson(mDomainFactory.getLocalFactory().getUuid(), localCustomTime.getId(), localCustomTime.getName(), localCustomTime.getHourMinute(DayOfWeek.SUNDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.SUNDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.MONDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.MONDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.TUESDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.TUESDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.WEDNESDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.THURSDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.THURSDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.FRIDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.FRIDAY).getMinute(), localCustomTime.getHourMinute(DayOfWeek.SATURDAY).getHour(), localCustomTime.getHourMinute(DayOfWeek.SATURDAY).getMinute());

            RemoteCustomTimeRecord customTimeRecord = remoteProject.newRemoteCustomTimeRecord(customTimeJson);

            localCustomTime.addRemoteCustomTimeRecord(customTimeRecord);
        }

        return localCustomTime.getRemoteId(remoteProject.getId());
    }

    @NonNull
    public RemoteCustomTime getRemoteCustomTime(@NonNull String remoteProjectId, @NonNull String remoteCustomTimeId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteProjectId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId));

        Assert.assertTrue(mRemoteProjects.containsKey(remoteProjectId));

        RemoteProject remoteProject = mRemoteProjects.get(remoteProjectId);
        Assert.assertTrue(remoteProject != null);

        return remoteProject.getRemoteCustomTime(remoteCustomTimeId);
    }

    @NonNull
    public List<RemoteCustomTime> getRemoteCustomTimes() {
        return Stream.of(mRemoteProjects.values())
                .map(RemoteProject::getRemoteCustomTimes)
                .map(Stream::of)
                .flatMap(stream -> stream)
                .collect(Collectors.toList());
    }

    public int getInstanceCount() {
        return Stream.of(mRemoteProjects.values())
                .map(RemoteProject::getRemoteTasks)
                .flatMap(Stream::of)
                .map(remoteTask -> remoteTask.getExistingInstances().size())
                .reduce(0, (x, y) -> x + y);
    }

    @NonNull
    public List<RemoteInstance> getExistingInstances() {
        return Stream.of(mRemoteProjects.values())
                .map(RemoteProject::getRemoteTasks)
                .flatMap(Stream::of)
                .flatMap(remoteTask -> Stream.of(remoteTask.getExistingInstances().values()))
                .collect(Collectors.toList());
    }

    @Nullable
    public RemoteInstance getExistingInstanceIfPresent(@NonNull InstanceKey instanceKey) {
        TaskKey taskKey = instanceKey.mTaskKey;

        if (TextUtils.isEmpty(taskKey.mRemoteTaskId))
            return null;

        RemoteTask remoteTask = getRemoteProjectForce(taskKey).getRemoteTaskIfPresent(taskKey.mRemoteTaskId);
        if (remoteTask == null)
            return null;

        return remoteTask.getExistingInstanceIfPresent(instanceKey.mScheduleKey);
    }

    @NonNull
    private RemoteProject getRemoteProjectForce(@NonNull TaskKey taskKey) {
        RemoteProject remoteProject = getRemoteProjectIfPresent(taskKey);
        Assert.assertTrue(remoteProject != null);

        return remoteProject;
    }

    @Nullable
    private RemoteProject getRemoteProjectIfPresent(@NonNull TaskKey taskKey) {
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteProjectId));
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteTaskId));

        return mRemoteProjects.get(taskKey.mRemoteProjectId);
    }

    @NonNull
    public RemoteTask getTaskForce(@NonNull TaskKey taskKey) {
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteTaskId));

        return getRemoteProjectForce(taskKey).getRemoteTaskForce(taskKey.mRemoteTaskId);
    }

    @Nullable
    public RemoteTask getTaskIfPresent(@NonNull TaskKey taskKey) {
        Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteTaskId));

        RemoteProject remoteProject = getRemoteProjectIfPresent(taskKey);
        if (remoteProject == null)
            return null;

        return remoteProject.getRemoteTaskIfPresent(taskKey.mRemoteTaskId);
    }

    @NonNull
    public Set<TaskKey> getTaskKeys() {
        Set<TaskKey> taskKeys = new HashSet<>();

        for (RemoteProject remoteProject : mRemoteProjects.values()) {
            Assert.assertTrue(remoteProject != null);

            String projectId = remoteProject.getId();

            taskKeys.addAll(Stream.of(remoteProject.getTaskIds())
                    .map(taskId -> new TaskKey(projectId, taskId))
                    .collect(Collectors.toList()));
        }

        return taskKeys;
    }

    public int getTaskCount() {
        return Stream.of(mRemoteProjects.values())
                .map(RemoteProject::getRemoteTasks)
                .map(Collection::size)
                .reduce(0, (x, y) -> x + y);
    }

    @NonNull
    Set<RemoteTaskHierarchy> getTaskHierarchiesByChildTaskKey(@NonNull TaskKey childTaskKey) {
        Assert.assertTrue(!TextUtils.isEmpty(childTaskKey.mRemoteTaskId));

        return getRemoteProjectForce(childTaskKey).getTaskHierarchiesByChildTaskKey(childTaskKey);
    }

    @NonNull
    public Collection<RemoteProject> getRemoteProjects() {
        return mRemoteProjects.values();
    }

    public void updateUserData(@NonNull UserData userData) {
        Stream.of(mRemoteProjects.values())
                .forEach(remoteProject -> remoteProject.updateUserData(userData));
    }

    @NonNull
    public RemoteProject getRemoteProjectForce(@NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));
        Assert.assertTrue(mRemoteProjects.containsKey(projectId));

        RemoteProject remoteProject = mRemoteProjects.get(projectId);
        Assert.assertTrue(remoteProject != null);

        return remoteProject;
    }

    void deleteProject(@NonNull RemoteProject remoteProject) {
        String projectId = remoteProject.getId();

        Assert.assertTrue(mRemoteProjects.containsKey(projectId));
        mRemoteProjects.remove(projectId);
    }
}
