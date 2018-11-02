package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteFriendFactory;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.gui.HierarchyData;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskHierarchyKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class DomainFactory {

    private final KotlinDomainFactory kotlinDomainFactory;

    private static void check(boolean value) {
        if (!value) throw new IllegalStateException();
    }

    DomainFactory(@NonNull KotlinDomainFactory kotlinDomainFactory) {
        this.kotlinDomainFactory = kotlinDomainFactory;
    }

    // misc

    // firebase

    // gets

    // sets

    public synchronized void createScheduleJoinRootTask(@NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createScheduleJoinRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());
        check(joinTaskKeys.size() > 1);

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        String joinProjectId = joinProjectIds.get(0);

        final String finalProjectId;
        if (!TextUtils.isEmpty(joinProjectId)) {
            check(TextUtils.isEmpty(projectId));

            finalProjectId = joinProjectId;
        } else if (!TextUtils.isEmpty(projectId)) {
            finalProjectId = projectId;
        } else {
            finalProjectId = null;
        }

        List<Task> joinTasks = Stream.of(joinTaskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        Task newParentTask;
        if (!TextUtils.isEmpty(finalProjectId)) {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(kotlinDomainFactory.getUserInfo() != null);

            newParentTask = kotlinDomainFactory.getRemoteProjectFactory().createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId);
        } else {
            newParentTask = kotlinDomainFactory.localFactory.createScheduleRootTask(kotlinDomainFactory, now, name, scheduleDatas, note);
        }

        joinTasks = Stream.of(joinTasks).map(joinTask -> joinTask.updateProject(now, projectId))
                .collect(Collectors.toList());

        kotlinDomainFactory.joinTasks(newParentTask, joinTasks, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(newParentTask.getRemoteNullableProject());
    }

    Task createChildTask(@NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        check(!TextUtils.isEmpty(name));

        Task parentTask = kotlinDomainFactory.getTaskForce(parentTaskKey);
        check(parentTask.current(now));

        Task childTask = parentTask.createChildTask(now, name, note);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(childTask.getRemoteNullableProject());

        return childTask;
    }

    public synchronized void createChildTask(int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createChildTask(now, dataId, source, parentTaskKey, name, note);
    }

    public synchronized void createJoinChildTask(int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createJoinChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task parentTask = kotlinDomainFactory.getTaskForce(parentTaskKey);
        check(parentTask.current(now));

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        List<Task> joinTasks = Stream.of(joinTaskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        Task childTask = parentTask.createChildTask(now, name, note);

        kotlinDomainFactory.joinTasks(childTask, joinTasks, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(childTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateChildTask(@NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull TaskKey parentTaskKey, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        Task newParentTask = kotlinDomainFactory.getTaskForce(parentTaskKey);
        check(task.current(now));

        task.setName(name, note);

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Stream.of(task.getCurrentSchedules(now))
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            newParentTask.addChild(task, now);
        } else if (oldParentTask != newParentTask) {
            TaskHierarchy oldTaskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
            check(oldTaskHierarchy != null);

            oldTaskHierarchy.setEndExactTimeStamp(now);

            newParentTask.addChild(task, now);
        }

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    public synchronized void setTaskEndTimeStamp(int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamp");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task.setEndExactTimeStamp(now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());
    }

    public synchronized void setInstanceOrdinal(int dataId, @NonNull InstanceKey instanceKey, double ordinal) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceOrdinal");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        instance.setOrdinal(ordinal, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, SaveService.Source.GUI);

        kotlinDomainFactory.notifyCloud(instance.getRemoteNullableProject());
    }

    public synchronized void setTaskHierarchyOrdinal(int dataId, @NonNull HierarchyData hierarchyData) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskHierarchyOrdinal");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject;
        TaskHierarchy taskHierarchy;
        if (hierarchyData.getTaskHierarchyKey() instanceof TaskHierarchyKey.LocalTaskHierarchyKey) {
            TaskHierarchyKey.LocalTaskHierarchyKey localTaskHierarchyKey = (TaskHierarchyKey.LocalTaskHierarchyKey) hierarchyData.getTaskHierarchyKey();

            remoteProject = null;
            taskHierarchy = kotlinDomainFactory.localFactory.getTaskHierarchy(localTaskHierarchyKey);
        } else {
            check(hierarchyData.getTaskHierarchyKey() instanceof TaskHierarchyKey.RemoteTaskHierarchyKey);

            TaskHierarchyKey.RemoteTaskHierarchyKey remoteTaskHierarchyKey = (TaskHierarchyKey.RemoteTaskHierarchyKey) hierarchyData.getTaskHierarchyKey();

            remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(remoteTaskHierarchyKey.getProjectId());
            taskHierarchy = remoteProject.getTaskHierarchy(remoteTaskHierarchyKey.getTaskHierarchyId());
        }

        check(taskHierarchy.current(now));

        taskHierarchy.setOrdinal(hierarchyData.getOrdinal());

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, SaveService.Source.GUI);

        if (remoteProject != null) kotlinDomainFactory.notifyCloud(remoteProject);
    }

    public synchronized void setTaskEndTimeStamps(int dataId, @NonNull SaveService.Source source, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamps");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Task> tasks = Stream.of(taskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        check(Stream.of(tasks)
                .allMatch(task -> task.current(now)));

        Stream.of(tasks)
                .forEach(task -> task.setEndExactTimeStamp(now));

        Set<RemoteProject> remoteProjects = Stream.of(tasks)
                .filter(Task::belongsToRemoteProject)
                .map(Task::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProjects);
    }

    public synchronized int createCustomTime(@NonNull SaveService.Source source, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createCustomTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        check(hourMinutes.get(DayOfWeek.SUNDAY) != null);
        check(hourMinutes.get(DayOfWeek.MONDAY) != null);
        check(hourMinutes.get(DayOfWeek.TUESDAY) != null);
        check(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
        check(hourMinutes.get(DayOfWeek.THURSDAY) != null);
        check(hourMinutes.get(DayOfWeek.FRIDAY) != null);
        check(hourMinutes.get(DayOfWeek.SATURDAY) != null);

        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.createLocalCustomTime(kotlinDomainFactory, name, hourMinutes);

        kotlinDomainFactory.save(0, source);

        return localCustomTime.getId();
    }

    public synchronized void updateCustomTime(int dataId, @NonNull SaveService.Source source, int localCustomTimeId, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateCustomTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(localCustomTimeId);

        localCustomTime.setName(name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = hourMinutes.get(dayOfWeek);
            check(hourMinute != null);

            if (hourMinute.compareTo(localCustomTime.getHourMinute(dayOfWeek)) != 0)
                localCustomTime.setHourMinute(dayOfWeek, hourMinute);
        }

        kotlinDomainFactory.save(dataId, source);
    }

    public synchronized void setCustomTimeCurrent(int dataId, @NonNull SaveService.Source source, @NonNull List<Integer> localCustomTimeIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setCustomTimeCurrent");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!localCustomTimeIds.isEmpty());

        for (int localCustomTimeId : localCustomTimeIds) {
            LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(localCustomTimeId);

            localCustomTime.setCurrent();
        }

        kotlinDomainFactory.save(dataId, source);
    }

    @NonNull
    Task createRootTask(@NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        check(!TextUtils.isEmpty(name));

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = kotlinDomainFactory.localFactory.createLocalTaskHelper(kotlinDomainFactory, name, now, note);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            task = kotlinDomainFactory.getRemoteProjectFactory().createRemoteTaskHelper(now, name, note, projectId);
        }

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createRootTask(int dataId, @NonNull SaveService.Source source, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createRootTask(now, dataId, source, name, note, projectId);
    }

    public synchronized void createJoinRootTask(int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createJoinRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        String joinProjectId = joinProjectIds.get(0);

        final String finalProjectId;
        if (!TextUtils.isEmpty(joinProjectId)) {
            check(TextUtils.isEmpty(projectId));

            finalProjectId = joinProjectId;
        } else if (!TextUtils.isEmpty(projectId)) {
            finalProjectId = projectId;
        } else {
            finalProjectId = null;
        }

        List<Task> joinTasks = Stream.of(joinTaskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        Task newParentTask;
        if (!TextUtils.isEmpty(finalProjectId)) {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(kotlinDomainFactory.getUserInfo() != null);

            newParentTask = kotlinDomainFactory.getRemoteProjectFactory().createRemoteTaskHelper(now, name, note, finalProjectId);
        } else {
            newParentTask = kotlinDomainFactory.localFactory.createLocalTaskHelper(kotlinDomainFactory, name, now, note);
        }

        joinTasks = Stream.of(joinTasks).map(joinTask -> joinTask.updateProject(now, projectId))
                .collect(Collectors.toList());

        kotlinDomainFactory.joinTasks(newParentTask, joinTasks, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(newParentTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateRootTask(int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(now, projectId);

        task.setName(name, note);

        TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
        if (taskHierarchy != null)
            taskHierarchy.setEndExactTimeStamp(now);

        Stream.of(task.getCurrentSchedules(now))
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    Irrelevant updateNotificationsTick(@NonNull ExactTimeStamp now, @NonNull SaveService.Source source, boolean silent) {
        kotlinDomainFactory.updateNotifications(silent, now, new ArrayList<>());

        Irrelevant irrelevant = kotlinDomainFactory.setIrrelevant(now);

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            kotlinDomainFactory.localFactory.deleteInstanceShownRecords(kotlinDomainFactory.getRemoteProjectFactory().getTaskKeys());

        kotlinDomainFactory.save(0, source);

        return irrelevant;
    }

    public synchronized void updateNotificationsTick(@NonNull SaveService.Source source, boolean silent, @NonNull String sourceName) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateNotificationsTick source: " + sourceName);
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        updateNotificationsTick(now, source, silent);
    }

    public synchronized void removeFriends(@NonNull Set<String> keys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.removeFriends");

        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(RemoteFriendFactory.Companion.hasFriends());
        check(!RemoteFriendFactory.Companion.isSaved());

        Stream.of(keys).forEach(friendId -> RemoteFriendFactory.Companion.removeFriend(kotlinDomainFactory.getUserInfo().getKey(), friendId));

        RemoteFriendFactory.Companion.save();
    }

    public synchronized void updateUserInfo(@NonNull SaveService.Source source, @NonNull UserInfo userInfo) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateUserInfo");
        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);

        if (kotlinDomainFactory.getUserInfo().equals(userInfo))
            return;

        kotlinDomainFactory.setUserInfo(userInfo);
        DatabaseWrapper.INSTANCE.setUserInfo(userInfo, kotlinDomainFactory.localFactory.getUuid());

        kotlinDomainFactory.getRemoteProjectFactory().updateUserInfo(userInfo);

        kotlinDomainFactory.save(0, source);
    }

    public synchronized void updateProject(int dataId, @NonNull SaveService.Source source, @NonNull String projectId, @NonNull String name, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateProject");

        check(!TextUtils.isEmpty(projectId));
        check(!TextUtils.isEmpty(name));
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(RemoteFriendFactory.Companion.hasFriends());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

        remoteProject.setName(name);
        remoteProject.updateRecordOf(Stream.of(addedFriends).map(RemoteFriendFactory.Companion::getFriend)
                .collect(Collectors.toSet()), removedFriends);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProject, removedFriends);
    }

    public synchronized void createProject(int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull Set<String> friends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createProject");

        check(!TextUtils.isEmpty(name));
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteRootUser() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<String> recordOf = new HashSet<>(friends);

        String key = kotlinDomainFactory.getUserInfo().getKey();
        check(!recordOf.contains(key));
        recordOf.add(key);

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().createRemoteProject(name, now, recordOf, kotlinDomainFactory.getRemoteRootUser());

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProject);
    }

    public synchronized void setProjectEndTimeStamps(int dataId, @NonNull SaveService.Source source, @NonNull Set<String> projectIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setProjectEndTimeStamps");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(!projectIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<RemoteProject> remoteProjects = Stream.of(projectIds).map(kotlinDomainFactory.getRemoteProjectFactory()::getRemoteProjectForce)
                .collect(Collectors.toSet());

        check(Stream.of(remoteProjects)
                .allMatch(remoteProject -> remoteProject.current(now)));

        Stream.of(remoteProjects)
                .forEach(remoteProject -> remoteProject.setEndExactTimeStamp(now));

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProjects);
    }

    // internal
}