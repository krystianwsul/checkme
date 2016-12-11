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
    private final Map<String, RemoteCustomTime> mRemoteCustomTimes;

    @NonNull
    private final Map<String, RemoteProject> mRemoteProjects;

    public RemoteFactory(@NonNull DomainFactory domainFactory, @NonNull Iterable<DataSnapshot> children, @NonNull UserData userData) {
        mDomainFactory = domainFactory;
        mUserData = userData;

        mRemoteManager = new RemoteManager(domainFactory, children);

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

        mRemoteProjects = Stream.of(mRemoteManager.mRemoteProjectRecords.values())
                .map(remoteProjectRecord -> new RemoteProject(domainFactory, remoteProjectRecord))
                .collect(Collectors.toMap(RemoteProject::getId, remoteProject -> remoteProject));
    }

    @NonNull
    public RemoteTask createScheduleRootTask(@NonNull ExactTimeStamp now, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull Collection<String> friends) {
        RemoteTask remoteTask = createRemoteTaskHelper(now, name, note, friends);

        remoteTask.createSchedules(now, scheduleDatas);

        return remoteTask;
    }

    @NonNull
    private String getProjectName(@NonNull Set<String> recordOf) {
        Assert.assertTrue(mDomainFactory.getFriends() != null);

        Map<String, UserData> lookup = new HashMap<>(mDomainFactory.getFriends());
        lookup.put(mUserData.getKey(), mUserData);

        List<String> names = new ArrayList<>();
        for (String key : recordOf) {
            UserData userData = lookup.get(key);
            Assert.assertTrue(userData != null); // todo what if one of the owners isn't a friend?

            String first = userData.getDisplayName().split(" ")[0];
            Assert.assertTrue(!TextUtils.isEmpty(first));

            names.add(first);
        }

        return TextUtils.join("/", names);
    }

    @NonNull
    public RemoteTask createRemoteTaskHelper(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note, @NonNull Collection<String> friends) {
        TaskJson taskJson = new TaskJson(name, now.getLong(), null, null, null, null, note, Collections.emptyMap());

        Set<String> recordOf = new HashSet<>(friends);
        recordOf.add(mUserData.getKey());

        return getRemoteProjectForce(recordOf, now).newRemoteTask(taskJson);
    }

    @NonNull
    public RemoteProject getRemoteProjectForce(@NonNull Set<String> recordOf, @NonNull ExactTimeStamp now) {
        List<RemoteProject> matches = Stream.of(mRemoteProjects.values())
                .filter(remoteProject -> remoteProject.getRecordOf().equals(recordOf))
                .filter(remoteProject -> remoteProject.getEndExactTimeStamp() == null)
                .collect(Collectors.toList());

        if (!matches.isEmpty()) {
            return matches.get(0);
        } else {
            ProjectJson projectJson = new ProjectJson(getProjectName(recordOf), now.getLong(), null, new HashMap<>(), new HashMap<>());

            RemoteProjectRecord remoteProjectRecord = mRemoteManager.newRemoteProjectRecord(mDomainFactory, new JsonWrapper(recordOf, projectJson));

            RemoteProject remoteProject = new RemoteProject(mDomainFactory, remoteProjectRecord);

            Assert.assertTrue(!mRemoteProjects.containsKey(remoteProject.getId()));

            mRemoteProjects.put(remoteProject.getId(), remoteProject);

            return remoteProject;
        }
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
    public Stream<RemoteTask> getTasks() {
        return Stream.of(mRemoteProjects.values())
                .map(RemoteProject::getRemoteTasks)
                .flatMap(Stream::of);
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
    UserData getUserData() {
        return mUserData;
    }

    @NonNull
    public Collection<RemoteProject> getRemoteProjects() {
        return mRemoteProjects.values();
    }
}
