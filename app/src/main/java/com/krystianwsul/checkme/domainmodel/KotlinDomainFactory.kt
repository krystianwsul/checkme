package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.local.LocalInstance
import com.krystianwsul.checkme.domainmodel.local.LocalTask
import com.krystianwsul.checkme.domainmodel.relevance.*
import com.krystianwsul.checkme.firebase.*
import com.krystianwsul.checkme.gui.HierarchyData
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManger
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.util.*

class KotlinDomainFactory(persistenceManager: PersistenceManger?) {

    companion object {

        var _kotlinDomainFactory: KotlinDomainFactory? = null

        @Synchronized
        fun getKotlinDomainFactory(persistenceManager: PersistenceManger? = null): KotlinDomainFactory {
            if (_kotlinDomainFactory == null)
                _kotlinDomainFactory = KotlinDomainFactory(persistenceManager)
            return _kotlinDomainFactory!!
        }
    }

    val domainFactory: DomainFactory

    private var start: ExactTimeStamp
    private var read: ExactTimeStamp
    private var stop: ExactTimeStamp

    val readMillis get() = read.long - start.long
    val instantiateMillis get() = stop.long - read.long

    var userInfo: UserInfo? = null

    var recordQuery: Query? = null
    var recordListener: ValueEventListener? = null

    var userQuery: Query? = null
    var userListener: ValueEventListener? = null

    @JvmField
    var localFactory: LocalFactory

    var remoteProjectFactory: RemoteProjectFactory? = null

    var remoteRootUser: RemoteRootUser? = null

    val notTickFirebaseListeners = mutableListOf<(DomainFactory) -> Unit>()

    var tickData: TickData? = null

    var skipSave = false

    val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    init {
        start = ExactTimeStamp.now

        domainFactory = DomainFactory(this)
        localFactory = persistenceManager?.let { LocalFactory(it) } ?: LocalFactory.instance

        read = ExactTimeStamp.now

        localFactory.initialize(this)

        stop = ExactTimeStamp.now
    }

    // misc

    val isHoldingWakeLock get() = tickData?.wakelock?.isHeld == true

    val taskCount get() = localFactory.taskCount + (remoteProjectFactory?.taskCount ?: 0)

    val instanceCount
        get() = localFactory.instanceCount + (remoteProjectFactory?.instanceCount ?: 0)

    val customTimeCount get() = customTimes.size

    // todo eliminate context
    fun save(context: Context, dataId: Int, source: SaveService.Source) = save(context, listOf(dataId), source)

    // internal

    private fun getExistingInstanceIfPresent(taskKey: TaskKey, scheduleDateTime: DateTime): Instance? {
        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, scheduleDateTime.time.timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    private fun getExistingInstanceIfPresent(instanceKey: InstanceKey): Instance? {
        return if (instanceKey.taskKey.localTaskId != null) {
            check(TextUtils.isEmpty(instanceKey.taskKey.remoteProjectId))
            check(TextUtils.isEmpty(instanceKey.taskKey.remoteTaskId))

            localFactory.getExistingInstanceIfPresent(instanceKey)
        } else {
            check(!TextUtils.isEmpty(instanceKey.taskKey.remoteProjectId))
            check(!TextUtils.isEmpty(instanceKey.taskKey.remoteTaskId))
            checkNotNull(remoteProjectFactory)

            remoteProjectFactory!!.getExistingInstanceIfPresent(instanceKey)
        }
    }

    fun getRemoteCustomTimeId(projectId: String, customTimeKey: CustomTimeKey): String {
        if (!TextUtils.isEmpty(customTimeKey.remoteProjectId)) {
            check(!TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))
            check(customTimeKey.localCustomTimeId == null)

            check(customTimeKey.remoteProjectId == projectId)

            return customTimeKey.remoteCustomTimeId!!
        } else {
            check(TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))
            checkNotNull(customTimeKey.localCustomTimeId)

            val localCustomTime = localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId)

            check(localCustomTime.hasRemoteRecord(projectId))

