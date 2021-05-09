package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

abstract class Project<T : ProjectType>(
    val copyScheduleHelper: CopyScheduleHelper,
    val assignedToHelper: AssignedToHelper,
    val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    val rootTaskProvider: RootTaskProvider,
) : Current, JsonTime.CustomTimeProvider, JsonTime.ProjectCustomTimeKeyProvider, Task.Parent {

    abstract val projectRecord: ProjectRecord<T>

    @Suppress("PropertyName")
    protected abstract val _tasks: MutableMap<String, ProjectTask>
    protected abstract val taskHierarchyContainer: TaskHierarchyContainer
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
    val projectTaskIds: Set<String> get() = _tasks.keys
    val projectTasks: Collection<ProjectTask> get() = _tasks.values

    abstract val customTimes: Collection<Time.Custom.Project<T>>

    val taskHierarchies
        get() = taskHierarchyContainer.all + getAllTasks().flatMap { it.nestedParentTaskHierarchies.values }

    val existingInstances get() = getAllTasks().flatMap { it.existingInstances.values }

    protected fun initializeInstanceHierarchyContainers() {
        getAllTasks().forEach {
            it.existingInstances
                .values
                .forEach { it.addToParentInstanceHierarchyContainer() }
        }
    }

    abstract fun createChildTask(
        // todo task edit
        parentTask: ProjectTask,
        now: ExactTimeStamp.Local,
        name: String,
        note: String?,
        image: TaskJson.Image?,
        ordinal: Double?,
    ): ProjectTask

    fun createTaskHierarchy(parentTask: ProjectTask, childTask: ProjectTask, now: ExactTimeStamp.Local): TaskHierarchyKey {
        return childTask.createParentNestedTaskHierarchy(parentTask, now)
    }

    protected abstract fun copyTaskRecord(
        // todo task edit
        oldTask: ProjectTask,
        now: ExactTimeStamp.Local,
        instanceJsons: MutableMap<String, InstanceJson>,
    ): ProjectTaskRecord

    private fun convertScheduleKey(
        // todo task edit
        oldTask: ProjectTask,
        oldScheduleKey: ScheduleKey,
        customTimeMigrationHelper: CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ): ScheduleKey {
        check(oldTask.project != this)

        val (oldScheduleDate, oldScheduleTimePair) = oldScheduleKey

        if (oldScheduleTimePair.customTimeKey == null) return oldScheduleKey

        val convertedTime = when (val customTime = oldTask.project.getCustomTime(oldScheduleTimePair.customTimeKey)) {
            is Time.Custom.Project<*> ->
                getOrCreateCustomTime(oldScheduleDate.dayOfWeek, customTime, customTimeMigrationHelper, now)
            is Time.Custom.User -> customTime
        }

        return ScheduleKey(oldScheduleDate, convertedTime.timePair)
    }

    private fun getOrCreateCustomTime(
        dayOfWeek: DayOfWeek,
        customTime: Time.Custom.Project<*>,
        customTimeMigrationHelper: CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ): Time {
        return customTimeMigrationHelper.tryMigrateProjectCustomTime(customTime, now)
            ?: Time.Normal(customTime.getHourMinute(dayOfWeek))
    }

    fun getOrCopyTime(
        dayOfWeek: DayOfWeek,
        time: Time,
        customTimeMigrationHelper: CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ) = time.let {
        when (it) {
            is Time.Custom.Project<*> -> getOrCreateCustomTime(dayOfWeek, it, customTimeMigrationHelper, now)
            is Time.Custom.User -> it
            is Time.Normal -> it
        }
    }

    private fun getInstanceJson(
        // todo task edit
        instance: Instance,
        newProjectKey: ProjectKey<*>,
        customTimeMigrationHelper: CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ): Pair<InstanceJson, (Map<String, String>) -> Any?> {
        val done = instance.doneOffset

        val instanceDate = instance.instanceDate

        val newInstanceTime = getOrCopyTime(
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
                val newTaskId = taskKeyMap.getValue((oldKey.taskKey as TaskKey.Project).taskId)
                val newTaskKey = TaskKey.Project(newProjectKey, newTaskId)

                instanceJson.parentJson = InstanceJson.ParentJson(InstanceKey(newTaskKey, oldKey.scheduleKey))
            }
        }

        return instanceJson to updater
    }

    fun copyTaskHierarchy(
        // todo task edit
        now: ExactTimeStamp.Local,
        startTaskHierarchy: TaskHierarchy,
        parentTaskId: String,
        childTask: ProjectTask,
    ) {
        childTask.copyParentNestedTaskHierarchy(now, startTaskHierarchy, parentTaskId)
    }

    fun deleteTask(task: ProjectTask) {
        check(_tasks.containsKey(task.id))

        _tasks.remove(task.id)
    }

    fun deleteTaskHierarchy(taskHierarchy: ProjectTaskHierarchy) {
        taskHierarchyContainer.removeForce(taskHierarchy.id)
        taskHierarchy.invalidateTasks()
    }

    fun getTaskIfPresent(taskId: String) = _tasks[taskId]

    fun getTaskIfPresent(taskKey: TaskKey.Project) = getTaskIfPresent(taskKey.taskId)

    fun getProjectTaskForce(taskId: String) = _tasks[taskId] ?: throw MissingTaskException(projectKey, taskId)

    fun getProjectTaskForce(taskKey: TaskKey.Project) = getProjectTaskForce(taskKey.taskId)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey.Project) =
        taskHierarchyContainer.getByChildTaskKey(childTaskKey)

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> {
        val projectTaskHierarchies = taskHierarchyContainer.getByParentTaskKey(parentTaskKey as TaskKey.Project)

        val nestedTaskHierarchies = projectTasks.flatMap {
            it.nestedParentTaskHierarchies.values
        }.filter { it.parentTaskKey == parentTaskKey }

        return projectTaskHierarchies + nestedTaskHierarchies
    }

    fun delete() = projectRecord.delete()

    fun getAllTasks(): Collection<Task> = _tasks.values + rootTaskProvider.getRootTasksForProject(projectKey)

    fun setEndExactTimeStamp(
        now: ExactTimeStamp.Local,
        projectUndoData: ProjectUndoData,
        removeInstances: Boolean,
    ) {
        requireCurrent(now)

        getAllTasks().filter { it.current(now) }.forEach {
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

    fun getProjectTaskHierarchy(id: TaskHierarchyId) = taskHierarchyContainer.getById(id)

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

    fun fixNotificationShown(
        shownFactory: Instance.ShownFactory,
        now: ExactTimeStamp.Local,
    ) = getAllTasks().forEach {
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

        val filteredTasks = getAllTasks().asSequence()
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
        // todo task edit
        now: ExactTimeStamp.Local,
        image: TaskJson.Image?,
        name: String,
        note: String?,
        ordinal: Double?,
    ): ProjectTask

    abstract fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, ProjectUser>

    override fun getTask(taskKey: TaskKey) = when (taskKey) {
        is TaskKey.Project -> getProjectTaskForce(taskKey)
        is TaskKey.Root -> rootTaskProvider.getRootTask(taskKey)
    }

    fun addRootTask(taskKey: TaskKey.Root) {
        projectRecord.rootTaskParentDelegate.addRootTask(taskKey) { updateRootTaskKeys(it) }
    }

    fun removeRootTask(taskKey: TaskKey.Root) {
        projectRecord.rootTaskParentDelegate.removeRootTask(taskKey) { updateRootTaskKeys(it) }
    }

    private fun updateRootTaskKeys(rootTaskKeys: Set<TaskKey.Root>) {
        rootTaskProvider.updateProjectRecord(projectKey, rootTaskKeys)
    }

    private class MissingTaskException(projectId: ProjectKey<*>, taskId: String) :
        Exception("projectId: $projectId, taskId: $taskId")

    interface CustomTimeMigrationHelper { // todo task edit

        fun tryMigrateProjectCustomTime(
            customTime: Time.Custom.Project<*>,
            now: ExactTimeStamp.Local
        ): Time.Custom.User?
    }

    interface RootTaskProvider {

        fun getRootTask(rootTaskKey: TaskKey.Root): RootTask

        fun getRootTasksForProject(projectKey: ProjectKey<*>): Collection<RootTask>

        fun updateProjectRecord(projectKey: ProjectKey<*>, dependentRootTaskKeys: Set<TaskKey.Root>)

        fun updateTaskRecord(taskKey: TaskKey.Root, dependentRootTaskKeys: Set<TaskKey.Root>)

        fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Double?,
        ): Task
    }
}
