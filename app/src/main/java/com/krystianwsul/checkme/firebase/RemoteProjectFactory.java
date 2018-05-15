package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.UserInfo;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.firebase.json.CustomTimeJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.ProjectJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.records.RemoteProjectManager;
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

public class RemoteProjectFactory {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final UserInfo mUserInfo;

    @NonNull
    private final RemoteProjectManager mRemoteProjectManager;

    @NonNull
    private final Map<String, RemoteProject> mRemoteProjects;

    @NonNull
    private final String mUuid;

    public RemoteProjectFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children, @NonNull UserInfo userInfo, @NonNull String uuid, @NonNull ExactTimeStamp now) {
        mDomainFactory = domainFactory;
        mUserInfo = userInfo;
        mUuid = uuid;

        mRemoteProjectManager = new RemoteProjectManager(domainFactory, children);

        mRemoteProjects = Stream.of(mRemoteProjectManager.getRemoteProjectRecords().values())
                .map(remoteProjectRecord -> new RemoteProject(domainFactory, remoteProjectRecord, mUserInfo, uuid, now))
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

        return getRemoteProjectForce(projectId).newRemoteTask(taskJson, now);
    }

    @NonNull
    public RemoteProject createRemoteProject(@NonNull String name, @NonNull ExactTimeStamp now, @NonNull Set<String> recordOf, @NonNull RemoteRootUser remoteRootUser) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Set<String> friendIds = new HashSet<>(recordOf);
        friendIds.remove(mUserInfo.getKey());

        Map<String, UserJson> userJsons = mDomainFactory.getUserJsons(friendIds);
        userJsons.put(mUserInfo.getKey(), remoteRootUser.getUserJson());

        ProjectJson projectJson = new ProjectJson(name, now.getLong(), null, new HashMap<>(), new HashMap<>(), new HashMap<>(), userJsons);

        RemoteProjectRecord remoteProjectRecord = mRemoteProjectManager.newRemoteProjectRecord(mDomainFactory, new JsonWrapper(recordOf, projectJson));

        RemoteProject remoteProject = new RemoteProject(mDomainFactory, remoteProjectRecord, mUserInfo, mUuid, now);

        Assert.assertTrue(!mRemoteProjects.containsKey(remoteProject.getId()));

        mRemoteProjects.put(remoteProject.getId(), remoteProject);

        return remoteProject;
    }

    public void save() {
        Assert.assertTrue(!mRemoteProjectManager.isSaved());

        mRemoteProjectManager.save();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSaved() {
        return mRemoteProjectManager.isSaved();
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

            RemoteCustomTime remoteCustomTime = remoteProject.newRemoteCustomTime(customTimeJson);

            localCustomTime.addRemoteCustomTimeRecord(remoteCustomTime.getRemoteCustomTimeRecord());
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
                .map(RemoteProject::getRemoteTasks).flatMap(Stream::of).flatMap(remoteTask -> Stream.of(remoteTask.getExistingInstances().values())).map(instance -> (RemoteInstance) instance)
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

    public void updateUserInfo(@NonNull UserInfo userInfo) {
        Stream.of(mRemoteProjects.values())
                .forEach(remoteProject -> remoteProject.updateUserInfo(userInfo, mUuid));
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
