package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteFriendFactory;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.firebase.RemoteProjectUser;
import com.krystianwsul.checkme.firebase.RemoteTask;
import com.krystianwsul.checkme.gui.HierarchyData;
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskHierarchyKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel;
import com.krystianwsul.checkme.viewmodels.FriendListViewModel;
import com.krystianwsul.checkme.viewmodels.MainViewModel;
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel;
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel;
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel;
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel;
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel;
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    @NonNull
    public synchronized ShowTaskInstancesViewModel.Data getShowTaskInstancesData(@NonNull TaskKey taskKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.getShowTaskInstancesData");

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

        Collection<Instance> existingInstances = task.getExistingInstances().values();
        List<Instance> pastInstances = task.getInstances(null, now, now);

        Set<Instance> allInstances = new HashSet<>(existingInstances);
        allInstances.addAll(pastInstances);

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = Stream.of(allInstances)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> {
                    Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(instance, now);

                    HierarchyData hierarchyData;
                    if (task.isRootTask(now)) {
                        hierarchyData = null;
                    } else {
                        TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
                        check(taskHierarchy != null);

                        hierarchyData = new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal());
                    }

                    return new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, hierarchyData, instance.getOrdinal());
                }, HashMap::new));

        return new ShowTaskInstancesViewModel.Data(new GroupListFragment.DataWrapper(customTimeDatas, task.current(now), null, null, instanceDatas));
    }

    @NonNull
    public synchronized ShowNotificationGroupViewModel.Data getShowNotificationGroupData(@NonNull Set<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.getShowNotificationGroupData");

        check(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = kotlinDomainFactory.getInstance(instanceKey);

            if (instance.isRootInstance(now))
                instances.add(instance);
        }

        Collections.sort(instances, (lhs, rhs) -> lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime()));

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(instance, now);
            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, null, instance.getOrdinal());
            Stream.of(children.values()).forEach(child -> child.setInstanceDataParent(instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, null, null, instanceDatas);

        Stream.of(instanceDatas.values()).forEach(instanceData -> instanceData.setInstanceDataParent(dataWrapper));

        return new ShowNotificationGroupViewModel.Data(dataWrapper);
    }

    @NonNull
    public synchronized ShowInstanceViewModel.Data getShowInstanceData(@NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.getShowInstanceData");

        Task task = kotlinDomainFactory.getTaskIfPresent(instanceKey.getTaskKey());
        if (task == null) return new ShowInstanceViewModel.Data(null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);
        if (!task.current(now) && !instance.exists()) return new ShowInstanceViewModel.Data(null);

        return new ShowInstanceViewModel.Data(new ShowInstanceViewModel.InstanceData(instance.getName(), instance.getDisplayText(now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), instance.exists(), kotlinDomainFactory.getGroupListData(instance, task, now)));
    }

    @NonNull
    kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>> getScheduleDatas(List<Schedule> schedules, ExactTimeStamp now) {
        Map<CustomTimeKey, CustomTime> customTimes = new HashMap<>();

        Map<CreateTaskViewModel.ScheduleData, List<Schedule>> scheduleDatas = new HashMap<>();

        Map<TimePair, List<WeeklySchedule>> weeklySchedules = new HashMap<>();

        for (Schedule schedule : schedules) {
            check(schedule != null);
            check(schedule.current(now));

            switch (schedule.getScheduleType()) {
                case SINGLE: {
                    SingleSchedule singleSchedule = (SingleSchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.SingleScheduleData(singleSchedule.getDate(), singleSchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = singleSchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                case DAILY: {
                    throw new UnsupportedOperationException();
                }
                case WEEKLY: {
                    WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                    TimePair timePair = weeklySchedule.getTimePair();
                    if (!weeklySchedules.containsKey(timePair))
                        weeklySchedules.put(timePair, new ArrayList<>());
                    weeklySchedules.get(timePair).add(weeklySchedule);

                    CustomTimeKey customTimeKey = weeklySchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_DAY: {
                    MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData(monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), monthlyDaySchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyDaySchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData(monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek(), monthlyWeekSchedule.getBeginningOfMonth(), monthlyWeekSchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyWeekSchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
        }

        for (Map.Entry<TimePair, List<WeeklySchedule>> entry : weeklySchedules.entrySet()) {
            Set<DayOfWeek> daysOfWeek = Stream.of(entry.getValue())
                    .map(WeeklySchedule::getDaysOfWeek)
                    .flatMap(Stream::of)
                    .collect(Collectors.toSet());
            scheduleDatas.put(new CreateTaskViewModel.ScheduleData.WeeklyScheduleData(daysOfWeek, entry.getKey()), new ArrayList<>(entry.getValue()));
        }

        return new kotlin.Pair<>(customTimes, scheduleDatas);
    }

    @NonNull
    public synchronized CreateTaskViewModel.Data getCreateTaskData(@Nullable TaskKey taskKey, @Nullable List<TaskKey> joinTaskKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.getCreateTaskData");

        check(taskKey == null || joinTaskKeys == null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> customTimes = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        List<TaskKey> excludedTaskKeys = new ArrayList<>();
        if (taskKey != null)
            excludedTaskKeys.add(taskKey);
        else if (joinTaskKeys != null)
            excludedTaskKeys.addAll(joinTaskKeys);

        CreateTaskViewModel.TaskData taskData = null;
        Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> parentTreeDatas;
        if (taskKey != null) {
            Task task = kotlinDomainFactory.getTaskForce(taskKey);

            CreateTaskViewModel.ParentKey.TaskParentKey taskParentKey;
            List<CreateTaskViewModel.ScheduleData> scheduleDatas = null;

            if (task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);

                taskParentKey = null;

                if (!schedules.isEmpty()) {
                    kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>> pair = getScheduleDatas(schedules, now);
                    customTimes.putAll(pair.getFirst());
                    scheduleDatas = new ArrayList<>(pair.getSecond().keySet());
                }
            } else {
                Task parentTask = task.getParentTask(now);
                check(parentTask != null);

                taskParentKey = new CreateTaskViewModel.ParentKey.TaskParentKey(parentTask.getTaskKey());
            }

            RemoteProject remoteProject = task.getRemoteNullableProject();
            String projectName = null;
            if (remoteProject != null)
                projectName = remoteProject.getName();

            taskData = new CreateTaskViewModel.TaskData(task.getName(), taskParentKey, scheduleDatas, task.getNote(), projectName);

            if (task instanceof RemoteTask) {
                RemoteTask remoteTask = (RemoteTask) task;

                parentTreeDatas = kotlinDomainFactory.getProjectTaskTreeDatas(now, remoteTask.getRemoteProject(), excludedTaskKeys);
            } else {
                check(task instanceof LocalTask);

                parentTreeDatas = kotlinDomainFactory.getParentTreeDatas(now, excludedTaskKeys);
            }
        } else {
            String projectId = null;
            if (joinTaskKeys != null) {
                check(joinTaskKeys.size() > 1);

                List<String> projectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                        .distinct()
                        .collect(Collectors.toList());

                check(projectIds.size() == 1);

                projectId = projectIds.get(0);
            }

            if (!TextUtils.isEmpty(projectId)) {
                check(kotlinDomainFactory.getRemoteProjectFactory() != null);

                RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

                parentTreeDatas = kotlinDomainFactory.getProjectTaskTreeDatas(now, remoteProject, excludedTaskKeys);
            } else {
                parentTreeDatas = kotlinDomainFactory.getParentTreeDatas(now, excludedTaskKeys);
            }
        }

        @SuppressLint("UseSparseArrays") HashMap<CustomTimeKey, CreateTaskViewModel.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new CreateTaskViewModel.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new CreateTaskViewModel.Data(taskData, parentTreeDatas, customTimeDatas);
    }

    @NonNull
    public synchronized ShowTaskViewModel.Data getShowTaskData(@NonNull TaskKey taskKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.getShowTaskData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        List<TaskListFragment.ChildTaskData> childTaskDatas = Stream.of(task.getChildTaskHierarchies(now))
                .map(taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(now), kotlinDomainFactory.getTaskListChildTaskDatas(childTask, now), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey(), new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()));
                })
                .collect(Collectors.toList());
        Collections.sort(childTaskDatas, TaskListFragment.ChildTaskData::compareTo);

        return new ShowTaskViewModel.Data(task.getName(), task.getScheduleText(now), new TaskListFragment.TaskData(childTaskDatas, task.getNote()), !task.getExistingInstances().isEmpty());
    }

    @NonNull
    public synchronized MainViewModel.Data getMainData() {
        MyCrashlytics.INSTANCE.log("DomainFactory.getMainData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        return new MainViewModel.Data(kotlinDomainFactory.getMainData(now));
    }

    @NonNull
    public synchronized ProjectListViewModel.Data getProjectListData() {
        MyCrashlytics.INSTANCE.log("DomainFactory.getProjectListData");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        TreeMap<String, ProjectListViewModel.ProjectData> projectDatas = Stream.of(kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjects().values())
                .filter(remoteProject -> remoteProject.current(now))
                .collect(Collectors.toMap(RemoteProject::getId, remoteProject -> {
                    String users = Stream.of(remoteProject.getUsers())
                            .map(RemoteProjectUser::getName)
                            .collect(Collectors.joining(", "));

                    return new ProjectListViewModel.ProjectData(remoteProject.getId(), remoteProject.getName(), users);
                }, TreeMap::new));

        return new ProjectListViewModel.Data(projectDatas);
    }

    @NonNull
    public synchronized FriendListViewModel.Data getFriendListData() {
        MyCrashlytics.INSTANCE.log("DomainFactory.getFriendListData");

        check(RemoteFriendFactory.Companion.hasFriends());

        Set<FriendListViewModel.UserListData> userListDatas = Stream.of(RemoteFriendFactory.Companion.getFriends()).map(remoteRootUser -> new FriendListViewModel.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId()))
                .collect(Collectors.toSet());

        return new FriendListViewModel.Data(userListDatas);
    }

    @NonNull
    public synchronized ShowProjectViewModel.Data getShowProjectData(@Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.getShowProjectData");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(RemoteFriendFactory.Companion.hasFriends());

        Map<String, ShowProjectViewModel.UserListData> friendDatas = Stream.of(RemoteFriendFactory.Companion.getFriends()).map(remoteRootUser -> new ShowProjectViewModel.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId())).collect(Collectors.toMap(ShowProjectViewModel.UserListData::getId, userData -> userData));

        String name;
        Set<ShowProjectViewModel.UserListData> userListDatas;
        if (!TextUtils.isEmpty(projectId)) {
            RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

            name = remoteProject.getName();

            userListDatas = Stream.of(remoteProject.getUsers()).filterNot(remoteUser -> remoteUser.getId().equals(kotlinDomainFactory.getUserInfo().getKey())).map(remoteUser -> new ShowProjectViewModel.UserListData(remoteUser.getName(), remoteUser.getEmail(), remoteUser.getId()))
                    .collect(Collectors.toSet());
        } else {
            name = null;
            userListDatas = new HashSet<>();
        }

        return new ShowProjectViewModel.Data(name, userListDatas, friendDatas);
    }

    // sets

    public synchronized void setInstanceDateTime(int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceDateTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesDateTime(int dataId, @NonNull SaveService.Source source, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDateTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Instance> instances = Stream.of(instanceKeys).map(kotlinDomainFactory::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances)
                .forEach(instance -> instance.setInstanceDateTime(instanceDate, instanceTimePair, now));

        Set<RemoteProject> remoteProjects = Stream.of(instances)
                .filter(Instance::belongsToRemoteProject)
                .map(Instance::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProjects);
    }

    public synchronized void setInstanceAddHourService(@NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourService");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(0, source);

        kotlinDomainFactory.notifyCloud(instance.getRemoteNullableProject());
    }

    public synchronized void setInstanceAddHourActivity(int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourActivity");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesAddHourActivity(int dataId, @NonNull SaveService.Source source, @NonNull Collection<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourActivity");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        List<Instance> instances = Stream.of(instanceKeys).map(kotlinDomainFactory::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances).forEach(instance -> instance.setInstanceDateTime(date, new TimePair(hourMinute), now));

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        @SuppressWarnings("Convert2MethodRef") Set<RemoteProject> remoteProjects = Stream.of(instances).map(Instance::getRemoteNullableProject)
                .filter(remoteProject -> remoteProject != null)
                .collect(Collectors.toSet());

        kotlinDomainFactory.notifyCloud(remoteProjects);
    }

    public synchronized void setInstanceNotificationDone(@NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceNotificationDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(0, source);

        kotlinDomainFactory.notifyCloud(instance.getRemoteNullableProject());
    }

    @NonNull
    public synchronized ExactTimeStamp setInstancesDone(int dataId, @NonNull SaveService.Source source, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Instance> instances = Stream.of(instanceKeys).map(kotlinDomainFactory::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances)
                .forEach(instance -> instance.setDone(true, now));

        Set<RemoteProject> remoteProjects = Stream.of(instances)
                .filter(Instance::belongsToRemoteProject)
                .map(Instance::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProjects);

        return now;
    }

    public synchronized ExactTimeStamp setInstanceDone(int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, boolean done) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = kotlinDomainFactory.setInstanceDone(now, dataId, source, instanceKey, done);

        return instance.getDone();
    }

    public synchronized void setInstancesNotified(@NonNull SaveService.Source source, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesNotified");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        for (InstanceKey instanceKey : instanceKeys)
            kotlinDomainFactory.setInstanceNotified(instanceKey, now);

        kotlinDomainFactory.save(0, source);
    }

    public synchronized void setInstanceNotified(int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceNotified");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        kotlinDomainFactory.setInstanceNotified(instanceKey, ExactTimeStamp.Companion.getNow());

        kotlinDomainFactory.save(dataId, source);
    }

    @NonNull
    Task createScheduleRootTask(@NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = kotlinDomainFactory.localFactory.createScheduleRootTask(kotlinDomainFactory, now, name, scheduleDatas, note);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            task = kotlinDomainFactory.getRemoteProjectFactory().createScheduleRootTask(now, name, scheduleDatas, note, projectId);
        }

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createScheduleRootTask(int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createScheduleRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createScheduleRootTask(now, dataId, source, name, scheduleDatas, note, projectId);
    }

    @NonNull
    TaskKey updateScheduleTask(@NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(now, projectId);

        task.setName(name, note);

        if (!task.isRootTask(now)) {
            TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
            check(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        task.updateSchedules(scheduleDatas, now);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    public synchronized TaskKey updateScheduleTask(int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateScheduleTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        return updateScheduleTask(now, dataId, source, taskKey, name, scheduleDatas, note, projectId);
    }

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