package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.domain.*
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.TaskHierarchyJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.interrupt.InterruptionChecker
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

abstract class Project<T : ProjectType>(
        val copyScheduleHelper: CopyScheduleHelper<T>,
        val assignedToHelper: AssignedToHelper<T>,
        val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Current, JsonTime.CustomTimeProvider<T>, JsonTime.ProjectCustomTimeKeyProvider<T> {

    abstract val projectRecord: ProjectRecord<T>

    @Suppress("PropertyName")
    protected abstract val _tasks: MutableMap<String, Task<T>>
    protected abstract val taskHierarchyContainer: TaskHierarchyContainer<T>
    protected abstract val remoteCustomTimes: Map<out CustomTimeId.Project<T>, Time.Custom.Project<T>>

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
    val tasks: Collection<Task<T>> get() = _tasks.values

    abstract val customTimes: Collection<Time.Custom.Project<T>>

    val taskHierarchies get() = taskHierarchyContainer.all

    val existingInstances get() = tasks.flatMap { it.existingInstances.values }

    protected fun initializeInstanceHierarchyContainers() {
        tasks.forEach {
            it.existingInstances
                    .values
                    .forEach { it.addToParentInstanceHierarchyContainer() }
        }
    }

    abstract fun createChildTask(
            parentTask: Task<T>,
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ): Task<T>

    fun createTaskHierarchy(
            parentTask: Task<T>,
            childTask: Task<T>,
            now: ExactTimeStamp.Local,
    ): TaskHierarchyKey {
        val taskHierarchyJson = TaskHierarchyJson(
                parentTask.id,
                childTask.id,
                now.long,
                now.offset
        )

        val taskHierarchyRecord = projectRecord.newTaskHierarchyRecord(taskHierarchyJson)

        val taskHierarchy = TaskHierarchy(this, taskHierarchyRecord)

        taskHierarchyContainer.add(taskHierarchy.id, taskHierarchy)
        taskHierarchy.invalidateTasks()

        return taskHierarchy.taskHierarchyKey
    }

    protected abstract fun copyTaskRecord(
            oldTask: Task<*>,
            now: ExactTimeStamp.Local,
            instanceJsons: MutableMap<String, InstanceJson>,
    ): TaskRecord<T>

    private fun convertScheduleKey(userInfo: UserInfo, oldTask: Task<*>, oldScheduleKey: ScheduleKey): ScheduleKey {
        check(oldTask.project != this)

        val (oldScheduleDate, oldScheduleTimePair) = oldScheduleKey

        if (oldScheduleTimePair.customTimeKey == null) return oldScheduleKey

        val newCustomTime = when (val customTime = oldTask.project.getCustomTime(oldScheduleTimePair.customTimeKey)) {
            is Time.Custom.Project<*> -> {
                val ownerKey = when (customTime) {
                    is PrivateCustomTime -> userInfo.key
                    is SharedCustomTime -> customTime.ownerKey!!
                    else -> throw IllegalStateException()
                }

                getOrCreateCustomTime(ownerKey, customTime)
            }
            is Time.Custom.User -> customTime
        }

        return ScheduleKey(oldScheduleDate, TimePair(newCustomTime.key))
    }

    private class InstanceConversionData(
            val newInstanceJson: InstanceJson,
            val newScheduleKey: ScheduleKey,
            val updater: (Map<String, String>) -> Any?,
    )

    @Suppress("ConstantConditionIf")
    fun copyTask(
            deviceDbInfo: DeviceDbInfo,
            oldTask: Task<*>,
            instances: Collection<Instance<*>>,
            now: ExactTimeStamp.Local,
            newProjectKey: ProjectKey<*>,
    ): Pair<Task<T>, List<(Map<String, String>) -> Any?>> {
        val instanceDatas = instances.map { oldInstance ->
            val (newInstance, updater) = getInstanceJson(deviceDbInfo.key, oldInstance, newProjectKey)

            val newScheduleKey = convertScheduleKey(deviceDbInfo.userInfo, oldTask, oldInstance.scheduleKey)

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

            newTask.copySchedules(deviceDbInfo, now, currentSchedules)
        } else {
            currentNoScheduleOrParent?.let { newTask.setNoScheduleOrParent(now) }
        }

        return newTask to instanceDatas.map { it.updater }
    }

    protected abstract fun getOrCreateCustomTime(
            ownerKey: UserKey,
            customTime: Time.Custom.Project<*>,
    ): Time.Custom.Project<T> // todo customtime migrate

    fun getOrCopyTime(ownerKey: UserKey, time: Time) = time.let {
        when (it) {
            is Time.Custom.Project<*> -> getOrCreateCustomTime(ownerKey, it)
            is Time.Custom.User -> it
            is Time.Normal -> it
        }
    }

    private fun getInstanceJson(
            ownerKey: UserKey,
            instance: Instance<*>,
            newProjectKey: ProjectKey<*>,
    ): Pair<InstanceJson, (Map<String, String>) -> Any?> {
        val done = instance.doneOffset

        val instanceDate = instance.instanceDate

        val newInstanceTime = getOrCopyTime(ownerKey, instance.instanceTime)
        val instanceTimeString = JsonTime.fromTime<T>(newInstanceTime).toJson()

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
                val newTaskId = taskKeyMap.getValue(oldKey.taskKey.taskId)
                val newTaskKey = TaskKey(newProjectKey, newTaskId)

                instanceJson.parentJson = InstanceJson.ParentJson(InstanceKey(newTaskKey, oldKey.scheduleKey))
            }
        }

        return instanceJson to updater
    }

    fun <V : TaskHierarchy<*>> copyTaskHierarchy(
            now: ExactTimeStamp.Local,
            startTaskHierarchy: V,
            parentTaskId: String,
            childTaskId: String,
    ): TaskHierarchy<T> {
        check(parentTaskId.isNotEmpty())
        check(childTaskId.isNotEmpty())

        val endTime = startTaskHierarchy.endExactTimeStamp?.long

        val taskHierarchyJson = TaskHierarchyJson(
                parentTaskId,
                childTaskId,
                now.long,
                now.offset,
                endTime
        )

        val taskHierarchyRecord = projectRecord.newTaskHierarchyRecord(taskHierarchyJson)

        val taskHierarchy = TaskHierarchy(this, taskHierarchyRecord)

        taskHierarchyContainer.add(taskHierarchy.id, taskHierarchy)
        taskHierarchy.invalidateTasks()

        return taskHierarchy
    }

    fun deleteTask(task: Task<T>) {
        check(_tasks.containsKey(task.id))

        _tasks.remove(task.id)
    }

    fun deleteTaskHierarchy(taskHierarchy: TaskHierarchy<T>) {
        taskHierarchyContainer.removeForce(taskHierarchy.id)
        taskHierarchy.invalidateTasks()
    }

    fun getTaskIfPresent(taskId: String) = _tasks[taskId]

    fun getTaskForce(taskId: String) = _tasks[taskId]
            ?: throw MissingTaskException(projectKey, taskId)

    fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy<T>> {
        check(childTaskKey.taskId.isNotEmpty())

        return taskHierarchyContainer.getByChildTaskKey(childTaskKey)
    }

    fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy<T>> {
        check(parentTaskKey.taskId.isNotEmpty())

        return taskHierarchyContainer.getByParentTaskKey(parentTaskKey)
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

    fun getTaskHierarchy(id: String) = taskHierarchyContainer.getById(id)

    abstract fun deleteCustomTime(remoteCustomTime: Time.Custom.Project<T>)

    abstract fun getProjectCustomTime(projectCustomTimeKey: CustomTimeKey.Project<T>): Time.Custom.Project<T>

    @Suppress("UNCHECKED_CAST")
    fun getUntypedProjectCustomTime(projectCustomTimeId: CustomTimeId.Project<*>) =
            getProjectCustomTime(projectCustomTimeId as CustomTimeId.Project<T>)

    fun getUntypedProjectCustomTime(projectCustomTimeId: String) =
            getProjectCustomTime(projectRecord.getProjectCustomTimeId(projectCustomTimeId))

    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User) =
            userCustomTimeProvider.getUserCustomTime(userCustomTimeKey)

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project<T>) = projectRecord.getProjectCustomTimeKey(projectCustomTimeId)

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
            ?.let(::getCustomTime)
            ?: Time.Normal(timePair.hourMinute!!)

    fun getDateTime(scheduleKey: ScheduleKey) =
            DateTime(scheduleKey.scheduleDate, getTime(scheduleKey.scheduleTimePair))

    fun convertRemoteToRemoteHelper(
            now: ExactTimeStamp.Local,
            remoteToRemoteConversion: RemoteToRemoteConversion<T>,
            startTask: Task<T>,
    ) {
        if (remoteToRemoteConversion.startTasks.containsKey(startTask.id)) return

        remoteToRemoteConversion.startTasks[startTask.id] = Pair(
                startTask,
                startTask.existingInstances
                        .values
                        .filter {
                            listOf(
                                    it.scheduleDateTime,
                                    it.instanceDateTime
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
    ): Sequence<Instance<out T>> {
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
    ): Task<T>

    abstract fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, ProjectUser>

    fun getInstance(instanceKey: InstanceKey) = getTaskForce(instanceKey.taskKey.taskId).getInstance(
            DateTime(
                    instanceKey.scheduleKey.scheduleDate,
                    getTime(instanceKey.scheduleKey.scheduleTimePair)
            )
    )

    private class MissingTaskException(projectId: ProjectKey<*>, taskId: String) :
            Exception("projectId: $projectId, taskId: $taskId")

    interface Parent {

        fun deleteProject(project: Project<*>) // todo this is never implemented, just get rid of it
    }
}