            return localCustomTime.getRemoteId(projectId)
        }
    }

    private fun generateInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        if (taskKey.localTaskId != null) {
            check(TextUtils.isEmpty(taskKey.remoteProjectId))
            check(TextUtils.isEmpty(taskKey.remoteTaskId))

            return LocalInstance(this, taskKey.localTaskId, scheduleDateTime)
        } else {
            check(remoteProjectFactory != null)
            check(!TextUtils.isEmpty(taskKey.remoteProjectId))
            check(!TextUtils.isEmpty(taskKey.remoteTaskId))

            val remoteCustomTimeId: String?
            val hour: Int?
            val minute: Int?

            val customTimeKey = scheduleDateTime.time.timePair.customTimeKey
            val hourMinute = scheduleDateTime.time.timePair.hourMinute

            if (customTimeKey != null) {
                check(hourMinute == null)

                remoteCustomTimeId = getRemoteCustomTimeId(taskKey.remoteProjectId!!, customTimeKey)

                hour = null
                minute = null
            } else {
                checkNotNull(hourMinute)

                remoteCustomTimeId = null

                hour = hourMinute.hour
                minute = hourMinute.minute
            }

            val instanceShownRecord = localFactory.getInstanceShownRecord(taskKey.remoteProjectId!!, taskKey.remoteTaskId!!, scheduleDateTime.date.year, scheduleDateTime.date.month, scheduleDateTime.date.day, remoteCustomTimeId, hour, minute)

            val remoteProject = remoteProjectFactory!!.getTaskForce(taskKey).remoteProject

            return RemoteInstance(this, remoteProject, taskKey.remoteTaskId, scheduleDateTime, instanceShownRecord)
        }
    }

    fun getInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        val existingInstance = getExistingInstanceIfPresent(taskKey, scheduleDateTime)

        return existingInstance ?: generateInstance(taskKey, scheduleDateTime)
    }

    fun getInstance(instanceKey: InstanceKey): Instance {
        getExistingInstanceIfPresent(instanceKey)?.let { return it }

        val dateTime = getDateTime(instanceKey.scheduleKey.scheduleDate, instanceKey.scheduleKey.scheduleTimePair)

        return generateInstance(instanceKey.taskKey, dateTime) // DateTime -> timePair
    }

    fun getPastInstances(task: Task, now: ExactTimeStamp): List<Instance> {
        val allInstances = HashMap<InstanceKey, Instance>()

        allInstances.putAll(task.existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(task.getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return ArrayList(allInstances.values)
    }

    fun getRootInstances(startExactTimeStamp: ExactTimeStamp?, endExactTimeStamp: ExactTimeStamp, now: ExactTimeStamp): List<Instance> {
        check(startExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        val allInstances = HashMap<InstanceKey, Instance>()

        for (instance in getExistingInstances()) {
            val instanceExactTimeStamp = instance.instanceDateTime
                    .timeStamp
                    .toExactTimeStamp()

            if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                continue

            if (endExactTimeStamp <= instanceExactTimeStamp)
                continue

            allInstances[instance.instanceKey] = instance
        }

        getTasks().forEach { task ->
            for (instance in task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                val instanceExactTimeStamp = instance.instanceDateTime.timeStamp.toExactTimeStamp()

                if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                    continue

                if (endExactTimeStamp <= instanceExactTimeStamp)
                    continue

                allInstances[instance.instanceKey] = instance
            }
        }

        return allInstances.values.filter { it.isRootInstance(now) && it.isVisible(now) }
    }

    fun getTime(timePair: TimePair): Time {
        return if (timePair.hourMinute != null) {
            check(timePair.customTimeKey == null)

            NormalTime(timePair.hourMinute)
        } else {
            checkNotNull(timePair.customTimeKey)

            getCustomTime(timePair.customTimeKey)
        }
    }

    private fun getDateTime(date: Date, timePair: TimePair) = DateTime(date, getTime(timePair))

    fun getParentTask(childTask: Task, exactTimeStamp: ExactTimeStamp): Task? {
        check(childTask.notDeleted(exactTimeStamp))

        val parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp)
        return if (parentTaskHierarchy == null) {
            null
        } else {
            check(parentTaskHierarchy.notDeleted(exactTimeStamp))

            val parentTask = parentTaskHierarchy.parentTask
            check(parentTask.notDeleted(exactTimeStamp))

            parentTask
        }
    }

    fun getCustomTime(customTimeKey: CustomTimeKey) = if (customTimeKey.localCustomTimeId != null) {
        check(TextUtils.isEmpty(customTimeKey.remoteProjectId))
        check(TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))

        localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId)
    } else {
        check(!TextUtils.isEmpty(customTimeKey.remoteProjectId))
        check(!TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))
        checkNotNull(remoteProjectFactory)

        remoteProjectFactory!!.getRemoteCustomTime(customTimeKey.remoteProjectId!!, customTimeKey.remoteCustomTimeId!!)
    }

    fun getCurrentCustomTimes() = localFactory.currentCustomTimes

    fun getChildInstanceDatas(instance: Instance, now: ExactTimeStamp): MutableMap<InstanceKey, GroupListFragment.InstanceData> {
        return instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)
                    val instanceData = GroupListFragment.InstanceData(childInstance.done, childInstance.instanceKey, null, childInstance.name, childInstance.instanceDateTime.timeStamp, childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.instanceDateTime.time.timePair, childTask.note, children, HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal), childInstance.ordinal)
                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()
    }

    private fun getChildTaskDatas(now: ExactTimeStamp, parentTask: Task, context: Context, excludedTaskKeys: List<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> =
            parentTask.getChildTaskHierarchies(now)
                    .asSequence()
                    .filterNot { excludedTaskKeys.contains(it.childTaskKey) }
                    .map {
                        val childTask = it.childTask
                        val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.childTaskKey)
                        val parentTreeData = CreateTaskViewModel.ParentTreeData(childTask.name, getChildTaskDatas(now, childTask, context, excludedTaskKeys), CreateTaskViewModel.ParentKey.TaskParentKey(childTask.taskKey), childTask.getScheduleText(context, now), childTask.note, CreateTaskViewModel.SortKey.TaskSortKey(childTask.startExactTimeStamp))

                        taskParentKey to parentTreeData
                    }
                    .toList()
                    .toMap()

    fun getParentTreeDatas(context: Context, now: ExactTimeStamp, excludedTaskKeys: List<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        val parentTreeDatas = mutableMapOf<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>()

        parentTreeDatas.putAll(localFactory.tasks
                .filter { !excludedTaskKeys.contains(it.taskKey) && it.current(now) && it.isVisible(now) && it.isRootTask(now) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getChildTaskDatas(now, it, context, excludedTaskKeys), taskParentKey, it.getScheduleText(context, now), it.note, CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp))

                    taskParentKey to parentTreeData
                }
                .toMap())

        if (remoteProjectFactory != null) {
            parentTreeDatas.putAll(remoteProjectFactory!!.remoteProjects
                    .values
                    .filter { it.current(now) }
                    .map {
                        val projectParentKey = CreateTaskViewModel.ParentKey.ProjectParentKey(it.id)

                        val users = it.users.joinToString(", ") { it.name }
                        val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getProjectTaskTreeDatas(context, now, it, excludedTaskKeys), projectParentKey, users, null, CreateTaskViewModel.SortKey.ProjectSortKey(it.id))

                        projectParentKey to parentTreeData
                    }
                    .toMap())
        }

        return parentTreeDatas
    }

    fun getProjectTaskTreeDatas(context: Context, now: ExactTimeStamp, remoteProject: RemoteProject, excludedTaskKeys: List<TaskKey>): Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> {
        return remoteProject.tasks
                .filter { !excludedTaskKeys.contains(it.taskKey) && it.current(now) && it.isVisible(now) && it.isRootTask(now) }
                .map {
                    val taskParentKey = CreateTaskViewModel.ParentKey.TaskParentKey(it.taskKey)
                    val parentTreeData = CreateTaskViewModel.ParentTreeData(it.name, getChildTaskDatas(now, it, context, excludedTaskKeys), taskParentKey, it.getScheduleText(context, now), it.note, CreateTaskViewModel.SortKey.TaskSortKey(it.startExactTimeStamp))

                    taskParentKey to parentTreeData
                }
                .toMap()
    }

    fun convertLocalToRemote(context: Context, now: ExactTimeStamp, startingLocalTask: LocalTask, projectId: String): RemoteTask {
        check(!TextUtils.isEmpty(projectId))

        checkNotNull(remoteProjectFactory)
        checkNotNull(userInfo)

        val localToRemoteConversion = LocalToRemoteConversion()
        localFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask)

        updateNotifications(context, true, now, localToRemoteConversion.localTasks
                .values
                .map { it.first.taskKey })

        val remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)

        for (pair in localToRemoteConversion.localTasks.values) {
            checkNotNull(pair)

            val remoteTask = remoteProject.copyLocalTask(pair.first, pair.second, now)
            localToRemoteConversion.remoteTasks[pair.first.id] = remoteTask
        }

        for (localTaskHierarchy in localToRemoteConversion.localTaskHierarchies) {
            checkNotNull(localTaskHierarchy)

            val parentRemoteTask = localToRemoteConversion.remoteTasks[localTaskHierarchy.parentTaskId]!!
            val childRemoteTask = localToRemoteConversion.remoteTasks[localTaskHierarchy.childTaskId]!!

            val remoteTaskHierarchy = remoteProject.copyLocalTaskHierarchy(localTaskHierarchy, parentRemoteTask.id, childRemoteTask.id)

            localToRemoteConversion.remoteTaskHierarchies.add(remoteTaskHierarchy)
        }

        for (pair in localToRemoteConversion.localTasks.values) {
            pair.second.forEach { it.delete() }

            pair.first.delete()
        }

        return localToRemoteConversion.remoteTasks[startingLocalTask.id]!!
    }

    fun joinTasks(newParentTask: Task, joinTasks: List<Task>, now: ExactTimeStamp) {
        check(newParentTask.current(now))
        check(joinTasks.size > 1)

        for (joinTask in joinTasks) {
            check(joinTask.current(now))

            if (joinTask.isRootTask(now)) {
                joinTask.getCurrentSchedules(now).forEach { it.setEndExactTimeStamp(now) }
            } else {
                val taskHierarchy = getParentTaskHierarchy(joinTask, now)!!

                taskHierarchy.setEndExactTimeStamp(now)
            }

            newParentTask.addChild(joinTask, now)
        }
    }

    fun getParentTaskHierarchy(childTask: Task, exactTimeStamp: ExactTimeStamp): TaskHierarchy? {
        if (childTask.current(exactTimeStamp)) {
            check(childTask.notDeleted(exactTimeStamp))

            val childTaskKey = childTask.taskKey

            val taskHierarchies = childTask.getTaskHierarchiesByChildTaskKey(childTaskKey).filter { it.current(exactTimeStamp) }

            return if (taskHierarchies.isEmpty()) {
                null
            } else {
                taskHierarchies.single()
            }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            check(childTask.notDeleted(exactTimeStamp))

            val childTaskKey = childTask.taskKey

            val taskHierarchies = childTask.getTaskHierarchiesByChildTaskKey(childTaskKey).filter { it.startExactTimeStamp == childTask.startExactTimeStamp }

            return if (taskHierarchies.isEmpty()) {
                null
            } else {
                taskHierarchies.single()
            }
        }
    }

    fun getTasks(): Stream<Task> {
        return if (remoteProjectFactory != null) {
            Stream.concat<Task>(Stream.of<LocalTask>(localFactory.tasks), Stream.of<RemoteTask>(remoteProjectFactory!!.tasks))
        } else {
            Stream.of<Task>(localFactory.tasks)
        }
    }

    private val customTimes
        get() = localFactory.localCustomTimes.toMutableList<CustomTime>().apply {
        remoteProjectFactory?.let { addAll(it.remoteCustomTimes) }
    }

    fun getTaskForce(taskKey: TaskKey) = if (taskKey.localTaskId != null) {
        check(TextUtils.isEmpty(taskKey.remoteTaskId))

        localFactory.getTaskForce(taskKey.localTaskId)
    } else {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))
        checkNotNull(remoteProjectFactory)

        remoteProjectFactory!!.getTaskForce(taskKey)
    }

    fun getTaskIfPresent(taskKey: TaskKey) = if (taskKey.localTaskId != null) {
        check(TextUtils.isEmpty(taskKey.remoteTaskId))

        localFactory.getTaskIfPresent(taskKey.localTaskId)
    } else {
        check(!TextUtils.isEmpty(taskKey.remoteTaskId))
        checkNotNull(remoteProjectFactory)

        remoteProjectFactory!!.getTaskIfPresent(taskKey)
    }

    fun getChildTaskHierarchies(parentTask: Task, exactTimeStamp: ExactTimeStamp): List<TaskHierarchy> {
        check(parentTask.current(exactTimeStamp))

        return parentTask.getTaskHierarchiesByParentTaskKey(parentTask.taskKey)
                .asSequence()
                .filter { it.current(exactTimeStamp) && it.childTask.current(exactTimeStamp) }
                .sortedBy { it.ordinal }
                .toList()
    }

    fun getChildTaskDatas(parentTask: Task, now: ExactTimeStamp, context: Context): List<TaskListFragment.ChildTaskData> {
        return parentTask.getChildTaskHierarchies(now)
                .asSequence()
                .sortedBy { it.ordinal }
                .map {
                    val childTask = it.childTask

                    TaskListFragment.ChildTaskData(childTask.name, childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.note, childTask.startExactTimeStamp, childTask.taskKey, HierarchyData(it.taskHierarchyKey, it.ordinal))
                }
                .toList()
    }

    private fun getExistingInstances() = localFactory.existingInstances
            .toMutableList<Instance>()
            .apply {
                remoteProjectFactory?.let { addAll(it.existingInstances) }
            }

    fun getChildTaskDatas(parentTask: Task, now: ExactTimeStamp): List<GroupListFragment.TaskData> = parentTask.getChildTaskHierarchies(now)
            .map {
                val childTask = it.childTask

                GroupListFragment.TaskData(childTask.taskKey, childTask.name, getChildTaskDatas(childTask, now), childTask.startExactTimeStamp, childTask.note)
            }

    fun getMainData(now: ExactTimeStamp, context: Context): TaskListFragment.TaskData {
        val childTaskDatas = getTasks().filter { it.current(now) && it.isVisible(now) && it.isRootTask(now) }
                .map { TaskListFragment.ChildTaskData(it.name, it.getScheduleText(context, now), getChildTaskDatas(it, now, context), it.note, it.startExactTimeStamp, it.taskKey, null) }
                .collect(Collectors.toList())
                .toMutableList()
                .apply { sortDescending() }

        return TaskListFragment.TaskData(childTaskDatas, null)
    }

    fun setInstanceDone(context: Context, now: ExactTimeStamp, dataId: Int, source: SaveService.Source, instanceKey: InstanceKey, done: Boolean): Instance {
        val instance = getInstance(instanceKey)

        instance.setDone(done, now)

        updateNotifications(context, now)

        save(context, dataId, source)

        notifyCloud(context, instance.remoteNullableProject)

        return instance
    }

    fun setIrrelevant(now: ExactTimeStamp): Irrelevant {
        val tasks = getTasks().collect(Collectors.toList<Task>())

        for (task in tasks)
            task.updateOldestVisible(now)

        // relevant hack
        val taskRelevances = tasks.map { it.taskKey to TaskRelevance(this, it) }.toMap()

        val existingInstances = getExistingInstances()
        val rootInstances = getRootInstances(null, now.plusOne(), now)

        val instanceRelevances = (existingInstances + rootInstances)
                .asSequence()
                .distinct()
                .map { it.instanceKey to InstanceRelevance(it) }
                .toList()
                .toMap()
                .toMutableMap()

        val localCustomTimeRelevances = localFactory.localCustomTimes
                .map { it.id to LocalCustomTimeRelevance(it) }
                .toMap()

        tasks.asSequence()
                .filter { it.current(now) && it.isRootTask(now) && it.isVisible(now) }
                .map { taskRelevances[it.taskKey]!! }.toList()
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now) }

        rootInstances.map { instanceRelevances[it.instanceKey]!! }.forEach { it.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now) }

        existingInstances.asSequence()
                .filter { it.isRootInstance(now) && it.isVisible(now) }
                .map { instanceRelevances[it.instanceKey]!! }.toList()
                .forEach { it.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now) }

        getCurrentCustomTimes().map { localCustomTimeRelevances[it.id]!! }.forEach { it.setRelevant() }

        val relevantTasks = taskRelevances.values
                .filter { it.relevant }
                .map { it.task }

        val irrelevantTasks = ArrayList<Task>(tasks)
        irrelevantTasks.removeAll(relevantTasks)

        check(irrelevantTasks.none { it.isVisible(now) })

        val relevantExistingInstances = instanceRelevances.values
                .filter { it.relevant }
                .map { it.instance }
                .filter { it.exists() }

        val irrelevantExistingInstances = ArrayList<Instance>(existingInstances)
        irrelevantExistingInstances.removeAll(relevantExistingInstances)

        check(irrelevantExistingInstances.none { it.isVisible(now) })

        val relevantLocalCustomTimes = localCustomTimeRelevances.values
                .filter { it.relevant }
                .map { it.localCustomTime }

        val irrelevantLocalCustomTimes = ArrayList<LocalCustomTime>(localFactory.localCustomTimes)
        irrelevantLocalCustomTimes.removeAll(relevantLocalCustomTimes)

        check(irrelevantLocalCustomTimes.none { it.current })

        irrelevantExistingInstances.forEach { it.delete() }
        irrelevantTasks.forEach { it.delete() }
        irrelevantLocalCustomTimes.forEach { it.delete() }

        val irrelevantRemoteCustomTimes: MutableList<RemoteCustomTime>?
        val irrelevantRemoteProjects: MutableList<RemoteProject>?
        if (remoteProjectFactory != null) {
            val remoteCustomTimes = remoteProjectFactory!!.remoteCustomTimes
            val remoteCustomTimeRelevances = remoteCustomTimes.map { kotlin.Pair(it.projectId, it.id) to RemoteCustomTimeRelevance(it) }.toMap()

            val remoteProjects = remoteProjectFactory!!.remoteProjects.values
            val remoteProjectRelevances = remoteProjects.map { it.id to RemoteProjectRelevance(it) }.toMap()

            remoteProjects.filter { it.current(now) }
                    .map { remoteProjectRelevances[it.id]!! }
                    .forEach { it.setRelevant() }

            taskRelevances.values
                    .filter { it.relevant }
                    .forEach { it.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances) }

            instanceRelevances.values
                    .filter { it.relevant }
                    .forEach { it.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances) }

            val relevantRemoteCustomTimes = remoteCustomTimeRelevances.values
                    .filter { it.relevant }
                    .map { it.remoteCustomTime }

            irrelevantRemoteCustomTimes = ArrayList(remoteCustomTimes)
            irrelevantRemoteCustomTimes.removeAll(relevantRemoteCustomTimes)
            irrelevantRemoteCustomTimes.forEach { it.delete() }

            val relevantRemoteProjects = remoteProjectRelevances.values
                    .filter { it.relevant }
                    .map { it.remoteProject }

            irrelevantRemoteProjects = ArrayList(remoteProjects)
            irrelevantRemoteProjects.removeAll(relevantRemoteProjects)
            irrelevantRemoteProjects.forEach { it.delete() }
        } else {
            irrelevantRemoteCustomTimes = null
            irrelevantRemoteProjects = null
        }

        return Irrelevant(irrelevantLocalCustomTimes, irrelevantTasks, irrelevantExistingInstances, irrelevantRemoteCustomTimes, irrelevantRemoteProjects)
    }

    fun notifyCloud(context: Context, remoteProject: RemoteProject?) { // todo check all functions for possible overloads
        val remoteProjects = setOf(remoteProject)
                .filterNotNull()
                .toSet()

        notifyCloud(context, remoteProjects)
    }

    fun notifyCloud(context: Context, remoteProjects: Set<RemoteProject>) {
        if (!remoteProjects.isEmpty()) {
            checkNotNull(userInfo)

            BackendNotifier.notify(context, remoteProjects, userInfo!!, listOf())
        }
    }

    fun notifyCloud(context: Context, remoteProject: RemoteProject, userKeys: Collection<String>) = BackendNotifier.notify(context, setOf(remoteProject), userInfo!!, userKeys)

    fun updateNotifications(context: Context, now: ExactTimeStamp) = updateNotifications(context, true, now, mutableListOf())

    private val taskKeys
        get() = localFactory.taskIds
                .map { TaskKey(it) }
                .toMutableSet()
                .apply { remoteProjectFactory?.let { addAll(it.taskKeys) } }

    fun updateNotifications(context: Context, silent: Boolean, now: ExactTimeStamp, removedTaskKeys: List<TaskKey>) {
        val rootInstances = getRootInstances(null, now.plusOne(), now) // 24 hack

        val notificationInstances = rootInstances.filter { it.done == null && !it.notified && it.instanceDateTime.timeStamp.toExactTimeStamp() <= now && !removedTaskKeys.contains(it.taskKey) }.associateBy { it.instanceKey }

        val shownInstanceKeys = getExistingInstances().filter { it.notificationShown }
                .map { it.instanceKey }
                .toMutableSet()

        val instanceShownRecordNotificationDatas = localFactory.instanceShownRecords
                .filterNot { it.notificationShown }
                .map { instanceShownRecord ->
                    val scheduleDate = Date(instanceShownRecord.scheduleYear, instanceShownRecord.scheduleMonth, instanceShownRecord.scheduleDay)
                    val remoteCustomTimeId = instanceShownRecord.scheduleCustomTimeId

                    val customTimeKey: CustomTimeKey?
                    val hourMinute: HourMinute?
                    if (!TextUtils.isEmpty(remoteCustomTimeId)) {
                        check(instanceShownRecord.scheduleHour == null)
                        check(instanceShownRecord.scheduleMinute == null)

                        customTimeKey = getCustomTimeKey(instanceShownRecord.projectId, remoteCustomTimeId!!)
                        hourMinute = null
                    } else {
                        checkNotNull(instanceShownRecord.scheduleHour)
                        checkNotNull(instanceShownRecord.scheduleMinute)

                        customTimeKey = null
                        hourMinute = HourMinute(instanceShownRecord.scheduleHour, instanceShownRecord.scheduleMinute)
                    }

                    val taskKey = TaskKey(instanceShownRecord.projectId, instanceShownRecord.taskId)
                    val instanceKey = InstanceKey(taskKey, scheduleDate, TimePair(customTimeKey, hourMinute))

                    shownInstanceKeys.add(instanceKey)

                    instanceKey to Pair(Instance.getNotificationId(scheduleDate, customTimeKey, hourMinute, taskKey), instanceShownRecord)
                }
                .toMap()

        val showInstanceKeys = notificationInstances.keys.filter { !shownInstanceKeys.contains(it) }

        val hideInstanceKeys = shownInstanceKeys.filter { !notificationInstances.containsKey(it) }.toSet()

        for (showInstanceKey in showInstanceKeys)
            getInstance(showInstanceKey).setNotificationShown(true, now)

        val allTaskKeys = taskKeys

        for (hideInstanceKey in hideInstanceKeys) {
            if (allTaskKeys.contains(hideInstanceKey.taskKey))
                getInstance(hideInstanceKey).setNotificationShown(false, now)
            else
                instanceShownRecordNotificationDatas[hideInstanceKey]!!.second.notificationShown = false
        }

        var message = ""

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size > TickJobIntentService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, silent, now)
                    } else {
                        NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, true, now)
                    }
                } else { // instances shown
                    for (shownInstanceKey in shownInstanceKeys) {
                        if (allTaskKeys.contains(shownInstanceKey.taskKey)) {
                            NotificationWrapper.instance.cancelNotification(getInstance(shownInstanceKey).notificationId)
                        } else {
                            val notificationId = instanceShownRecordNotificationDatas[shownInstanceKey]!!.first

                            NotificationWrapper.instance.cancelNotification(notificationId)
                        }
                    }

                    NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, silent, now)
                }
            } else { // show instances
                if (shownInstanceKeys.size > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    NotificationWrapper.instance.cancelNotification(0)

                    for (instance in notificationInstances.values)
                        notifyInstance(instance, silent, now)
                } else { // instances shown
                    for (hideInstanceKey in hideInstanceKeys) {
                        if (allTaskKeys.contains(hideInstanceKey.taskKey)) {
                            NotificationWrapper.instance.cancelNotification(getInstance(hideInstanceKey).notificationId)
                        } else {
                            val notificationId = instanceShownRecordNotificationDatas[hideInstanceKey]!!.first

                            NotificationWrapper.instance.cancelNotification(notificationId)
                        }
                    }

                    for (showInstanceKey in showInstanceKeys)
                        notifyInstance(notificationInstances[showInstanceKey]!!, silent, now)

                    notificationInstances.values
                            .filter { !showInstanceKeys.contains(it.instanceKey) }
                            .forEach { updateInstance(it, now) }
                }
            }
        } else {
            if (notificationInstances.isEmpty()) {
                message += ", hg"
                NotificationWrapper.instance.cancelNotification(0)
            } else {
                message += ", sg"
                NotificationWrapper.instance.notifyGroup(this, notificationInstances.values, true, now)
            }

            message += ", hiding " + hideInstanceKeys.size
            for (hideInstanceKey in hideInstanceKeys) {
                if (allTaskKeys.contains(hideInstanceKey.taskKey)) {
                    NotificationWrapper.instance.cancelNotification(getInstance(hideInstanceKey).notificationId)
                } else {
                    val notificationId = instanceShownRecordNotificationDatas[hideInstanceKey]!!.first

                    NotificationWrapper.instance.cancelNotification(notificationId)
                }
            }

            message += ", s " + showInstanceKeys.size
            for (showInstanceKey in showInstanceKeys)
                notifyInstance(notificationInstances[showInstanceKey]!!, silent, now)

            val updateInstances = notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

            message += ", u " + updateInstances.size
            updateInstances.forEach { updateInstance(it, now) }
        }

        val sharedPreferences = context.getSharedPreferences(TickJobIntentService.TICK_PREFERENCES, Context.MODE_PRIVATE)!!

        val tickLog = sharedPreferences.getString(TickJobIntentService.TICK_LOG, "")
        val tickLogArr = Arrays.asList(*TextUtils.split(tickLog, "\n"))
        val tickLogArrTrimmed = ArrayList(tickLogArr.subList(Math.max(tickLogArr.size - 20, 0), tickLogArr.size))
        tickLogArrTrimmed.add(now.toString() + " s? " + (if (silent) "t" else "f") + message)

        val editor = sharedPreferences.edit()

        if (!silent)
            editor.putLong(TickJobIntentService.LAST_TICK_KEY, now.long)

        var nextAlarm = getExistingInstances().map { it.instanceDateTime.timeStamp }
                .filter { it.toExactTimeStamp() > now }
                .min()

        val minSchedulesTimeStamp = getTasks().filter { it.current(now) && it.isRootTask(now) }
                .flatMap { Stream.of<Schedule>(it.getCurrentSchedules(now)) }
                .map { it.getNextAlarm(now) }
                .filter { timeStamp -> timeStamp != null }
                .min { obj, other -> obj!!.compareTo(other!!) }

        if (minSchedulesTimeStamp.isPresent && (nextAlarm == null || nextAlarm > minSchedulesTimeStamp.get()!!))
            nextAlarm = minSchedulesTimeStamp.get()

        NotificationWrapper.instance.updateAlarm(nextAlarm)

        if (nextAlarm != null)
            tickLogArrTrimmed.add("next tick: $nextAlarm")

        editor.putString(TickJobIntentService.TICK_LOG, TextUtils.join("\n", tickLogArrTrimmed))
        editor.apply()
    }

    private fun notifyInstance(instance: Instance, silent: Boolean, now: ExactTimeStamp) {
        var reallySilent = silent
        val realtime = SystemClock.elapsedRealtime()

        val optional = lastNotificationBeeps.values.max()
        if (optional?.let { realtime - it < 5000 } == true) {
            Log.e("asdf", "skipping notification sound for " + instance.name)

            reallySilent = true
        }

        NotificationWrapper.instance.notifyInstance(this, instance, reallySilent, now)

        if (!reallySilent)
            lastNotificationBeeps[instance.instanceKey] = SystemClock.elapsedRealtime()
    }

    private fun updateInstance(instance: Instance, now: ExactTimeStamp) {
        val instanceKey = instance.instanceKey

        val realtime = SystemClock.elapsedRealtime()

        if (lastNotificationBeeps.containsKey(instanceKey)) {
            val then = lastNotificationBeeps[instanceKey]!!

            check(realtime > then)

            if (realtime - then < 5000) {
                Log.e("asdf", "skipping notification update for " + instance.name)

                return
            }
        }

        NotificationWrapper.instance.notifyInstance(this, instance, true, now)
    }

    fun setInstanceNotified(instanceKey: InstanceKey, now: ExactTimeStamp) {
        if (instanceKey.type === TaskKey.Type.LOCAL) {
            val instance = getInstance(instanceKey)

            instance.setNotified(now)
            instance.setNotificationShown(false, now)
        } else {
            val taskKey = instanceKey.taskKey

            val projectId = taskKey.remoteProjectId
            check(!TextUtils.isEmpty(projectId))

            val taskId = taskKey.remoteTaskId
            check(!TextUtils.isEmpty(taskId))

            val scheduleKey = instanceKey.scheduleKey
            val scheduleDate = scheduleKey.scheduleDate

            val stream = localFactory.instanceShownRecords
                    .asSequence()
                    .filter { it.projectId == projectId && it.taskId == taskId && it.scheduleYear == scheduleDate.year && it.scheduleMonth == scheduleDate.month && it.scheduleDay == scheduleDate.day }

            val matches: Sequence<InstanceShownRecord>
            if (scheduleKey.scheduleTimePair.customTimeKey != null) {
                check(scheduleKey.scheduleTimePair.hourMinute == null)

                check(scheduleKey.scheduleTimePair.customTimeKey.type === TaskKey.Type.REMOTE) // remote custom time key hack
                check(scheduleKey.scheduleTimePair.customTimeKey.localCustomTimeId == null)
                check(projectId == scheduleKey.scheduleTimePair.customTimeKey.remoteProjectId)

                val customTimeId = scheduleKey.scheduleTimePair.customTimeKey.remoteCustomTimeId
                check(!TextUtils.isEmpty(customTimeId))

                matches = stream.filter { customTimeId == it.scheduleCustomTimeId }
            } else {
                check(scheduleKey.scheduleTimePair.hourMinute != null)

                val hourMinute = scheduleKey.scheduleTimePair.hourMinute

                matches = stream.filter { hourMinute.hour == it.scheduleHour && hourMinute.minute == it.scheduleMinute }
            }

            val instanceShownRecord = matches.single()

            instanceShownRecord.notified = true
            instanceShownRecord.notificationShown = false
        }
    }

    fun getGroupListData(timeStamp: TimeStamp, now: ExactTimeStamp): GroupListFragment.DataWrapper {
        val endCalendar = timeStamp.calendar.apply { add(Calendar.MINUTE, 1) }
        val endTimeStamp = TimeStamp(endCalendar)

        val rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now)

        val currentInstances = rootInstances.filter { it.instanceDateTime.timeStamp.compareTo(timeStamp) == 0 }

        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = HashMap<InstanceKey, GroupListFragment.InstanceData>()
        for (instance in currentInstances) {
            val task = instance.task

            val isRootTask = if (task.current(now)) task.isRootTask(now) else null

            val children = getChildInstanceDatas(instance, now)
            val instanceData = GroupListFragment.InstanceData(instance.done, instance.instanceKey, null, instance.name, instance.instanceDateTime.timeStamp, task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.instanceDateTime.time.timePair, task.note, children, null, instance.ordinal)
            children.values.forEach { it.instanceDataParent = instanceData }
            instanceDatas[instance.instanceKey] = instanceData
        }

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, null, null, null, instanceDatas)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    fun getGroupListData(instance: Instance, task: Task, now: ExactTimeStamp): GroupListFragment.DataWrapper {
        val customTimeDatas = getCurrentCustomTimes().map { GroupListFragment.CustomTimeData(it.name, it.hourMinutes) }

        val instanceDatas = instance.getChildInstances(now)
                .map { (childInstance, taskHierarchy) ->
                    val childTask = childInstance.task

                    val isRootTask = if (childTask.current(now)) childTask.isRootTask(now) else null

                    val children = getChildInstanceDatas(childInstance, now)
                    val instanceData = GroupListFragment.InstanceData(childInstance.done, childInstance.instanceKey, null, childInstance.name, childInstance.instanceDateTime.timeStamp, childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.instanceDateTime.time.timePair, childTask.note, children, HierarchyData(taskHierarchy.taskHierarchyKey, taskHierarchy.ordinal), childInstance.ordinal)
                    children.values.forEach { it.instanceDataParent = instanceData }
                    childInstance.instanceKey to instanceData
                }
                .toMap()
                .toMutableMap()

        val dataWrapper = GroupListFragment.DataWrapper(customTimeDatas, task.current(now), null, task.note, instanceDatas)

        instanceDatas.values.forEach { it.instanceDataParent = dataWrapper }

        return dataWrapper
    }

    fun getCustomTimeKey(remoteProjectId: String, remoteCustomTimeId: String) = localFactory.getLocalCustomTime(remoteProjectId, remoteCustomTimeId)?.customTimeKey
            ?: CustomTimeKey(remoteProjectId, remoteCustomTimeId)

    fun save(context: Context, dataIds: List<Int>, source: SaveService.Source) {
        if (skipSave)
            return

        localFactory.save(context, source)

        if (remoteProjectFactory != null)
            remoteProjectFactory!!.save()

        ObserverHolder.notifyDomainObservers(dataIds)
    }
}