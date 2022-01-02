package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.ProjectUndoData
import com.krystianwsul.common.domain.TaskHierarchyContainer
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.*
import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.cache.InvalidatableCache
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.cache.invalidatableCache
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.task.performIntervalUpdate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

abstract class Project<T : ProjectType>(
    val assignedToHelper: AssignedToHelper,
    userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    val rootTaskProvider: RootTaskProvider,
    val rootModelChangeManager: RootModelChangeManager,
) : Endable,
    JsonTime.CustomTimeProvider,
    JsonTime.ProjectCustomTimeKeyProvider,
    Task.Parent,
    JsonTime.UserCustomTimeProvider by userCustomTimeProvider {

    val clearableInvalidatableManager = ClearableInvalidatableManager()

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

    val startExactTimeStamp by lazy { ExactTimeStamp.Local(projectRecord.startTime) }

    override val endExactTimeStamp get() = projectRecord.endTime?.let { ExactTimeStamp.Local(it) }

    // don't want these to be mutable
    val projectTaskIds: Set<String> get() = _tasks.keys
    val projectTasks: Collection<ProjectTask> get() = _tasks.values

    abstract val customTimes: Collection<Time.Custom.Project<T>>

    val taskHierarchies
        get() = taskHierarchyContainer.all + getAllDependenciesLoadedTasks().flatMap { it.nestedParentTaskHierarchies.values }

    val existingInstances get() = getAllDependenciesLoadedTasks().flatMap { it.existingInstances.values }

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

    fun deleteTask(task: ProjectTask) {
        check(_tasks.containsKey(task.id))

        _tasks.remove(task.id)
    }

    fun deleteTaskHierarchy(taskHierarchy: ProjectTaskHierarchy) {
        taskHierarchyContainer.removeForce(taskHierarchy.id)
        taskHierarchy.invalidateTasks()
    }

    fun getTaskIfPresent(taskKey: TaskKey.Project) = _tasks[taskKey.taskId]

    fun getProjectTaskForce(taskKey: TaskKey.Project) =
        _tasks[taskKey.taskId] ?: throw MissingTaskException(projectKey, taskKey.taskId)

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

    // visible for testing
    val rootTasksCache =
        invalidatableCache<Collection<RootTask>>(clearableInvalidatableManager) { invalidatableCache ->
            val managerRemovable =
                rootModelChangeManager.rootTaskProjectIdInvalidatableManager.addInvalidatable(invalidatableCache)

            val rootTasks = rootTaskProvider.getRootTasksForProject(projectKey)

            val rootTaskRemovables = rootTasks.map {
                it.projectIdCache
                    .invalidatableManager
                    .addInvalidatable(invalidatableCache)
            }

            InvalidatableCache.ValueHolder(rootTasks) {
                managerRemovable.remove()

                rootTaskRemovables.forEach { it.remove() }
            }
        }

    fun getAllDependenciesLoadedTasks(): Collection<Task> {
        DomainThreadChecker.instance.requireDomainThread()

        return _tasks.values.filter { it.dependenciesLoaded } + rootTasksCache.value
    }

    fun setEndExactTimeStamp(now: ExactTimeStamp.Local, projectUndoData: ProjectUndoData, removeInstances: Boolean) {
        requireNotDeleted()

        getAllDependenciesLoadedTasks().filter { it.notDeleted }.forEach {
            it.performIntervalUpdate { setEndData(Task.EndData(now, removeInstances), projectUndoData.taskUndoData) }
        }

        projectUndoData.projectIds.add(projectKey)

        projectRecord.endTime = now.long
        projectRecord.endTimeOffset = now.offset
    }

    fun clearEndExactTimeStamp() {
        requireDeleted()

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

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project) =
        projectRecord.getProjectCustomTimeKey(projectCustomTimeId)

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
        ?.let(::getCustomTime)
        ?: Time.Normal(timePair.hourMinute!!)

    fun getDateTime(instanceScheduleKey: InstanceScheduleKey) =
        DateTime(instanceScheduleKey.scheduleDate, getTime(instanceScheduleKey.scheduleTimePair))

    fun fixNotificationShown(
        shownFactory: Instance.ShownFactory,
        now: ExactTimeStamp.Local,
    ) = getAllDependenciesLoadedTasks().forEach {
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

        val filteredTasks = getAllDependenciesLoadedTasks().asSequence()
            .filter { it.mayHaveRootInstances() }
            .filterSearch(searchData?.searchCriteria?.search, now).map { it.first }
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

    abstract fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, ProjectUser>

    override fun getTask(taskKey: TaskKey) = when (taskKey) {
        is TaskKey.Project -> getProjectTaskForce(taskKey)
        is TaskKey.Root -> rootTaskProvider.getRootTask(taskKey)
    }

    override fun getAllExistingInstances() = rootTaskProvider.getAllExistingInstances()

    private class MissingTaskException(projectId: ProjectKey<*>, taskId: String) :
        Exception("projectId: $projectId, taskId: $taskId")

    interface CustomTimeMigrationHelper {

        fun tryMigrateProjectCustomTime(
            customTime: Time.Custom.Project<*>,
            now: ExactTimeStamp.Local
        ): Time.Custom.User?
    }

    interface RootTaskProvider {

        fun tryGetRootTask(rootTaskKey: TaskKey.Root): RootTask?

        fun getRootTask(rootTaskKey: TaskKey.Root) =
            tryGetRootTask(rootTaskKey) ?: throw MissingRootTaskException(rootTaskKey)

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

        fun getAllExistingInstances(): Sequence<Instance>

        private class MissingRootTaskException(taskKey: TaskKey.Root) : Exception("taskKey: $taskKey")
    }
}
