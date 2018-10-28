package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.text.TextUtils
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

            val localCustomTime = localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId!!)

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

                hour = hourMinute!!.hour
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

    fun getTime(timePair: TimePair) = if (timePair.hourMinute != null) {
        check(timePair.customTimeKey == null)

        NormalTime(timePair.hourMinute)
    } else {
        checkNotNull(timePair.customTimeKey)

        getCustomTime(timePair.customTimeKey!!)
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

        val localToRemoteConversion = DomainFactory.LocalToRemoteConversion()
        localFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask)

        domainFactory.updateNotifications(context, true, now, localToRemoteConversion.mLocalTasks
                .values
                .map { it.first.taskKey })

        val remoteProject = remoteProjectFactory!!.getRemoteProjectForce(projectId)

        for (pair in localToRemoteConversion.mLocalTasks.values) {
            checkNotNull(pair)

            val remoteTask = remoteProject.copyLocalTask(pair!!.first, pair.second, now)
            localToRemoteConversion.mRemoteTasks[pair.first.id] = remoteTask
        }

        for (localTaskHierarchy in localToRemoteConversion.mLocalTaskHierarchies) {
            checkNotNull(localTaskHierarchy)

            val parentRemoteTask = localToRemoteConversion.mRemoteTasks[localTaskHierarchy!!.parentTaskId]!!
            val childRemoteTask = localToRemoteConversion.mRemoteTasks[localTaskHierarchy.childTaskId]!!

            val remoteTaskHierarchy = remoteProject.copyLocalTaskHierarchy(localTaskHierarchy, parentRemoteTask.id, childRemoteTask.id)

            localToRemoteConversion.mRemoteTaskHierarchies.add(remoteTaskHierarchy)
        }

        for (pair in localToRemoteConversion.mLocalTasks.values) {
            pair.second.forEach { it.delete() }

            pair.first.delete()
        }

        return localToRemoteConversion.mRemoteTasks[startingLocalTask.id]!!
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

    fun getTasks(): Stream<Task> { // todo eliminate stream
        return if (remoteProjectFactory != null) {
            Stream.concat<Task>(Stream.of<LocalTask>(localFactory.tasks), Stream.of<RemoteTask>(remoteProjectFactory!!.tasks))
        } else {
            Stream.of<Task>(localFactory.tasks)
        }
    }

    fun getCustomTimes() = localFactory.localCustomTimes.toMutableList<CustomTime>().apply {
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

    fun getExistingInstances() = localFactory.existingInstances
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

        domainFactory.updateNotifications(context, now)

        domainFactory.save(context, dataId, source)

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

    fun notifyCloud(context: Context, remoteProject: RemoteProject?) {
        val remoteProjects = mutableSetOf<RemoteProject>()
        remoteProject?.let { remoteProjects.add(it) }

        notifyCloud(context, remoteProjects)
    }

    fun notifyCloud(context: Context, remoteProjects: Set<RemoteProject>) {
        if (!remoteProjects.isEmpty()) {
            checkNotNull(userInfo)

            BackendNotifier.notify(context, remoteProjects, userInfo!!, ArrayList())
        }
    }
}