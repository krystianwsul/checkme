package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.*
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.models.customtime.PrivateCustomTime
import com.krystianwsul.common.firebase.models.customtime.SharedCustomTime
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

abstract class Project<T : ProjectType>(
        val copyScheduleHelper: CopyScheduleHelper<T>,
        val assignedToHelper: AssignedToHelper,
        val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Current, JsonTime.CustomTimeProvider<T>, JsonTime.ProjectCustomTimeKeyProvider {

    abstract val projectRecord: ProjectRecord<T>

    @Suppress("PropertyName")
    protected abstract val _tasks: MutableMap<String, Task>
    protected abstract val taskHierarchyContainer: TaskHierarchyContainer<T>
    protected abstract val remoteCustomTimes: Map<out CustomTimeId.Project, Time.Custom.Project<T>>

    abstract val projectKey: ProjectKey<T>

    var name
        get() = projectRecord.name
        set(name) {
            check(name.isNotEmpty())

            projectRecord.name = name
        }

    override val startExactTimeStamp by lazy { ExactTimeStamp.Local(projectRecord.startTime) }

    override val endExactTimeStamp get() = projectRecord.endTime?.let { ExactTimeStamp.Local(it) }

    // don't want these to be mutable
    val taskIds: Set<String> get() = _tasks.keys
    val tasks: Collection<Task> get() = _tasks.values

    abstract val customTimes: Collection<Time.Custom.Project<T>>

    val taskHierarchies: Collection<TaskHierarchy>
        get() =
            taskHierarchyContainer.all + tasks.flatMap { it.nestedParentTaskHierarchies.values }

    val existingInstances get() = tasks.flatMap { it.existingInstances.values }

    protected fun initializeInstanceHierarchyContainers() {
        tasks.forEach {
            it.existingInstances
                    .values
                    .forEach { it.addToParentInstanceHierarchyContainer() }
        }
    }

    abstract fun createChildTask(
            parentTask: Task,
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): Task

    fun createTaskHierarchy(parentTask: Task, childTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey {
        return if (TaskHierarchy.WRITE_NESTED_TASK_HIERARCHIES) {
            childTask.createParentNestedTaskHierarchy(parentTask, now)
        } else {
            val taskHierarchyJson = ProjectTaskHierarchyJson(
                    parentTask.id,
                    childTask.id,
                    now.long,
                    now.offset,
            )

            val taskHierarchyRecord = projectRecord.newTaskHierarchyRecord(taskHierarchyJson)

            val taskHierarchy = ProjectTaskHierarchy(this, taskHierarchyRecord)

            taskHierarchyContainer.add(taskHierarchy.id, taskHierarchy)
            taskHierarchy.invalidateTasks()

            return taskHierarchy.taskHierarchyKey
        }
    }

    protected abstract fun copyTaskRecord(
            oldTask: Task,
            now: ExactTimeStamp.Local,
            instanceJsons: MutableMap<String, InstanceJson>,
    ): ProjectTaskRecord

    private fun convertScheduleKey(
            userInfo: UserInfo,
            oldTask: Task,
            oldScheduleKey: ScheduleKey,
            customTimeMigrationHelper: CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ): ScheduleKey {
        check(oldTask.project != this)

        val (oldScheduleDate, oldScheduleTimePair) = oldScheduleKey

        if (oldScheduleTimePair.customTimeKey == null) return oldScheduleKey

        val convertedTime = when (val customTime = oldTask.project.getCustomTime(oldScheduleTimePair.customTimeKey)) {
            is Time.Custom.Project<*> -> {
                val ownerKey = when (customTime) {
                    is PrivateCustomTime -> userInfo.key
                    is SharedCustomTime -> customTime.ownerKey!!
                    else -> throw IllegalStateException()
                }

                getOrCreateCustomTime(ownerKey, oldScheduleDate.dayOfWeek, customTime, customTimeMigrationHelper, now)
            }
            is Time.Custom.User -> customTime
        }

        return ScheduleKey(oldScheduleDate, convertedTime.timePair)
    }

    private class InstanceConversionData(
            val newInstanceJson: InstanceJson,
            val newScheduleKey: ScheduleKey,
            val updater: (Map<String, String>) -> Any?,
    )

    @Suppress("ConstantConditionIf")
    fun copyTask(
            deviceDbInfo: DeviceDbInfo,
            oldTask: Task,
            instances: Collection<Instance>,
            now: ExactTimeStamp.Local,
            newProjectKey: ProjectKey<*>,
            customTimeMigrationHelper: CustomTimeMigrationHelper,
    ): Pair<Task, List<(Map<String, String>) -> Any?>> {
        val instanceDatas = instances.map { oldInstance ->
            val (newInstance, updater) = getInstanceJson(
                    deviceDbInfo.key,
                    oldInstance,
                    newProjectKey,
                    customTimeMigrationHelper,
                    now,
            )

            val newScheduleKey = convertScheduleKey(
                    deviceDbInfo.userInfo,
                    oldTask,
                    oldInstance.scheduleKey,
                    customTimeMigrationHelper,
                    now,
            )

            InstanceConversionData(newInstance, newScheduleKey, updater)
        }

        // todo migrate tasks this just makes a bigger mess of things
        @Suppress("SimplifyBooleanWithConstants")
        val instanceJsons = if (true) {
            mutableMapOf()
        } else {
            instanceDatas.associate {
                InstanceRecord.scheduleKeyToString(it.newScheduleKey) to it.newInstanceJson
            }.toMutableMap()
        }

        val taskRecord = copyTaskRecord(oldTask, now, instanceJsons)

        val newTask = Task(this, taskRecord)
        check(!_tasks.containsKey(newTask.id))

        _tasks[newTask.id] = newTask

        val currentSchedules = oldTask.getCurrentScheduleIntervals(now).map { it.schedule }
        val currentNoScheduleOrParent = oldTask.getCurrentNoScheduleOrParent(now)?.noScheduleOrParent

        if (currentSchedules.isNotEmpty()) {
            check(currentNoScheduleOrParent == null)

            newTask.copySchedules(deviceDbInfo, now, currentSchedules, customTimeMigrationHelper)
        } else {
            currentNoScheduleOrParent?.let { newTask.setNoScheduleOrParent(now) }
        }

        return newTask to instanceDatas.map { it.updater }
    }

    private fun getOrCreateCustomTime(
            ownerKey: UserKey,
            dayOfWeek: DayOfWeek,
            customTime: Time.Custom.Project<*>,
            customTimeMigrationHelper: CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ): Time {
        return if (Time.Custom.User.WRITE_USER_CUSTOM_TIMES) {
            customTimeMigrationHelper.tryMigrateProjectCustomTime(customTime, now)
                    ?: Time.Normal(customTime.getHourMinute(dayOfWeek))
        } else {
            getOrCreateCustomTimeOld(ownerKey, customTime)
        }
    }

    protected abstract fun getOrCreateCustomTimeOld(
            ownerKey: UserKey,
            customTime: Time.Custom.Project<*>,
    ): Time.Custom.Project<T>

    fun getOrCopyTime(
            ownerKey: UserKey,
            dayOfWeek: DayOfWeek,
            time: Time,
            customTimeMigrationHelper: CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ) = time.let {
        when (it) {
            is Time.Custom.Project<*> -> getOrCreateCustomTime(ownerKey, dayOfWeek, it, customTimeMigrationHelper, now)
            is Time.Custom.User -> it
            is Time.Normal -> it
        }
    }

    private fun getInstanceJson(
            ownerKey: UserKey,
            instance: Instance,
            newProjectKey: ProjectKey<*>,
            customTimeMigrationHelper: CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ): Pair<InstanceJson, (Map<String, String>) -> Any?> {
        val done = instance.doneOffset

        val instanceDate = instance.instanceDate

        val newInstanceTime = getOrCopyTime(
                ownerKey,
                instanceDate.dayOfWeek,
                instance.instanceTime,
                customTimeMigrationHelper,
                now,
        )

        val instanceTimeString = JsonTime.fromTime(newInstanceTime).toJson()

        val parentState = instance.parentState

        val instanceJson = InstanceJson(
                done?.long,
                done?.offset,
                instanceDate.toJson(),
                instanceTimeString,
                parentJson = parentState.parentInstanceKey?.let(InstanceJson::ParentJson),
                noParent = parentState.noParent,
        )

        val updater = { taskKeyMap: Map<String, String> ->
            parentState.parentInstanceKey?.let { oldKey ->
                val newTaskId = taskKeyMap.getValue((oldKey.taskKey as TaskKey.Project).taskId) // todo task convert
                val newTaskKey = TaskKey.Project(newProjectKey, newTaskId)

                instanceJson.parentJson = InstanceJson.ParentJson(InstanceKey(newTaskKey, oldKey.scheduleKey))
            }
        }

        return instanceJson to updater
    }

    fun <V : TaskHierarchy> copyTaskHierarchy(
            now: ExactTimeStamp.Local,
            startTaskHierarchy: V,
            parentTaskId: String,
            childTask: Task,
    ) {
        if (TaskHierarchy.WRITE_NESTED_TASK_HIERARCHIES) {
            childTask.copyParentNestedTaskHierarchy(now, startTaskHierarchy, parentTaskId)
        } else {
            check(parentTaskId.isNotEmpty())

            val taskHierarchyJson = ProjectTaskHierarchyJson(
                    parentTaskId,
                    childTask.id,
                    now.long,
                    now.offset,
                    startTaskHierarchy.endExactTimeStampOffset?.long,
                    startTaskHierarchy.endExactTimeStampOffset?.offset,
            )

            val taskHierarchyRecord = projectRecord.newTaskHierarchyRecord(taskHierarchyJson)
            val taskHierarchy = ProjectTaskHierarchy(this, taskHierarchyRecord)

            taskHierarchyContainer.add(taskHierarchy.id, taskHierarchy)
            taskHierarchy.invalidateTasks()
        }
    }

    fun deleteTask(task: Task) {
        check(_tasks.containsKey(task.id))

        _tasks.remove(task.id)
    }

    fun deleteTaskHierarchy(taskHierarchy: ProjectTaskHierarchy) {
        taskHierarchyContainer.removeForce(taskHierarchy.id)
        taskHierarchy.invalidateTasks()
    }

    fun getTaskIfPresent(taskId: String) = _tasks[taskId]

    fun getTaskForce(taskId: String) = _tasks[taskId]
            ?: throw MissingTaskException(projectKey, taskId)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<ProjectTaskHierarchy> { // todo task after model
        return taskHierarchyContainer.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> { // todo task after model
        val projectTaskHierarchies = taskHierarchyContainer.getByParentTaskKey(parentTaskKey)

        val nestedTaskHierarchies = tasks.flatMap {
            it.nestedParentTaskHierarchies.values
        }.filter { it.parentTaskKey == parentTaskKey }

        return projectTaskHierarchies + nestedTaskHierarchies
    }

    fun delete(parent: Parent) {
        parent.deleteProject(this)

        projectRecord.delete()
    }

    fun setEndExactTimeStamp(
            now: ExactTimeStamp.Local,
            projectUndoData: ProjectUndoData,
            removeInstances: Boolean,
    ) {
        requireCurrent(now)

        _tasks.values
                .filter { it.current(now) }
                .forEach {
                    it.setEndData(Task.EndData(now, removeInstances), projectUndoData.taskUndoData)
                }

        projectUndoData.projectIds.add(projectKey)

        projectRecord.endTime = now.long
        projectRecord.endTimeOffset = now.offset
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp.Local) {
        requireNotCurrent(now)

        projectRecord.endTime = null
        projectRecord.endTimeOffset = null
    }

    fun getProjectTaskHierarchy(id: String) = taskHierarchyContainer.getById(id)

    abstract fun deleteCustomTime(remoteCustomTime: Time.Custom.Project<T>)

    abstract fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<T>): Time.Custom.Project<T>

    fun getUntypedProjectCustomTime(projectCustomTimeId: CustomTimeId.Project) =
            getProjectCustomTime(projectCustomTimeId)

    fun getUntypedProjectCustomTime(projectCustomTimeId: String) =
            getProjectCustomTime(projectRecord.getProjectCustomTimeId(projectCustomTimeId))

    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User) =
            userCustomTimeProvider.getUserCustomTime(userCustomTimeKey)

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project) =
            projectRecord.getProjectCustomTimeKey(projectCustomTimeId)

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
            ?.let(::getCustomTime)
            ?: Time.Normal(timePair.hourMinute!!)

    fun getDateTime(scheduleKey: ScheduleKey) =
            DateTime(scheduleKey.scheduleDate, getTime(scheduleKey.scheduleTimePair))

    fun convertRemoteToRemoteHelper(
            now: ExactTimeStamp.Local,
            remoteToRemoteConversion: RemoteToRemoteConversion,
            startTask: Task,
    ) {
        if (remoteToRemoteConversion.startTasks.containsKey(startTask.id)) return

        remoteToRemoteConversion.startTasks[startTask.id] = Pair(
                startTask,
                startTask.existingInstances
                        .values
                        .filter {
                            listOf(
                                    it.scheduleDateTime,
                                    it.instanceDateTime,
                            ).maxOrNull()!!.toLocalExactTimeStamp() >= now
                        }
        )

        val childTaskHierarchies = startTask.getChildTaskHierarchies(now)

        remoteToRemoteConversion.startTaskHierarchies.addAll(childTaskHierarchies)

        childTaskHierarchies.map { it.childTask }.forEach {
            it.requireCurrent(now)

            convertRemoteToRemoteHelper(now, remoteToRemoteConversion, it)
        }
    }

    fun fixNotificationShown(
            shownFactory: Instance.ShownFactory,
            now: ExactTimeStamp.Local,
    ) = tasks.forEach {
        it.existingInstances
                .values
                .forEach { it.fixNotificationShown(shownFactory, now) }
    }

    data class SearchData(val searchCriteria: SearchCriteria, val myUser: MyUser)

    fun getRootInstances(
            startExactTimeStamp: ExactTimeStamp.Offset?,
            endExactTimeStamp: ExactTimeStamp.Offset?,
            now: ExactTimeStamp.Local,
            searchData: SearchData? = null,
            filterVisible: Boolean = true,
    ): Sequence<Instance> {
        check(startExactTimeStamp == null || endExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        InterruptionChecker.throwIfInterrupted()

        val filteredTasks = tasks.asSequence()
                .filter { it.mayHaveRootInstances() }
                .filterQuery(searchData?.searchCriteria?.query).map { it.first }
                .toList()

        val instanceSequences = filteredTasks.map {
            it.getInstances(
                    startExactTimeStamp,
                    endExactTimeStamp,
                    now,
                    onlyRoot = true,
                    filterVisible = filterVisible,
            )
        }

        return combineInstanceSequences(instanceSequences).let { sequence ->
            InterruptionChecker.throwIfInterrupted()

            searchData?.let { sequence.filterSearchCriteria(it.searchCriteria, now, it.myUser) } ?: sequence
        }.let { instances ->
            InterruptionChecker.throwIfInterrupted()

            if (filterVisible) {
                instances.filter { instance ->
                    InterruptionChecker.throwIfInterrupted()

                    instance.isVisible(
                            now,
                            Instance.VisibilityOptions(hack24 = true, assumeRoot = true)
                    )
                }
            } else {
                instances
            }
        }
    }

    fun fixOffsets() {
        if (projectRecord.startTimeOffset == null) projectRecord.startTimeOffset = startExactTimeStamp.offset

        endExactTimeStamp?.let {
            if (projectRecord.endTimeOffset == null) projectRecord.endTimeOffset = it.offset
        }

        _tasks.values.forEach { it.fixOffsets() }
    }

    abstract fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Double?,
    ): Task

    abstract fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, ProjectUser>

    fun getInstance(instanceKey: InstanceKey) = getTaskForce((instanceKey.taskKey as TaskKey.Project).taskId).getInstance( // todo task after model
            DateTime(
                    instanceKey.scheduleKey.scheduleDate,
                    getTime(instanceKey.scheduleKey.scheduleTimePair),
            )
    )

    private class MissingTaskException(projectId: ProjectKey<*>, taskId: String) :
            Exception("projectId: $projectId, taskId: $taskId")

    interface Parent {

        fun deleteProject(project: Project<*>) // todo this is never implemented, just get rid of it
    }

    interface CustomTimeMigrationHelper {

        fun tryMigrateProjectCustomTime(customTime: Time.Custom.Project<*>, now: ExactTimeStamp.Local): Time.Custom.User?
    }
}